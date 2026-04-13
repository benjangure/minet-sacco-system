package com.minet.sacco.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class LoanApplicationRequest {

    @NotNull
    private Long memberId;

    @NotNull
    private Long loanProductId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotNull
    @Min(1)
    private Integer termMonths;

    private List<GuarantorRequest> guarantors;  // Changed from List<Long> to List<GuarantorRequest>

    private String purpose;

    // Getters and Setters
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public Long getLoanProductId() { return loanProductId; }
    public void setLoanProductId(Long loanProductId) { this.loanProductId = loanProductId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public List<GuarantorRequest> getGuarantors() { return guarantors; }
    public void setGuarantors(List<GuarantorRequest> guarantors) { this.guarantors = guarantors; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
