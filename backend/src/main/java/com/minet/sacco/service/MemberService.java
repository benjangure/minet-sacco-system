package com.minet.sacco.service;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.User;
import com.minet.sacco.entity.MemberCredential;
import com.minet.sacco.entity.Guarantor;
import com.minet.sacco.dto.MemberCreationResponseDTO;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.AccountRepository;
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.repository.MemberCredentialRepository;
import com.minet.sacco.repository.GuarantorRepository;
import com.minet.sacco.util.PasswordGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    @Autowired
    private MemberCredentialRepository memberCredentialRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private GuarantorRepository guarantorRepository;

    // Temporary storage for generated passwords (cleaned up after use)
    private static final Map<Long, String> generatedPasswords = new ConcurrentHashMap<>();

    @Cacheable(value = "members", unless = "#result == null || #result.isEmpty()")
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public org.springframework.data.domain.Page<Member> getAllMembersPaginated(org.springframework.data.domain.Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    @Cacheable(value = "memberById", key = "#id", unless = "#result == null || !#result.isPresent()")
    public Optional<Member> getMemberById(Long id) {
        return memberRepository.findById(id);
    }

    @Cacheable(value = "memberByNumber", key = "#memberNumber", unless = "#result == null || !#result.isPresent()")
    public Optional<Member> getMemberByMemberNumber(String memberNumber) {
        return memberRepository.findByMemberNumber(memberNumber);
    }

    @Transactional
    @CacheEvict(value = {"members", "membersByStatus"}, allEntries = true)
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

    /**
     * Creates a member and returns both member data and credential info for UI display
     */
    @Transactional
    public MemberCreationResponseDTO createMemberWithCredentials(Member member, Long createdByUserId) {
        Member savedMember = createMember(member, createdByUserId);
        
        // Get username
        String username = member.getEmployeeId() != null ? member.getEmployeeId() : savedMember.getMemberNumber();
        
        // Determine password type and temporary password
        boolean hasNationalId = member.getNationalId() != null && !member.getNationalId().trim().isEmpty();
        String temporaryPassword = null;
        String passwordType;
        
        if (hasNationalId) {
            passwordType = "NATIONAL_ID";
        } else {
            temporaryPassword = getLastGeneratedPassword(savedMember.getId());
            passwordType = "GENERATED";
        }
        
        return new MemberCreationResponseDTO(
            savedMember.getId(),
            savedMember.getMemberNumber(),
            savedMember.getFirstName(),
            savedMember.getLastName(),
            username,
            temporaryPassword,
            hasNationalId,
            passwordType,
            "Member created successfully. Credentials are ready for delivery."
        );
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
        if (username == null) {
            return; // No identifier available
        }
        
        // Check if user already exists
        Optional<User> existingUser = userRepository.findByUsername(username);
        User user;
        boolean isNewUser = false;
        String temporaryPassword = null;
        boolean hasNationalId = member.getNationalId() != null && !member.getNationalId().trim().isEmpty();
        
        if (existingUser.isPresent()) {
            // Update existing user with member_id if not already set
            user = existingUser.get();
            if (user.getMemberId() == null) {
                user.setMemberId(member.getId());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        } else {
            // Create new user account
            isNewUser = true;
            user = new User();
            user.setUsername(username);
            user.setEmail(member.getEmail() != null ? member.getEmail() : username + "@minet.sacco");
            
            // Set password based on whether National ID is available
            if (hasNationalId) {
                user.setPassword(passwordEncoder.encode(member.getNationalId())); // default password = national ID
            } else {
                // Generate secure temporary password
                temporaryPassword = PasswordGenerator.generateTemporaryPassword();
                user.setPassword(passwordEncoder.encode(temporaryPassword));
                // Store temporarily for retrieval in controller
                generatedPasswords.put(member.getId(), temporaryPassword);
            }
            
            user.setRole(User.Role.MEMBER);
            user.setMemberId(member.getId());
            user.setEnabled(true);
            user.setFirstLogin(true);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
            
            // Create credential tracking record for individual registration
            if (isNewUser) {
                createCredentialTrackingRecord(member, username, temporaryPassword, hasNationalId);
            }
        }
    }
    
    /**
     * Creates a credential tracking record for individual member registration
     */
    private void createCredentialTrackingRecord(Member member, String username, String temporaryPassword, boolean hasNationalId) {
        try {
            // Check if credential record already exists
            Optional<MemberCredential> existingCredential = memberCredentialRepository.findByMemberId(member.getId());
            if (existingCredential.isPresent()) {
                return; // Already tracked
            }
            
            MemberCredential credential = new MemberCredential();
            credential.setMemberId(member.getId());
            credential.setUsername(username);
            credential.setMemberName(member.getFullName());
            credential.setEmail(member.getEmail());
            credential.setHasNationalId(hasNationalId);
            credential.setEmailSent(false); // Admin needs to manually deliver credentials
            credential.setPasswordChanged(false);
            credential.setCreatedAt(LocalDateTime.now());
            
            // Store the temporary password if generated, or indicate National ID usage
            if (hasNationalId) {
                credential.setPassword(member.getNationalId());
            } else {
                credential.setPassword(temporaryPassword);
            }
            
            memberCredentialRepository.save(credential);
            
            // Console logging for admin reference (until email is set up)
            if (hasNationalId) {
                System.out.println("=== INDIVIDUAL MEMBER REGISTRATION ===");
                System.out.println("Member: " + member.getFirstName() + " " + member.getLastName());
                System.out.println("Username: " + username);
                System.out.println("Initial Password: Use National ID (" + member.getNationalId() + ")");
                System.out.println("Instructions: Tell member to use their National ID as password");
                System.out.println("=====================================");
            } else {
                System.out.println("=== INDIVIDUAL MEMBER REGISTRATION ===");
                System.out.println("Member: " + member.getFirstName() + " " + member.getLastName());
                System.out.println("Username: " + username);
                System.out.println("Temporary Password: " + temporaryPassword);
                System.out.println("Instructions: Share these credentials with member");
                System.out.println("=====================================");
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: Failed to create credential tracking record for member " + member.getId() + ": " + e.getMessage());
            // Don't throw exception - credential tracking shouldn't block member creation
        }
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
        
        // Create Shares Account with mandatory 3000 KES share capital
        Account sharesAccount = new Account();
        sharesAccount.setMember(member);
        sharesAccount.setAccountType(Account.AccountType.SHARES);
        sharesAccount.setBalance(new BigDecimal("3000.00")); // Mandatory share capital for all members
        sharesAccount.setCreatedAt(LocalDateTime.now());
        accountRepository.save(sharesAccount);
    }

    @CacheEvict(value = {"members", "memberById", "memberByNumber", "membersByStatus"}, allEntries = true)
    public Member updateMember(Member member) {
        member.setUpdatedAt(LocalDateTime.now());
        return memberRepository.save(member);
    }

    @CacheEvict(value = {"members", "memberById", "memberByNumber", "membersByStatus"}, allEntries = true)
    public void deleteMember(Long id) {
        memberRepository.deleteById(id);
    }

    /**
     * Retrieves and removes the temporarily stored generated password for a member
     */
    protected String getLastGeneratedPassword(Long memberId) {
        return generatedPasswords.remove(memberId);
    }

    /**
     * Stores a generated password temporarily for retrieval (for UI display)
     */
    protected void storeGeneratedPassword(Long memberId, String password) {
        generatedPasswords.put(memberId, password);
    }

    @Cacheable(value = "membersByStatus", key = "#status", unless = "#result == null || #result.isEmpty()")
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

    /**
     * Analyze the impact of a member exiting
     * Returns information about loans they guarantee and NOK coverage
     */
    public com.minet.sacco.dto.MemberExitImpactResponse analyzeExitImpact(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Check for active loans where this member is a guarantor
        List<Guarantor> activeGuarantees = guarantorRepository.findActiveGuaranteesByMemberId(memberId);
        
        com.minet.sacco.dto.MemberExitImpactResponse response = new com.minet.sacco.dto.MemberExitImpactResponse();
        response.setMemberId(memberId);
        response.setMemberName(member.getFirstName() + " " + member.getLastName());
        response.setEmployeeId(member.getEmployeeId());
        response.setTotalLoansAsGuarantor(activeGuarantees.size());
        
        int loansWithoutNok = 0;
        boolean allLoansHaveNok = true;
        java.math.BigDecimal totalGuaranteeAmount = java.math.BigDecimal.ZERO;
        java.util.List<com.minet.sacco.dto.MemberExitImpactResponse.LoanGuaranteeInfo> loanInfoList = new java.util.ArrayList<>();
        
        for (Guarantor guarantee : activeGuarantees) {
            Loan loan = guarantee.getLoan();
            Member borrower = loan.getMember();
            
            // Add guarantee amount to total
            totalGuaranteeAmount = totalGuaranteeAmount.add(guarantee.getGuaranteeAmount());
            
            // Check if NOK exists
            Guarantor nokGuarantor = guarantee.getNextOfKinGuarantor();
            boolean hasNok = nokGuarantor != null;
            String nokName = null;
            Long nokMemberId = null;
            
            if (hasNok) {
                Member nokMember = nokGuarantor.getMember();
                nokName = nokMember.getFirstName() + " " + nokMember.getLastName();
                nokMemberId = nokMember.getId();
            } else {
                loansWithoutNok++;
                allLoansHaveNok = false;
            }
            
            // Create loan info
            com.minet.sacco.dto.MemberExitImpactResponse.LoanGuaranteeInfo loanInfo = 
                new com.minet.sacco.dto.MemberExitImpactResponse.LoanGuaranteeInfo(
                    loan.getId(),
                    loan.getLoanNumber(),
                    borrower.getFirstName() + " " + borrower.getLastName(),
                    guarantee.getGuaranteeAmount(),
                    hasNok,
                    nokName,
                    nokMemberId
                );
            loanInfoList.add(loanInfo);
        }
        
        response.setLoansAsGuarantor(loanInfoList);
        response.setTotalGuaranteeAmount(totalGuaranteeAmount);
        response.setLoansWithoutNok(loansWithoutNok);
        response.setAllLoansHaveNok(allLoansHaveNok);
        
        return response;
    }

    @Transactional
    public Member exitMember(Long memberId, com.minet.sacco.dto.MemberExitRequest exitRequest, Long exitedByUserId) {
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

        // Replace this member's guarantees with NOK guarantors
        List<Guarantor> activeGuarantees = guarantorRepository.findActiveGuaranteesByMemberId(memberId);
        for (Guarantor guarantee : activeGuarantees) {
            Guarantor nokGuarantor = guarantee.getNextOfKinGuarantor();
            if (nokGuarantor != null) {
                // Activate the NOK guarantor
                nokGuarantor.setStatus(Guarantor.Status.ACTIVATED_FROM_NOK);
                guarantorRepository.save(nokGuarantor);
                
                // Deactivate the exiting member's guarantee
                guarantee.setStatus(Guarantor.Status.REPLACED_DUE_TO_EXIT);
                guarantee.setReplacedAt(LocalDateTime.now());
                guarantee.setReplacedByGuarantorId(nokGuarantor.getId());
                guarantee.setReplacementReason("Member exited from SACCO: " + exitRequest.getExitReason());
                guarantorRepository.save(guarantee);
            }
        }

        // Mark member as EXITED
        member.setStatus(Member.Status.EXITED);
        member.setExitDate(exitRequest.getExitDate() != null ? exitRequest.getExitDate() : LocalDateTime.now());
        member.setExitReason(exitRequest.getExitReason() + 
                (exitRequest.getExitNotes() != null && !exitRequest.getExitNotes().trim().isEmpty() 
                    ? " - " + exitRequest.getExitNotes() 
                    : ""));
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

    /**
     * Reactivate an exited member
     * Changes status from EXITED back to ACTIVE
     */
    @Transactional
    public Member reactivateMember(Long memberId, Long reactivatedByUserId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        if (member.getStatus() != Member.Status.EXITED) {
            throw new RuntimeException("Only exited members can be reactivated. Current status: " + member.getStatus());
        }

        // Check if member has outstanding loans
        List<Loan> outstandingLoans = loanRepository.findByMemberId(memberId).stream()
                .filter(loan -> loan.getStatus() == Loan.Status.DISBURSED || 
                               loan.getStatus() == Loan.Status.APPROVED)
                .toList();

        if (!outstandingLoans.isEmpty()) {
            throw new RuntimeException("Member has " + outstandingLoans.size() + " outstanding loans. Loans must be cleared before reactivation.");
        }

        // Reactivate the member
        member.setStatus(Member.Status.ACTIVE);
        member.setExitDate(null); // Clear exit date
        member.setExitReason(null); // Clear exit reason
        member.setUpdatedAt(LocalDateTime.now());

        return memberRepository.save(member);
    }
}