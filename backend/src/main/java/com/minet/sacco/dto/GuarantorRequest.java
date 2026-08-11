package com.minet.sacco.dto;

import java.math.BigDecimal;

/**
 * DTO for guarantor request during loan application
 * Allows specifying custom guarantee amount per guarantor
 * Now supports Next of Kin (backup) guarantors
 */
public class GuarantorRequest {
    
    private Long guarantorId;  // Member ID of the guarantor
    private BigDecimal guaranteeAmount;  // Amount this guarantor is pledging
    private boolean selfGuarantee;  // True if member is self-guaranteeing
    
    // Next of Kin (NOK) Guarantor Support
    private Long nextOfKinGuarantorId;  // Member ID of the NOK (backup) guarantor
    private BigDecimal nextOfKinGuaranteeAmount;  // Amount NOK is pledging (should match guaranteeAmount)
    
    // Constructors
    public GuarantorRequest() {}
    
    public GuarantorRequest(Long guarantorId, BigDecimal guaranteeAmount) {
        this.guarantorId = guarantorId;
        this.guaranteeAmount = guaranteeAmount;
        this.selfGuarantee = false;
    }
    
    public GuarantorRequest(Long guarantorId, BigDecimal guaranteeAmount, boolean selfGuarantee) {
        this.guarantorId = guarantorId;
        this.guaranteeAmount = guaranteeAmount;
        this.selfGuarantee = selfGuarantee;
    }
    
    public GuarantorRequest(Long guarantorId, BigDecimal guaranteeAmount, 
                           Long nextOfKinGuarantorId, BigDecimal nextOfKinGuaranteeAmount) {
        this.guarantorId = guarantorId;
        this.guaranteeAmount = guaranteeAmount;
        this.nextOfKinGuarantorId = nextOfKinGuarantorId;
        this.nextOfKinGuaranteeAmount = nextOfKinGuaranteeAmount;
        this.selfGuarantee = false;
    }
    
    // Getters and Setters
    public Long getGuarantorId() {
        return guarantorId;
    }
    
    public void setGuarantorId(Long guarantorId) {
        this.guarantorId = guarantorId;
    }
    
    public BigDecimal getGuaranteeAmount() {
        return guaranteeAmount;
    }
    
    public void setGuaranteeAmount(BigDecimal guaranteeAmount) {
        this.guaranteeAmount = guaranteeAmount;
    }
    
    public boolean isSelfGuarantee() {
        return selfGuarantee;
    }
    
    public void setSelfGuarantee(boolean selfGuarantee) {
        this.selfGuarantee = selfGuarantee;
    }
    
    public Long getNextOfKinGuarantorId() {
        return nextOfKinGuarantorId;
    }
    
    public void setNextOfKinGuarantorId(Long nextOfKinGuarantorId) {
        this.nextOfKinGuarantorId = nextOfKinGuarantorId;
    }
    
    public BigDecimal getNextOfKinGuaranteeAmount() {
        return nextOfKinGuaranteeAmount;
    }
    
    public void setNextOfKinGuaranteeAmount(BigDecimal nextOfKinGuaranteeAmount) {
        this.nextOfKinGuaranteeAmount = nextOfKinGuaranteeAmount;
    }
}
