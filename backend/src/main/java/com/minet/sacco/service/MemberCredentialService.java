package com.minet.sacco.service;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.MemberCredential;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberCredentialRepository;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.util.NameFormatter;
import com.minet.sacco.util.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Single place that guarantees every member has (a) a login User and (b) a visible
 * MemberCredential tracking record. Used by single registration, bulk import,
 * migration, and the repair endpoint / startup sync.
 */
@Service
public class MemberCredentialService {

    private static final Logger log = LoggerFactory.getLogger(MemberCredentialService.class);

    @Autowired private MemberRepository memberRepository;
    @Autowired private MemberCredentialRepository memberCredentialRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private HrAuthService hrAuthService;

    /** Username convention: employee id (member number) else member number. */
    public static String usernameFor(Member member) {
        if (member.getEmployeeId() != null && !member.getEmployeeId().isBlank()) {
            return member.getEmployeeId().trim();
        }
        return member.getMemberNumber();
    }

    /**
     * Idempotently ensures a credential tracking record exists for the member.
     * If a login user already exists the password is unknown (hashed), so the record
     * is created without a stored password unless the member's national id is the password.
     *
     * @return the existing or new credential record, or null if the member has no identifier
     */
    @Transactional
    public MemberCredential ensureCredentialRecord(Member member, String plainPasswordIfKnown, Long createdBy) {
        if (member == null || member.getId() == null) {
            return null;
        }
        Optional<MemberCredential> existing = memberCredentialRepository.findByMemberId(member.getId());
        if (existing.isPresent()) {
            MemberCredential cred = existing.get();
            String proper = NameFormatter.toProperCase(member.getFullName());
            if (proper != null && !proper.equals(cred.getMemberName())) {
                cred.setMemberName(proper);
                memberCredentialRepository.save(cred);
            }
            return cred;
        }

        String username = usernameFor(member);
        if (username == null || username.isBlank()) {
            log.warn("Cannot create credential record for member id={} - no identifier", member.getId());
            return null;
        }

        boolean hasNationalId = member.getNationalId() != null && !member.getNationalId().trim().isEmpty();
        MemberCredential credential = new MemberCredential(
                member.getId(), username, member.getFullName(), member.getEmail(), hasNationalId, createdBy);
        boolean changedByMember = false;
        if (plainPasswordIfKnown != null) {
            credential.setPassword(plainPasswordIfKnown);
        } else {
            // A login already exists. Only record a password we can PROVE is current - otherwise the
            // credentials dashboard would show a password that does not work.
            Optional<User> login = userRepository.findByUsername(username);
            if (login.isPresent()) {
                User u = login.get();
                if (hasNationalId && passwordEncoder.matches(member.getNationalId().trim(), u.getPassword())) {
                    credential.setPassword(member.getNationalId().trim());
                } else {
                    changedByMember = !u.isFirstLogin();
                }
            }
        }
        credential.setPasswordChanged(changedByMember);
        credential.setCreatedAt(LocalDateTime.now());
        return memberCredentialRepository.save(credential);
    }

    /**
     * Ensures the member has a login user (national id or generated temporary password)
     * and a credential record. Safe to call repeatedly.
     */
    @Transactional
    public MemberCredential ensureLoginAndCredential(Member member, Long createdBy) {
        String username = usernameFor(member);
        if (username == null || username.isBlank()) {
            return null;
        }
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getMemberId() == null) {
                user.setMemberId(member.getId());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
            return ensureCredentialRecord(member, null, createdBy);
        }

        boolean hasNationalId = member.getNationalId() != null && !member.getNationalId().trim().isEmpty();
        String password = hasNationalId ? member.getNationalId().trim() : PasswordGenerator.generateTemporaryPassword();

        User user = new User();
        user.setUsername(username);
        user.setEmail(member.getEmail() != null && !member.getEmail().isBlank() ? member.getEmail() : username + "@minet.sacco");
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(User.Role.MEMBER);
        user.setMemberId(member.getId());
        user.setEnabled(true);
        user.setFirstLogin(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        return ensureCredentialRecord(member, password, createdBy);
    }

