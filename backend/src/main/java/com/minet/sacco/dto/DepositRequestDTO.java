package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DepositRequestDTO {

    private Long id;
    private Long memberId;
    private String memberNumber;
    private String memberName;
    private Long accountId;
    private String accountType;
    private BigDecimal claimedAmount;
    private BigDecimal confirmedAmount;
    private String description;
    private String receiptFileName;
    private String status;
    private String approvalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    public DepositRequestDTO(Long id, Long memberId, String memberNumber, String memberName, 
                            Long accountId, String accountType, BigDecimal claimedAmount, 
                            BigDecimal confirmedAmount, String description, String receiptFileName, 
                            String status, String approvalNotes, LocalDateTime createdAt, LocalDateTime approvedAt) {
        this.id = id;
        this.memberId = memberId;
        this.memberNumber = memberNumber;
        this.memberName = memberName;
        this.accountId = accountId;
        this.accountType = accountType;
        this.claimedAmount = claimedAmount;
        this.confirmedAmount = confirmedAmount;
        this.description = description;
        this.receiptFileName = receiptFileName;
        this.status = status;
        this.approvalNotes = approvalNotes;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public BigDecimal getClaimedAmount() { return claimedAmount; }
    public void setClaimedAmount(BigDecimal claimedAmount) { this.claimedAmount = claimedAmount; }

    public BigDecimal getConfirmedAmount() { return confirmedAmount; }
    public void setConfirmedAmount(BigDecimal confirmedAmount) { this.confirmedAmount = confirmedAmount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReceiptFileName() { return receiptFileName; }
    public void setReceiptFileName(String receiptFileName) { this.receiptFileName = receiptFileName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApprovalNotes() { return approvalNotes; }
    public void setApprovalNotes(String approvalNotes) { this.approvalNotes = approvalNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
}
