package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LoanUpdateRequestDTO {
    
    @JsonProperty("disbursementDate")
    private LocalDate disbursementDate;
    
    @JsonProperty("outstandingBalance")
    private BigDecimal outstandingBalance;
    
    @JsonProperty("termMonths")
    private Integer termMonths;
    
    @JsonProperty("guarantorshipType")
    private String guarantorshipType;
    
    @JsonProperty("guarantors")
    private List<GuarantorPairDTO> guarantors;
    
    // Constructors
    public LoanUpdateRequestDTO() {}
    
    public LoanUpdateRequestDTO(LocalDate disbursementDate, BigDecimal outstandingBalance, 
                                Integer termMonths, String guarantorshipType, 
                                List<GuarantorPairDTO> guarantors) {
        this.disbursementDate = disbursementDate;
        this.outstandingBalance = outstandingBalance;
        this.termMonths = termMonths;
        this.guarantorshipType = guarantorshipType;
        this.guarantors = guarantors;
    }
    
    // Getters and Setters
    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }
    
    public void setDisbursementDate(LocalDate disbursementDate) {
        this.disbursementDate = disbursementDate;
    }
    
    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }
    
    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }
    
    public Integer getTermMonths() {
        return termMonths;
    }
    
    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }
    
    public String getGuarantorshipType() {
        return guarantorshipType;
    }
    
    public void setGuarantorshipType(String guarantorshipType) {
        this.guarantorshipType = guarantorshipType;
    }
    
    public List<GuarantorPairDTO> getGuarantors() {
        return guarantors;
    }
    
    public void setGuarantors(List<GuarantorPairDTO> guarantors) {
        this.guarantors = guarantors;
    }
    
    // Inner DTO for guarantor pairs
    public static class GuarantorPairDTO {
        @JsonProperty("employeeId")
        private String employeeId;
        
        @JsonProperty("pledgeAmount")
        private BigDecimal pledgeAmount;
        
        // Constructors
        public GuarantorPairDTO() {}
        
        public GuarantorPairDTO(String employeeId, BigDecimal pledgeAmount) {
            this.employeeId = employeeId;
            this.pledgeAmount = pledgeAmount;
        }
        
        // Getters and Setters
        public String getEmployeeId() {
            return employeeId;
        }
        
        public void setEmployeeId(String employeeId) {
            this.employeeId = employeeId;
        }
        
        public BigDecimal getPledgeAmount() {
            return pledgeAmount;
        }
        
        public void setPledgeAmount(BigDecimal pledgeAmount) {
            this.pledgeAmount = pledgeAmount;
        }
    }
}
