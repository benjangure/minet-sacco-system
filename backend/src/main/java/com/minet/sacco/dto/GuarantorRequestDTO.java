package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class GuarantorRequestDTO {
    private Long id;
    private Long loanId;
    private Long guarantorId;
    private String guarantorName;
    private String guarantorPhone;
    private String status; // PENDING, ACCEPTED, REJECTED
    private BigDecimal pledgeAmount;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private String loanProductName;
    private BigDecimal loanAmount;
    private String borrowerName;

    public GuarantorRequestDTO() {}

    public GuarantorRequestDTO(Long id, Long loanId, Long guarantorId, String guarantorName, 
                               String guarantorPhone, String status, BigDecimal pledgeAmount, 
                               LocalDateTime createdAt, String loanProductName, BigDecimal loanAmount, 
                               String borrowerName) {
        this.id = id;
        this.loanId = loanId;
        this.guarantorId = guarantorId;
        this.guarantorName = guarantorName;
        this.guarantorPhone = guarantorPhone;
        this.status = status;
        this.pledgeAmount = pledgeAmount;
        this.createdAt = createdAt;
        this.loanProductName = loanProductName;
        this.loanAmount = loanAmount;
        this.borrowerName = borrowerName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getGuarantorId() { return guarantorId; }
    public void setGuarantorId(Long guarantorId) { this.guarantorId = guarantorId; }

    public String getGuarantorName() { return guarantorName; }
    public void setGuarantorName(String guarantorName) { this.guarantorName = guarantorName; }

    public String getGuarantorPhone() { return guarantorPhone; }
    public void setGuarantorPhone(String guarantorPhone) { this.guarantorPhone = guarantorPhone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getPledgeAmount() { return pledgeAmount; }
    public void setPledgeAmount(BigDecimal pledgeAmount) { this.pledgeAmount = pledgeAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    public String getLoanProductName() { return loanProductName; }
    public void setLoanProductName(String loanProductName) { this.loanProductName = loanProductName; }

    public BigDecimal getLoanAmount() { return loanAmount; }
    public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }
}
