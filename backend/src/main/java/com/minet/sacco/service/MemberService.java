package com.minet.sacco.service;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.AccountRepository;
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public Optional<Member> getMemberById(Long id) {
        return memberRepository.findById(id);
    }

    public Optional<Member> getMemberByMemberNumber(String memberNumber) {
        return memberRepository.findByMemberNumber(memberNumber);
    }

    @Transactional
    public Member createMember(Member member, Long createdByUserId) {
        // Use employeeId as the member identifier (memberNumber)
        if (member.getEmployeeId() != null && !member.getEmployeeId().isBlank()) {
            if (memberRepository.existsByEmployeeId(member.getEmployeeId())) {
                throw new RuntimeException("A member with member number " + member.getEmployeeId() + " already exists");
            }
            member.setMemberNumber(member.getEmployeeId());
        } else if (member.getMemberNumber() == null) {
            member.setMemberNumber(generateMemberNumber());
        }

        // Register immediately as ACTIVE — no approval step needed
        member.setStatus(Member.Status.ACTIVE);
        member.setCreatedBy(createdByUserId);
        member.setApprovedBy(createdByUserId);
        member.setApprovedAt(LocalDateTime.now());
        member.setCreatedAt(LocalDateTime.now());

        Member savedMember = memberRepository.save(member);

        // Create default Savings and Shares accounts immediately
        createDefaultAccounts(savedMember);

        // Create mobile app login credentials: username = memberNumber, password = nationalId
        createMemberUserAccount(savedMember);

        return savedMember;
    }

    @Transactional
    public Member approveMember(Long memberId, Long approvedByUserId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        if (member.getStatus() != Member.Status.PENDING) {
            throw new RuntimeException("Only PENDING members can be approved");
        }
        
        member.setStatus(Member.Status.APPROVED);
        member.setApprovedBy(approvedByUserId);
        member.setApprovedAt(LocalDateTime.now());
        member.setUpdatedAt(LocalDateTime.now());
        
        Member savedMember = memberRepository.save(member);
        
        // Create default accounts (Savings and Shares)
        createDefaultAccounts(savedMember);

        // Auto-create member login credentials for mobile app
        // Username = employeeId (or memberNumber), default password = nationalId
        createMemberUserAccount(savedMember);
        
        // Log audit event
        try {
            User approvedByUser = userRepository.findById(approvedByUserId)
                    .orElse(null);
            if (approvedByUser != null) {
                String memberDetails = "Member #" + savedMember.getMemberNumber() + " - " + 
                                      savedMember.getFirstName() + " " + savedMember.getLastName() + 
                                      " (ID: " + savedMember.getNationalId() + ")";
                auditService.logAction(approvedByUser, "APPROVE", "MEMBER", savedMember.getId(), 
                                      memberDetails, "Member approved", "SUCCESS");
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to log audit for member approval: " + e.getMessage());
        }
        
        return savedMember;
    }

    /**
     * Creates a User account for the member so they can log in via the mobile app.
     * Username = employeeId, default password = nationalId (member must change on first login).
     */
    private void createMemberUserAccount(Member member) {
        String username = member.getEmployeeId() != null ? member.getEmployeeId() : member.getMemberNumber();
        if (username == null || userRepository.existsByUsername(username)) {
            return; // Already has an account or no identifier available
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(member.getEmail() != null ? member.getEmail() : username + "@minet.sacco");
        user.setPassword(passwordEncoder.encode(member.getNationalId())); // default password = national ID
        user.setRole(User.Role.MEMBER);
        user.setMemberId(member.getId());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public Member rejectMember(Long memberId, String reason, Long rejectedByUserId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        if (member.getStatus() != Member.Status.PENDING) {
            throw new RuntimeException("Only PENDING members can be rejected");
        }
        
        member.setStatus(Member.Status.REJECTED);
        member.setRejectionReason(reason);
        member.setUpdatedAt(LocalDateTime.now());
        
        Member savedMember = memberRepository.save(member);
        
        // Log audit event
        try {
            User rejectedByUser = userRepository.findById(rejectedByUserId)
                    .orElse(null);
            if (rejectedByUser != null) {
                String memberDetails = "Member #" + savedMember.getMemberNumber() + " - " + 
                                      savedMember.getFirstName() + " " + savedMember.getLastName() + 
                                      " (ID: " + savedMember.getNationalId() + ")";
                auditService.logAction(rejectedByUser, "REJECT", "MEMBER", savedMember.getId(), 
                                      memberDetails, reason, "SUCCESS");
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to log audit for member rejection: " + e.getMessage());
        }
        
        return savedMember;
    }

    @Transactional
    public Member activateMember(Long memberId, Long activatedByUserId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        if (member.getStatus() != Member.Status.APPROVED) {
            throw new RuntimeException("Only APPROVED members can be activated");
        }
        
        member.setStatus(Member.Status.ACTIVE);
        member.setUpdatedAt(LocalDateTime.now());
        
        Member savedMember = memberRepository.save(member);
        
        // Log audit event
        try {
            User activatedByUser = userRepository.findById(activatedByUserId)
                    .orElse(null);
            if (activatedByUser != null) {
                String memberDetails = "Member #" + savedMember.getMemberNumber() + " - " + 
                                      savedMember.getFirstName() + " " + savedMember.getLastName() + 
                                      " (ID: " + savedMember.getNationalId() + ")";
                auditService.logAction(activatedByUser, "ACTIVATE", "MEMBER", savedMember.getId(), 
                                      memberDetails, "Member activated", "SUCCESS");
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to log audit for member activation: " + e.getMessage());
        }
        
        return savedMember;
    }

    private void createDefaultAccounts(Member member) {
        // Create Savings Account
        Account savingsAccount = new Account();
        savingsAccount.setMember(member);
        savingsAccount.setAccountType(Account.AccountType.SAVINGS);
        savingsAccount.setBalance(BigDecimal.ZERO);
        savingsAccount.setCreatedAt(LocalDateTime.now());
        accountRepository.save(savingsAccount);
        
        // Create Shares Account
        Account sharesAccount = new Account();
        sharesAccount.setMember(member);
        sharesAccount.setAccountType(Account.AccountType.SHARES);
        sharesAccount.setBalance(BigDecimal.ZERO);
        sharesAccount.setCreatedAt(LocalDateTime.now());
        accountRepository.save(sharesAccount);
    }

    public Member updateMember(Member member) {
        member.setUpdatedAt(LocalDateTime.now());
        return memberRepository.save(member);
    }

    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }

    public List<Member> getMembersByStatus(Member.Status status) {
        return memberRepository.findByStatus(status);
    }

    public List<Member> getMembersByDepartment(String department) {
        return memberRepository.findByDepartment(department);
    }

    private String generateMemberNumber() {
        // Generate format: MNT-XXXXX (e.g., MNT-00001)
        long count = memberRepository.count() + 1;
        return String.format("MNT-%05d", count);
    }

    @Transactional
    public Member exitMember(Long memberId, String exitReason, Long exitedByUserId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (member.getStatus() == Member.Status.EXITED) {
            throw new RuntimeException("Member has already exited");
        }

        // Check for active loans
        List<Loan> activeLoans = loanRepository.findByMemberId(memberId).stream()
                .filter(loan -> loan.getStatus() == Loan.Status.DISBURSED || 
                               loan.getStatus() == Loan.Status.APPROVED)
                .toList();

        if (!activeLoans.isEmpty()) {
            throw new RuntimeException("Member has " + activeLoans.size() + " active loans. All loans must be settled before exit.");
        }

        // Mark member as EXITED
        member.setStatus(Member.Status.EXITED);
        member.setExitDate(LocalDateTime.now());
        member.setExitReason(exitReason);
        member.setUpdatedAt(LocalDateTime.now());

        return memberRepository.save(member);
    }

    public List<Member> getExitedMembers() {
        return memberRepository.findByStatus(Member.Status.EXITED);
    }

    public List<Member> getExitedMembersWithOutstandingLoans() {
        List<Member> exitedMembers = getExitedMembers();
        return exitedMembers.stream()
                .filter(member -> {
                    List<Loan> loans = loanRepository.findByMemberId(member.getId());
                    return loans.stream().anyMatch(loan -> 
                        loan.getStatus() == Loan.Status.DISBURSED || 
                        loan.getStatus() == Loan.Status.APPROVED);
                })
                .toList();
    }
}