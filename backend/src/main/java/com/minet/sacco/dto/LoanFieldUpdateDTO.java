package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Phase A: Low-risk field editing (individual loans only)
 * CRITICAL: This DTO must NEVER contain guarantor data
 * Editable fields only: loanStatus, disbursementDate, interestRate, outstandingBalance, purpose
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
    
    @JsonProperty("purpose")
    private String purpose;
    
    // Constructors
    public LoanFieldUpdateDTO() {}
    
    public LoanFieldUpdateDTO(String loanStatus, LocalDate disbursementDate,
                              BigDecimal interestRate, BigDecimal outstandingBalance,
                              String purpose) {
        this.loanStatus = loanStatus;
        this.disbursementDate = disbursementDate;
        this.interestRate = interestRate;
        this.outstandingBalance = outstandingBalance;
        this.purpose = purpose;
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
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    /**
     * Validation helper: check if at least one field is provided
     */
    public boolean hasAtLeastOneField() {
        return loanStatus != null ||
               disbursementDate != null ||
               interestRate != null ||
               outstandingBalance != null ||
               (purpose != null && !purpose.trim().isEmpty());
    }
}
