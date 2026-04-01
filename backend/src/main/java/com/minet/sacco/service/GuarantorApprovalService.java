package com.minet.sacco.service;

import com.minet.sacco.entity.Guarantor;
import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.GuarantorRepository;
import com.minet.sacco.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GuarantorApprovalService {

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditService auditService;

    /**
     * Guarantor accepts the loan guarantee
     */
    public void approveGuarantee(Long loanId, Long guarantorId) {
        Optional<Guarantor> guarantorOpt = guarantorRepository.findById(guarantorId);
        if (!guarantorOpt.isPresent()) {
            throw new RuntimeException("Guarantor not found");
        }

        Guarantor guarantor = guarantorOpt.get();
        guarantor.setStatus(Guarantor.Status.ACCEPTED);
        guarantorRepository.save(guarantor);

        // Check if all guarantors have approved
        Loan loan = guarantor.getLoan();
        List<Guarantor> allGuarantors = guarantorRepository.findByLoan(loan);
        
        boolean allApproved = allGuarantors.stream()
            .allMatch(g -> g.getStatus() == Guarantor.Status.ACCEPTED);

        if (allApproved) {
            // Move loan to loan officer review
            loan.setStatus(Loan.Status.PENDING_LOAN_OFFICER_REVIEW);
            loanRepository.save(loan);
            
            // Notify loan officers
            notificationService.notifyUsersByRole("LOAN_OFFICER", 
                "Loan " + loan.getLoanNumber() + " is ready for review. All guarantors have approved.", 
                "LOAN_APPROVAL", loan.getId(), loan.getMember().getId(), "GUARANTOR_APPROVED");
        }

        // Log audit event
        Optional<User> guarantorUserOpt = userService.getUserByMemberId(guarantor.getMember().getId());
        if (guarantorUserOpt.isPresent()) {
            String guarantorDetails = "Guarantor: " + guarantor.getMember().getFirstName() + " " + 
                                     guarantor.getMember().getLastName() + " - Loan #" + loan.getLoanNumber() + 
                                     " - Member: " + loan.getMember().getFirstName() + " " + loan.getMember().getLastName();
            auditService.logAction(guarantorUserOpt.get(), "APPROVE", "GUARANTOR", guarantor.getId(), guarantorDetails, "Guarantor approved loan guarantee", "SUCCESS");
        }
    }

    /**
     * Guarantor rejects the loan guarantee
     */
    public void rejectGuarantee(Long loanId, Long guarantorId, String reason) {
        Optional<Guarantor> guarantorOpt = guarantorRepository.findById(guarantorId);
        if (!guarantorOpt.isPresent()) {
            throw new RuntimeException("Guarantor not found");
        }

        Guarantor guarantor = guarantorOpt.get();
        guarantor.setStatus(Guarantor.Status.REJECTED);
        guarantorRepository.save(guarantor);

        // Revert loan to PENDING (member needs to find new guarantor)
        Loan loan = guarantor.getLoan();
        loan.setStatus(Loan.Status.PENDING);
        loan.setRejectionReason("Guarantor " + guarantor.getMember().getFirstName() + " " + guarantor.getMember().getLastName() + " rejected: " + reason);
        loanRepository.save(loan);

        // Notify ONLY the member who applied for the loan (not all members)
        String notificationMessage = "Your loan application was rejected by guarantor " + 
            guarantor.getMember().getFirstName() + " " + guarantor.getMember().getLastName() + 
            ". Reason: " + reason + ". Please select a different guarantor and reapply.";
        
        Optional<User> memberUserOpt = userService.getUserByMemberId(loan.getMember().getId());
        if (memberUserOpt.isPresent()) {
            notificationService.notifyUser(memberUserOpt.get().getId(), notificationMessage, "LOAN_REJECTION", loan.getId(), loan.getMember().getId(), "GUARANTOR_REJECTED");
        }

        // Log audit event
        Optional<User> guarantorUserOpt = userService.getUserByMemberId(guarantor.getMember().getId());
        if (guarantorUserOpt.isPresent()) {
            String guarantorDetails = "Guarantor: " + guarantor.getMember().getFirstName() + " " + 
                                     guarantor.getMember().getLastName() + " - Loan #" + loan.getLoanNumber() + 
                                     " - Member: " + loan.getMember().getFirstName() + " " + loan.getMember().getLastName();
            auditService.logAction(guarantorUserOpt.get(), "REJECT", "GUARANTOR", guarantor.getId(), guarantorDetails, reason, "SUCCESS");
        }
    }

    /**
     * Get pending guarantor approvals for a member
     */
    public List<Guarantor> getPendingGuarantorRequests(Long memberId) {
        return guarantorRepository.findByMemberIdAndStatus(memberId, Guarantor.Status.PENDING);
    }

    /**
     * Check if all guarantors for a loan have approved
     */
    public boolean allGuarantorsApproved(Long loanId) {
        Optional<Loan> loanOpt = loanRepository.findById(loanId);
        if (!loanOpt.isPresent()) {
            return false;
        }

        List<Guarantor> guarantors = guarantorRepository.findByLoan(loanOpt.get());
        return guarantors.stream().allMatch(g -> g.getStatus() == Guarantor.Status.ACCEPTED);
    }
}
