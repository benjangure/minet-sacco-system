package com.minet.sacco.dto;

import java.util.List;
import java.util.Map;

public class LoanApprovalWithValidationDTO {
    private Long loanId;
    private Boolean approved;
    private String comments;
    private List<Map<String, Object>> guarantorValidations; // Validation results for each guarantor
    private String validationSummary; // Human-readable summary
    private Boolean allGuarantorsEligible; // Flag if all guarantors meet criteria

    public LoanApprovalWithValidationDTO() {}

    public LoanApprovalWithValidationDTO(Long loanId, Boolean approved, String comments) {
        this.loanId = loanId;
        this.approved = approved;
        this.comments = comments;
    }

    // Getters and Setters
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public List<Map<String, Object>> getGuarantorValidations() { return guarantorValidations; }
    public void setGuarantorValidations(List<Map<String, Object>> guarantorValidations) { this.guarantorValidations = guarantorValidations; }

    public String getValidationSummary() { return validationSummary; }
    public void setValidationSummary(String validationSummary) { this.validationSummary = validationSummary; }

    public Boolean getAllGuarantorsEligible() { return allGuarantorsEligible; }
    public void setAllGuarantorsEligible(Boolean allGuarantorsEligible) { this.allGuarantorsEligible = allGuarantorsEligible; }
}
