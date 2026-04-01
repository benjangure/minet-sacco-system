package com.minet.sacco.dto;

import jakarta.validation.constraints.NotNull;

public class MemberApprovalRequest {

    @NotNull
    private Long memberId;

    @NotNull
    private Boolean approved;

    private String rejectionReason;

    public MemberApprovalRequest() {}

    public MemberApprovalRequest(Long memberId, Boolean approved, String rejectionReason) {
        this.memberId = memberId;
        this.approved = approved;
        this.rejectionReason = rejectionReason;
    }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
