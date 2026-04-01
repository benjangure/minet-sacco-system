package com.minet.sacco.dto;

import java.time.LocalDateTime;

public class MemberNotificationDTO {
    private Long id;
    private String type; // GUARANTOR_REQUEST, LOAN_APPROVED, LOAN_REJECTED, GUARANTOR_APPROVED, GUARANTOR_REJECTED, LOAN_DISBURSED, REPAYMENT_DUE, CONTRIBUTION_DUE
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Long relatedLoanId;
    private Long relatedGuarantorRequestId;

    public MemberNotificationDTO() {}

    public MemberNotificationDTO(Long id, String type, String title, String message, Boolean isRead, 
                                 LocalDateTime createdAt, Long relatedLoanId, Long relatedGuarantorRequestId) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.relatedLoanId = relatedLoanId;
        this.relatedGuarantorRequestId = relatedGuarantorRequestId;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getRelatedLoanId() { return relatedLoanId; }
    public void setRelatedLoanId(Long relatedLoanId) { this.relatedLoanId = relatedLoanId; }

    public Long getRelatedGuarantorRequestId() { return relatedGuarantorRequestId; }
    public void setRelatedGuarantorRequestId(Long relatedGuarantorRequestId) { this.relatedGuarantorRequestId = relatedGuarantorRequestId; }
}
