package com.minet.sacco.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for creating a guarantor with their next of kin (backup) guarantor
 */
public class GuarantorWithNokRequest {

    @NotNull(message = "Primary guarantor ID is required")
    private Long guarantorId;

    @NotNull(message = "Guarantee amount is required")
    @DecimalMin(value = "0.01", message = "Guarantee amount must be greater than zero")
    private BigDecimal guaranteeAmount;

    private boolean selfGuarantee = false;

    // Next of Kin guarantor details
    @NotNull(message = "Next of kin guarantor ID is required")
    private Long nextOfKinGuarantorId;

    // NOK guarantee amount (should match primary, but included for validation)
    @NotNull(message = "Next of kin guarantee amount is required")
    @DecimalMin(value = "0.01", message = "Next of kin guarantee amount must be greater than zero")
    private BigDecimal nextOfKinGuaranteeAmount;

    // Constructors
    public GuarantorWithNokRequest() {}

    public GuarantorWithNokRequest(Long guarantorId, BigDecimal guaranteeAmount, 
                                    Long nextOfKinGuarantorId, BigDecimal nextOfKinGuaranteeAmount) {
        this.guarantorId = guarantorId;
        this.guaranteeAmount = guaranteeAmount;
        this.nextOfKinGuarantorId = nextOfKinGuarantorId;
        this.nextOfKinGuaranteeAmount = nextOfKinGuaranteeAmount;
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
