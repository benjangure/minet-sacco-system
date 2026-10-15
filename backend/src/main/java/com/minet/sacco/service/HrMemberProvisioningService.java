package com.minet.sacco.service;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.MemberCredential;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberCredentialRepository;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Creates / refreshes the local member + login after a successful HR authentication, so a member
 * who can sign in with their HR credentials is automatically added to the SACCO system.
 */
@Service
public class HrMemberProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(HrMemberProvisioningService.class);

    @Autowired private MemberRepository memberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MemberService memberService;
    @Autowired private MemberCredentialService memberCredentialService;
    @Autowired private MemberCredentialRepository memberCredentialRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /**
     * The SACCO member number is the HR payroll number (PayrollNo). The HR login name (e.g.
     * "wakaee") is only the credential used to authenticate. Falls back to the typed username
     * when HR does not return a payroll number.
     */
    public static String memberIdentifier(String typedUsername, HrAuthService.Result hr) {
        String payroll = hr.get("payrollno", "payroll_no", "payroll", "payroll_number", "employee_no",
                "employee_id", "employeeid", "staff_no", "staffno");
        return (payroll != null && !payroll.isBlank()) ? payroll.trim() : typedUsername.trim();
    }

    @Transactional
    public User provisionOrSync(String username, String plainPassword, HrAuthService.Result hr) {
        String id = username.trim();

        Optional<Member> existing = memberRepository.findByEmployeeId(id);
        if (existing.isEmpty()) {
            existing = memberRepository.findByMemberNumber(id);
        }

        Member member;
        if (existing.isPresent()) {
            member = existing.get();
        } else {
            member = buildMember(id, hr);
            member = memberService.createMember(member, null);
            log.info("HR sign-in: created new member {}", id);
        }

        // Remember the HR login name so the member can sign in with their Employee ID next time
        String hrUser = hr.get("username", "user_name", "login", "userid", "user_id");
        if (hrUser != null && !hrUser.equalsIgnoreCase(member.getHrUsername())) {
            member.setHrUsername(hrUser.length() > 100 ? hrUser.substring(0, 100) : hrUser);
            member = memberRepository.save(member);
        }

        // Make sure a login exists for this member (createMember normally does this)
        User user = userRepository.findByUsername(id).orElse(null);
        if (user == null) {
            memberCredentialService.ensureLoginAndCredential(member, null);
            user = userRepository.findByUsername(id)
                    .orElseThrow(() -> new RuntimeException("Could not create a login for this member"));
        }
        if (user.getMemberId() == null) {
            user.setMemberId(member.getId());
        }

        // HR owns the password: keep the local hash in step (used only if local fallback is enabled)
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setFirstLogin(false);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Credentials dashboard: no temporary password applies any more
        MemberCredential credential = memberCredentialService.ensureCredentialRecord(member, null, user.getId());
        if (credential != null) {
            credential.setPassword(null);
            credential.setPasswordChanged(true);
            credential.setPasswordChangedAt(LocalDateTime.now());
            memberCredentialRepository.save(credential);
        }
        return user;
    }

    private Member buildMember(String id, HrAuthService.Result hr) {
        Member m = new Member();
        m.setEmployeeId(id);
        m.setMemberNumber(id);

        String fullName = hr.get("names", "full_name", "fullname", "employee_name", "name", "display_name", "displayname");
        if (fullName == null) {
            String first = hr.get("first_name", "firstname", "fname");
            String last = hr.get("last_name", "lastname", "surname", "lname");
            if (first != null || last != null) {
                fullName = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
            }
        }
        m.setFullName(fullName != null && !fullName.isBlank() ? fullName : id);

        String email = hr.get("email", "email_address", "mail", "work_email");
        if (email != null) email = email.trim().toLowerCase();
        if (email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                && userRepository.findByEmail(email).isEmpty()
                && memberRepository.findByEmail(email).isEmpty()) {
            m.setEmail(email);
        } else {
            m.setEmail(id + "@minet.sacco");
        }

        String phone = hr.get("phone", "phone_number", "mobile", "msisdn", "telephone", "tel");
        if (phone != null) {
            String digits = phone.replaceAll("[^0-9+]", "");
            if (!digits.isEmpty() && digits.length() <= 15) {
                m.setPhone(digits);
            }
        }

        String nationalId = hr.get("national_id", "nationalid", "id_number", "idnumber", "id_no", "idno", "national_id_no");
        if (nationalId != null && memberRepository.findByNationalId(nationalId).isEmpty()) {
            m.setNationalId(nationalId);
        }

        String department = hr.get("department", "dept", "department_name");
        if (department != null) {
            m.setDepartment(department.length() > 50 ? department.substring(0, 50) : department);
        }
        m.setEmploymentStatus("PERMANENT");
        return m;
    }
}
