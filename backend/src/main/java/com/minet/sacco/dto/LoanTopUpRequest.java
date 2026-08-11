package com.minet.sacco.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for requesting a loan top-up
 * Adds additional funds to an existing loan
 */
public class LoanTopUpRequest {

    @NotNull(message = "Top-up amount is required")
    @DecimalMin(value = "1000.00", message = "Minimum top-up amount is KES 1,000")
    private BigDecimal topupAmount;

    private String purpose;

    private List<GuarantorRequest> newGuarantors;

    // Nested class for guarantor data
    public static class GuarantorRequest {
        private String guarantorMemberNumber;
        private BigDecimal guaranteeAmount;

        public String getGuarantorMemberNumber() {
            return guarantorMemberNumber;
        }

        public void setGuarantorMemberNumber(String guarantorMemberNumber) {
            this.guarantorMemberNumber = guarantorMemberNumber;
        }

        public BigDecimal getGuaranteeAmount() {
            return guaranteeAmount;
        }

        public void setGuaranteeAmount(BigDecimal guaranteeAmount) {
            this.guaranteeAmount = guaranteeAmount;
        }
    }

    // Getters and Setters
    public BigDecimal getTopupAmount() {
        return topupAmount;
    }

    public void setTopupAmount(BigDecimal topupAmount) {
        this.topupAmount = topupAmount;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public List<GuarantorRequest> getNewGuarantors() {
        return newGuarantors;
    }

    public void setNewGuarantors(List<GuarantorRequest> newGuarantors) {
        this.newGuarantors = newGuarantors;
    }
}