    /**
     * Admin-assisted reset ("forgot password" when email is unavailable): sets a fresh temporary
     * password, forces the member to choose their own at next login, clears any lockout and
     * updates the credentials dashboard record.
     *
     * @return the temporary password in plain text (shown once to the administrator)
     */
    @Transactional
    public String resetToTemporaryPassword(Long memberId, Long performedByUserId) {
        if (hrAuthService.isHrOnly()) {
            throw new RuntimeException("Member passwords are managed by the HR system and cannot be reset here.");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        String username = usernameFor(member);
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Member has no login identifier");
        }

        boolean hasNationalId = member.getNationalId() != null && !member.getNationalId().trim().isEmpty();
        String temporary = hasNationalId ? member.getNationalId().trim() : PasswordGenerator.generateTemporaryPassword();

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = new User();
            user.setUsername(username);
            user.setEmail(member.getEmail() != null && !member.getEmail().isBlank() ? member.getEmail() : username + "@minet.sacco");
            user.setRole(User.Role.MEMBER);
            user.setEnabled(true);
            user.setCreatedAt(LocalDateTime.now());
        }
        user.setMemberId(member.getId());
        user.setPassword(passwordEncoder.encode(temporary));
        user.setFirstLogin(true);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        MemberCredential credential = memberCredentialRepository.findByMemberId(member.getId())
                .orElseGet(() -> new MemberCredential(member.getId(), username, member.getFullName(), member.getEmail(), hasNationalId, performedByUserId));
        credential.setUsername(username);
        credential.setHasNationalId(hasNationalId);
        credential.setPassword(temporary);
        credential.setPasswordChanged(false);
        credential.setPasswordChangedAt(null);
        credential.setEmailSent(false);
        credential.setEmailSentAt(null);
        memberCredentialRepository.save(credential);
        return temporary;
    }

    /**
     * Remove every member's locally stored password (login hash and the readable temporary password
     * on the credentials dashboard). Members then authenticate through HR only. Staff accounts are
     * never touched. Irreversible; refused unless HR sign-in is the only password source.
     *
     * @return number of member logins and credential records cleared
     */
    @Transactional
    public Map<String, Object> purgeLocalMemberPasswords() {
        if (!hrAuthService.isHrOnly()) {
            throw new RuntimeException(
                    "Refused: HR sign-in must be configured (HR_API_URL) with ALLOW_LOCAL_AUTH_FALLBACK=false "
                            + "before local member passwords can be removed.");
        }
        int logins = 0;
        int records = 0;
        for (User u : userRepository.findByRole(User.Role.MEMBER)) {
            // The column is mandatory, so store a random value nobody knows (not a usable password)
            u.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID() + "-" + java.util.UUID.randomUUID()));
            u.setFirstLogin(false);
            u.setUpdatedAt(LocalDateTime.now());
            userRepository.save(u);
            logins++;
        }
        for (MemberCredential c : memberCredentialRepository.findAll()) {
            if (c.getPassword() != null) {
                c.setPassword(null);
                memberCredentialRepository.save(c);
                records++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("memberLoginsCleared", logins);
        result.put("credentialRecordsCleared", records);
        return result;
    }

    /** Repair: create missing credential records for every member that lacks one. */
    public Map<String, Object> syncMissingCredentials(Long createdBy) {
        int created = 0, skipped = 0, failed = 0;
        List<Member> members = memberRepository.findAll();
        for (Member m : members) {
            try {
                if (memberCredentialRepository.findByMemberId(m.getId()).isPresent()) {
                    continue;
                }
                MemberCredential c = ensureLoginAndCredential(m, createdBy);
                if (c != null) created++; else skipped++;
            } catch (Exception e) {
                failed++;
                log.error("Credential sync failed for member id={} number={}: {}", m.getId(), m.getMemberNumber(), e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("membersChecked", members.size());
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("failed", failed);
        return result;
    }

    /** Repair: normalise stored names to Proper Case on members and credential records. */
    @Transactional
    public int normalizeStoredNames() {
        int changed = 0;
        for (Member m : memberRepository.findAll()) {
            String full = NameFormatter.toProperCase(m.getFullName());
            if (full != null && !full.equals(m.getFullName())) {
                m.setFullName(full);
                memberRepository.save(m);
                changed++;
            }
        }
        for (MemberCredential c : memberCredentialRepository.findAll()) {
            String proper = NameFormatter.toProperCase(c.getMemberName());
            if (proper != null && !proper.equals(c.getMemberName())) {
                c.setMemberName(proper);
                memberCredentialRepository.save(c);
                changed++;
            }
        }
        return changed;
    }
}
