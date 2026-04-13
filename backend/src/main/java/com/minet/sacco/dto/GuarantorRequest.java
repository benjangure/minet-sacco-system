package com.minet.sacco.dto;

import java.math.BigDecimal;

/**
 * DTO for guarantor request during loan application
 * Allows specifying custom guarantee amount per guarantor
 */
public class GuarantorRequest {
    
    private Long guarantorId;  // Member ID of the guarantor
    private BigDecimal guaranteeAmount;  // Amount this guarantor is pledging
    private boolean selfGuarantee;  // True if member is self-guaranteeing
    
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
}
