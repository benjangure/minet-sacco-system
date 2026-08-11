package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Phase A: Low-risk field editing (individual loans only)
 * Now includes guarantor management for treasurer workflow
 * Editable fields: loanStatus, disbursementDate, interestRate, outstandingBalance, interestCollected, purpose, guarantors
 */
public class LoanFieldUpdateDTO {
    
    @JsonProperty("loanStatus")
    private String loanStatus;
    
    @JsonProperty("disbursementDate")
    private LocalDate disbursementDate;
    
    @JsonProperty("interestRate")
    private BigDecimal interestRate;
    
    @JsonProperty("outstandingBalance")
    private BigDecimal outstandingBalance;

    @JsonProperty("interestCollected")
    private BigDecimal interestCollected;
    
    @JsonProperty("purpose")
    private String purpose;
    
    @JsonProperty("guarantorshipType")
    private String guarantorshipType;
    
    @JsonProperty("guarantors")
    private List<GuarantorData> guarantors;
    
    public static class GuarantorData {
        @JsonProperty("employeeId")
        private String employeeId;
        
        @JsonProperty("pledgeAmount")
        private BigDecimal pledgeAmount;
        
        public String getEmployeeId() { return employeeId; }
        public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
        
        public BigDecimal getPledgeAmount() { return pledgeAmount; }
        public void setPledgeAmount(BigDecimal pledgeAmount) { this.pledgeAmount = pledgeAmount; }
    }
    
    // Constructors
    public LoanFieldUpdateDTO() {}
    
    public LoanFieldUpdateDTO(String loanStatus, LocalDate disbursementDate,
                              BigDecimal interestRate, BigDecimal outstandingBalance,
                              BigDecimal interestCollected, String purpose,
                              String guarantorshipType, List<GuarantorData> guarantors) {
        this.loanStatus = loanStatus;
        this.disbursementDate = disbursementDate;
        this.interestRate = interestRate;
        this.outstandingBalance = outstandingBalance;
        this.interestCollected = interestCollected;
        this.purpose = purpose;
        this.guarantorshipType = guarantorshipType;
        this.guarantors = guarantors;
    }
    
    // Getters and Setters
    public String getLoanStatus() { return loanStatus; }
    public void setLoanStatus(String loanStatus) { this.loanStatus = loanStatus; }
    
    public LocalDate getDisbursementDate() { return disbursementDate; }
    public void setDisbursementDate(LocalDate disbursementDate) { this.disbursementDate = disbursementDate; }
    
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public BigDecimal getInterestCollected() { return interestCollected; }
    public void setInterestCollected(BigDecimal interestCollected) { this.interestCollected = interestCollected; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public String getGuarantorshipType() { return guarantorshipType; }
    public void setGuarantorshipType(String guarantorshipType) { this.guarantorshipType = guarantorshipType; }
    
    public List<GuarantorData> getGuarantors() { return guarantors; }
    public void setGuarantors(List<GuarantorData> guarantors) { this.guarantors = guarantors; }
    
    /**
     * Validation helper: check if at least one field is provided
     */
    public boolean hasAtLeastOneField() {
        return loanStatus != null ||
               disbursementDate != null ||
               interestRate != null ||
               outstandingBalance != null ||
               interestCollected != null ||
               (purpose != null && !purpose.trim().isEmpty()) ||
               (guarantors != null && !guarantors.isEmpty());
    }
}
