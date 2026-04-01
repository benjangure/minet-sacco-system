package com.minet.sacco.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO for returning eligibility validation information during loan approval
 * Used by Credit Committee to see member and guarantor eligibility before approving
 */
public class LoanApprovalValidationDTO {
    private Long loanId;
    private String memberNumber;
    private String loanProductName;
    private String loanAmount;
    private String purpose;
    private Boolean productEnabled;
    private String productError;
    
    // Member eligibility
    private MemberEligibilityInfo memberInfo;
    
    // Guarantor validation results
    private List<GuarantorEligibilityInfo> validationResults;
    private Integer guarantorCount;
    
    // Decision
    private Boolean canApprove;
    private String decisionReason;

    public static class MemberEligibilityInfo {
        private String memberName;
        private String status;
        private String savingsBalance;
        private String sharesBalance;
        private String totalBalance;
        private String totalOutstandingBalance;
        private Integer activeLoans;
        private Integer defaultedLoans;
        private Boolean isEligible;
        private List<String> errors;
        private List<String> warnings;

        // Getters and Setters
        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getSavingsBalance() { return savingsBalance; }
        public void setSavingsBalance(String savingsBalance) { this.savingsBalance = savingsBalance; }
        
        public String getSharesBalance() { return sharesBalance; }
        public void setSharesBalance(String sharesBalance) { this.sharesBalance = sharesBalance; }
        
        public String getTotalBalance() { return totalBalance; }
        public void setTotalBalance(String totalBalance) { this.totalBalance = totalBalance; }
        
        public String getTotalOutstandingBalance() { return totalOutstandingBalance; }
        public void setTotalOutstandingBalance(String totalOutstandingBalance) { this.totalOutstandingBalance = totalOutstandingBalance; }
        
        public Integer getActiveLoans() { return activeLoans; }
        public void setActiveLoans(Integer activeLoans) { this.activeLoans = activeLoans; }
        
        public Integer getDefaultedLoans() { return defaultedLoans; }
        public void setDefaultedLoans(Integer defaultedLoans) { this.defaultedLoans = defaultedLoans; }
        
        public Boolean getIsEligible() { return isEligible; }
        public void setIsEligible(Boolean isEligible) { this.isEligible = isEligible; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }

    public static class GuarantorEligibilityInfo {
        private Long guarantorId;
        private String guarantorName;
        private String savingsBalance;
        private String sharesBalance;
        private String totalBalance;
        private String outstandingBalance;
        private String availableGuaranteeCapacity;
        private Boolean isEligible;
        private List<String> errors;
        private List<String> warnings;

        // Getters and Setters
        public Long getGuarantorId() { return guarantorId; }
        public void setGuarantorId(Long guarantorId) { this.guarantorId = guarantorId; }
        
        public String getGuarantorName() { return guarantorName; }
        public void setGuarantorName(String guarantorName) { this.guarantorName = guarantorName; }
        
        public String getSavingsBalance() { return savingsBalance; }
        public void setSavingsBalance(String savingsBalance) { this.savingsBalance = savingsBalance; }
        
        public String getSharesBalance() { return sharesBalance; }
        public void setSharesBalance(String sharesBalance) { this.sharesBalance = sharesBalance; }
        
        public String getTotalBalance() { return totalBalance; }
        public void setTotalBalance(String totalBalance) { this.totalBalance = totalBalance; }
        
        public String getOutstandingBalance() { return outstandingBalance; }
        public void setOutstandingBalance(String outstandingBalance) { this.outstandingBalance = outstandingBalance; }

        public String getAvailableGuaranteeCapacity() { return availableGuaranteeCapacity; }
        public void setAvailableGuaranteeCapacity(String availableGuaranteeCapacity) { this.availableGuaranteeCapacity = availableGuaranteeCapacity; }

        public Boolean getIsEligible() { return isEligible; }
        public void setIsEligible(Boolean isEligible) { this.isEligible = isEligible; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }

    // Getters and Setters
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    
    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }
    
    public String getLoanProductName() { return loanProductName; }
    public void setLoanProductName(String loanProductName) { this.loanProductName = loanProductName; }
    
    public String getLoanAmount() { return loanAmount; }
    public void setLoanAmount(String loanAmount) { this.loanAmount = loanAmount; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public Boolean getProductEnabled() { return productEnabled; }
    public void setProductEnabled(Boolean productEnabled) { this.productEnabled = productEnabled; }
    
    public String getProductError() { return productError; }
    public void setProductError(String productError) { this.productError = productError; }
    
    public MemberEligibilityInfo getMemberInfo() { return memberInfo; }
    public void setMemberInfo(MemberEligibilityInfo memberInfo) { this.memberInfo = memberInfo; }
    
    public List<GuarantorEligibilityInfo> getValidationResults() { return validationResults; }
    public void setValidationResults(List<GuarantorEligibilityInfo> validationResults) { this.validationResults = validationResults; }
    
    public Integer getGuarantorCount() { return guarantorCount; }
    public void setGuarantorCount(Integer guarantorCount) { this.guarantorCount = guarantorCount; }
    
    public Boolean getCanApprove() { return canApprove; }
    public void setCanApprove(Boolean canApprove) { this.canApprove = canApprove; }
    
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String decisionReason) { this.decisionReason = decisionReason; }
}
