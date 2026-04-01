package com.minet.sacco.dto;

import jakarta.validation.constraints.NotNull;

public class DeletionApprovalDTO {
    @NotNull
    private Long requestId;
    
    @NotNull
    private Boolean approved;
    
    private String rejectionReason;

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
