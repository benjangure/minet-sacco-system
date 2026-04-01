package com.minet.sacco.service;

import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.Member;
import com.minet.sacco.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MemberValidationService {

    private static final Logger log = LoggerFactory.getLogger(MemberValidationService.class);

    @Autowired
    private LoanRepository loanRepository;

    /**
     * Validate a member for loan eligibility
     * Checks: member is ACTIVE, has completed KYC, no defaulted loans
     */
    public MemberValidationResult validateMember(Member member) {
        log.debug("=== MEMBER VALIDATION START ===");
        log.debug("Member: {} {}, ID: {}", member.getFirstName(), member.getLastName(), member.getId());

        MemberValidationResult result = new MemberValidationResult();
        result.setMemberId(member.getId());
        result.setMemberName(member.getFirstName() + " " + member.getLastName());
        result.setIsEligible(true);

        // Check 1: Member must be ACTIVE
        log.debug("Check 1: Member status = {}", member.getStatus());
        if (member.getStatus() != Member.Status.ACTIVE) {
            log.debug("FAIL: Not ACTIVE");
            result.addError("Member is not ACTIVE (Current status: " + member.getStatus() + ")");
            result.setIsEligible(false);
            log.debug("=== RESULT: NOT ELIGIBLE (Check 1 - Status) ===");
            return result;
        }
        log.debug("PASS: Member is ACTIVE");

        // Check 2: Member must have completed KYC
        log.debug("Check 2: Member KYC status = {}", member.getKycCompletionStatus());
        if (member.getKycCompletionStatus() == null || 
            (member.getKycCompletionStatus() != Member.KycCompletionStatus.COMPLETE && 
             member.getKycCompletionStatus() != Member.KycCompletionStatus.VERIFIED)) {
            log.debug("FAIL: KYC not completed");
            result.addError("Member has not completed KYC verification");
            result.setIsEligible(false);
            log.debug("=== RESULT: NOT ELIGIBLE (Check 2 - KYC) ===");
            return result;
        }
        log.debug("PASS: KYC completed");

        // Check 3: Check for defaulted loans
        log.debug("Check 3: Checking for defaulted loans");
        List<Loan> memberLoans = loanRepository.findByMemberId(member.getId());
        log.debug("Total loans for member: {}", memberLoans.size());
        
        boolean hasDefaultedLoan = memberLoans.stream()
                .anyMatch(loan -> loan.getStatus() == Loan.Status.DEFAULTED);
        
        log.debug("Has defaulted loan: {}", hasDefaultedLoan);
        if (hasDefaultedLoan) {
            log.debug("FAIL: Has defaulted loans");
            result.addError("Member has defaulted loans");
            result.setIsEligible(false);
            log.debug("=== RESULT: NOT ELIGIBLE (Check 3 - Defaulted Loans) ===");
            return result;
        }
        log.debug("PASS: No defaulted loans");

        log.debug("=== FINAL RESULT ===");
        log.debug("Member: {}", result.getMemberName());
        log.debug("Is Eligible: {}", result.isEligible());
        log.debug("Errors: {}", result.getErrors());
        log.debug("=== END VALIDATION ===");
        
        return result;
    }

    /**
     * Result class for member validation
     */
    public static class MemberValidationResult {
        private Long memberId;
        private String memberName;
        
        @com.fasterxml.jackson.annotation.JsonProperty("isEligible")
        private boolean isEligible;
        
        private List<String> errors = new ArrayList<>();

        public void addError(String error) {
            this.errors.add(error);
        }

        // Getters and Setters
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }

        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }

        public boolean isEligible() { return isEligible; }
        public void setIsEligible(boolean eligible) { isEligible = eligible; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }
}
