# Member Exit & Suspension System Code

## **Backend Controllers**

### MemberSuspensionController.java
```java
package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.MemberSuspension;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.service.MemberSuspensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberSuspensionController {

    @Autowired
    private MemberSuspensionService memberSuspensionService;

    @Autowired
    private MemberRepository memberRepository;

    /**
     * Suspend a member
     * Credit Committee initiates suspension (acting as HR), Treasurer approves
     */
    @PostMapping("/{memberId}/suspend")
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberSuspension>> suspendMember(
            @PathVariable String memberId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String reason = request.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Reason is required", null));
            }

            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));

            User user = (User) authentication.getPrincipal();
            MemberSuspension suspension = memberSuspensionService.suspendMember(member.getId(), reason, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Member suspended successfully", suspension));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Lift suspension
     */
    @PostMapping("/{memberId}/lift-suspension")
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberSuspension>> liftSuspension(
            @PathVariable String memberId,
            Authentication authentication) {

        try {
            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));
            
            User user = (User) authentication.getPrincipal();
            MemberSuspension suspension = memberSuspensionService.liftSuspension(member.getId(), user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Suspension lifted successfully", suspension));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Validate suspension (Credit Committee approval)
     */
    @PostMapping("/{memberId}/validate-suspension")
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberSuspension>> validateSuspension(
            @PathVariable String memberId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String validationNotes = request.get("validationNotes");
            
            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));

            // Get active suspension
            MemberSuspension suspension = memberSuspensionService.getActiveSuspension(member.getId())
                    .orElseThrow(() -> new RuntimeException("No active suspension found for member"));

            User user = (User) authentication.getPrincipal();
            MemberSuspension updatedSuspension = memberSuspensionService.validateSuspension(suspension.getId(), validationNotes, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Suspension validated successfully", updatedSuspension));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Reactivate a member
     */
    @PostMapping("/{memberId}/reactivate")
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<String>> reactivateMember(
            @PathVariable String memberId,
            Authentication authentication) {

        try {
            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));

            User user = (User) authentication.getPrincipal();
            String result = memberSuspensionService.reactivateMember(member.getId(), user);

            return ResponseEntity.ok(new ApiResponse<>(true, result, null));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get all active suspensions
     */
    @GetMapping("/suspensions/active")
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<List<MemberSuspension>>> getAllActiveSuspensions() {
        try {
            List<MemberSuspension> suspensions = memberSuspensionService.getAllActiveSuspensions();
            return ResponseEntity.ok(new ApiResponse<>(true, "Active suspensions retrieved", suspensions));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
```

### MemberExitController.java
```java
package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.MemberExit;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.service.MemberExitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberExitController {

    @Autowired
    private MemberExitService memberExitService;

    @Autowired
    private MemberRepository memberRepository;

    /**
     * Initiate member exit
     * Credit Committee initiates member exit (acting as HR), Treasurer approves
     */
    @PostMapping("/{memberId}/exit")
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberExit>> initiateMemberExit(
            @PathVariable String memberId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String exitReason = request.get("exitReason");
            if (exitReason == null || exitReason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Exit reason is required", null));
            }

            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));

            User user = (User) authentication.getPrincipal();
            MemberExit exit = memberExitService.initiateMemberExit(member.getId(), exitReason, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Member exit initiated successfully", exit));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Approve member exit (Treasurer approval)
     */
    @PostMapping("/{memberId}/approve-exit")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<ApiResponse<MemberExit>> approveMemberExit(
            @PathVariable String memberId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String approvalNotes = request.get("approvalNotes");
            
            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));

            // Get pending exit
            MemberExit pendingExit = memberExitService.getPendingExit(member.getId())
                    .orElseThrow(() -> new RuntimeException("No pending exit found for member"));

            User user = (User) authentication.getPrincipal();
            MemberExit approvedExit = memberExitService.approveMemberExit(pendingExit.getId(), approvalNotes, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Member exit approved successfully", approvedExit));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
```

## **Backend Services**

### MemberSuspensionService.java
```java
@Service
public class MemberSuspensionService {

    @Autowired
    private MemberSuspensionRepository memberSuspensionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuditService auditService;

    @Transactional
    public MemberSuspension suspendMember(Long memberId, String reason, User suspendedBy) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        Optional<MemberSuspension> existing = memberSuspensionRepository.findByMemberIdAndIsActiveTrue(memberId);
        if (existing.isPresent()) {
            throw new RuntimeException("Member is already suspended");
        }

        MemberSuspension suspension = new MemberSuspension();
        suspension.setMember(member);
        suspension.setReason(reason);
        suspension.setSuspendedBy(suspendedBy);
        suspension.setIsActive(true);

        MemberSuspension saved = memberSuspensionRepository.save(suspension);

        auditService.logAction(suspendedBy, "MEMBER_SUSPENDED",
                "Member", memberId,
                "Member: " + member.getEmployeeId() + ", Reason: " + reason,
                "Credit Committee (acting as HR) suspended member", "SUCCESS");

        return saved;
    }

    @Transactional
    public MemberSuspension validateSuspension(Long suspensionId, String validationNotes, User validatedBy) {
        MemberSuspension suspension = memberSuspensionRepository.findById(suspensionId)
                .orElseThrow(() -> new RuntimeException("Suspension not found"));

        suspension.setValidationNotes(validationNotes);
        suspension.setValidatedBy(validatedBy);
        suspension.setValidatedAt(LocalDateTime.now());

        MemberSuspension saved = memberSuspensionRepository.save(suspension);

        auditService.logAction(validatedBy, "SUSPENSION_VALIDATED",
                "MemberSuspension", suspensionId, 
                "Suspension validated: " + validationNotes,
                "Credit Committee validated suspension", "SUCCESS");

        return saved;
    }

    @Transactional
    public String reactivateMember(Long memberId, User reactivatedBy) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        Optional<MemberSuspension> suspension = memberSuspensionRepository.findByMemberIdAndIsActiveTrue(memberId);
        if (!suspension.isPresent()) {
            return "Member is not currently suspended";
        }

        MemberSuspension activeSuspension = suspension.get();
        activeSuspension.setIsActive(false);
        activeSuspension.setLiftedAt(LocalDateTime.now());
        memberSuspensionRepository.save(activeSuspension);

        auditService.logAction(reactivatedBy, "MEMBER_REACTIVATED",
                "Member", memberId, 
                "Member " + member.getFirstName() + " " + member.getLastName() + " reactivated",
                "Suspension lifted and member reactivated", "SUCCESS");

        return "Member reactivated successfully";
    }
}
```

### MemberExitService.java
```java
@Service
public class MemberExitService {

    @Transactional
    public MemberExit approveMemberExit(Long exitId, String approvalNotes, User approvedBy) {
        MemberExit exit = memberExitRepository.findById(exitId)
                .orElseThrow(() -> new RuntimeException("Exit not found"));

        exit.setApprovalNotes(approvalNotes);
        exit.setApprovedBy(approvedBy);
        exit.setApprovedAt(LocalDateTime.now());
        exit.setStatus("APPROVED");

        MemberExit saved = memberExitRepository.save(exit);

        auditService.logAction(approvedBy, "MEMBER_EXIT_APPROVED",
                "MemberExit", exitId, 
                "Member exit approved: " + approvalNotes,
                "Treasurer approved member exit", "SUCCESS");

        return saved;
    }

    public Optional<MemberExit> getPendingExit(Long memberId) {
        return memberExitRepository.findByMemberIdAndStatus(memberId, "PENDING");
    }
}
```

## **Backend Entities**

### MemberSuspension.java
```java
@Entity
@Table(name = "member_suspensions")
public class MemberSuspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne
    @JoinColumn(name = "suspended_by", nullable = false)
    private User suspendedBy;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @ManyToOne
    @JoinColumn(name = "lifted_by")
    private User liftedBy;

    @Column(name = "lifted_at")
    private LocalDateTime liftedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToOne
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @Column(name = "validation_notes", columnDefinition = "TEXT")
    private String validationNotes;

    @Column(name = "validated_at")
    private LocalDateTime validatedAt;

    // Getters and Setters
    public User getValidatedBy() { return validatedBy; }
    public void setValidatedBy(User validatedBy) { this.validatedBy = validatedBy; }

    public String getValidationNotes() { return validationNotes; }
    public void setValidationNotes(String validationNotes) { this.validationNotes = validationNotes; }

    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }
}
```

### MemberExit.java
```java
@Entity
@Table(name = "member_exits")
public class MemberExit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "exit_reason", nullable = false, length = 50)
    private String exitReason;

    @ManyToOne
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "status")
    private String status;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Getters and Setters
    public String getApprovalNotes() { return approvalNotes; }
    public void setApprovalNotes(String approvalNotes) { this.approvalNotes = approvalNotes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
```

## **Security Implementation**

### CustomUserDetailsService.java
```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberSuspensionRepository memberSuspensionRepository;

    @Autowired
    private MemberExitRepository memberExitRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Check if user is a MEMBER - validate member status
        if (user.getRole() == User.Role.MEMBER) {
            Member member = memberRepository.findByEmployeeId(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Member not found: " + username));

            // Check if member is suspended
            Optional<MemberSuspension> suspension = memberSuspensionRepository.findByMemberIdAndIsActiveTrue(member.getId());
            if (suspension.isPresent()) {
                throw new UsernameNotFoundException("Member is suspended: " + username + ". Please contact administrator.");
            }

            // Check if member has exited
            Optional<MemberExit> exit = memberExitRepository.findByMemberIdAndStatus(member.getId(), "APPROVED");
            if (exit.isPresent()) {
                throw new UsernameNotFoundException("Member has exited: " + username + ". Please contact administrator.");
            }
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.getEnabled(),
                true, true, true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
```

## **Frontend Components**

### MemberSuspension.tsx
- Accepts text employee IDs (EMP001)
- Credit Committee can initiate suspension
- Credit Committee can reactivate members
- Confirmation dialog for actions

### MemberExit.tsx  
- Accepts text employee IDs (EMP001)
- Credit Committee initiates exit
- Treasurer approves exit
- Confirmation dialog before permanent action

## **User Entity Role Update**

### User.java
```java
public enum Role {
    ADMIN,              // System configuration and user management
    HR,                 // Human Resources - member exit/suspension initiation
    TREASURER,          // Financial transactions and bulk uploads
    LOAN_OFFICER,       // Loan processing
    CREDIT_COMMITTEE,   // Loan approval and bulk approval (Checker)
    AUDITOR,            // View-only access and audit logs
    TELLER,             // Cash handling
    CUSTOMER_SUPPORT,   // Member support
    MEMBER              // SACCO member (mobile app login)
}
```

## **Repository Updates**

### MemberExitRepository.java
```java
@Repository
public interface MemberExitRepository extends JpaRepository<MemberExit, Long> {
    Optional<MemberExit> findByMemberId(Long memberId);
    Optional<MemberExit> findByMemberIdAndStatus(Long memberId, String status);
    List<MemberExit> findByApprovedByIsNull();
    List<MemberExit> findByApprovedByIsNotNull();
}
```
