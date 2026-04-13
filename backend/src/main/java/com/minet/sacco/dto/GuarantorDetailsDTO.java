package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for displaying guarantor details with current status
 * Used for staff visibility and member tracking
 */
public class GuarantorDetailsDTO {
    
    private Long guarantorId;
    private Long memberId;
    private String memberNumber;
    private String firstName;
    private String lastName;
    private String status;  // PENDING, ACCEPTED, REJECTED, ACTIVE, DECLINED, RELEASED
    private BigDecimal guaranteeAmount;  // Original guarantee amount
    private BigDecimal frozenPledge;  // Current frozen amount (may be less due to repayments)
    private BigDecimal availableCapacity;  // Available capacity for new guarantees
    private String eligibilityStatus;  // ELIGIBLE, NOT_ELIGIBLE
    private String eligibilityErrors;  // Errors if not eligible
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private boolean selfGuarantee;  // True if member is self-guaranteeing
    
    // Constructors
    public GuarantorDetailsDTO() {}
    
    public GuarantorDetailsDTO(Long guarantorId, Long memberId, String memberNumber, 
                               String firstName, String lastName, String status,
                               BigDecimal guaranteeAmount, BigDecimal frozenPledge) {
        this.guarantorId = guarantorId;
        this.memberId = memberId;
        this.memberNumber = memberNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.guaranteeAmount = guaranteeAmount;
        this.frozenPledge = frozenPledge;
    }
    
    // Getters and Setters
    public Long getGuarantorId() {
        return guarantorId;
    }
    
    public void setGuarantorId(Long guarantorId) {
        this.guarantorId = guarantorId;
    }
    
    public Long getMemberId() {
        return memberId;
    }
    
    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }
    
    public String getMemberNumber() {
        return memberNumber;
    }
    
    public void setMemberNumber(String memberNumber) {
        this.memberNumber = memberNumber;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigDecimal getGuaranteeAmount() {
        return guaranteeAmount;
    }
    
    public void setGuaranteeAmount(BigDecimal guaranteeAmount) {
        this.guaranteeAmount = guaranteeAmount;
    }
    
    public BigDecimal getFrozenPledge() {
        return frozenPledge;
    }
    
    public void setFrozenPledge(BigDecimal frozenPledge) {
        this.frozenPledge = frozenPledge;
    }
    
    public BigDecimal getAvailableCapacity() {
        return availableCapacity;
    }
    
    public void setAvailableCapacity(BigDecimal availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
    
    public String getEligibilityStatus() {
        return eligibilityStatus;
    }
    
    public void setEligibilityStatus(String eligibilityStatus) {
        this.eligibilityStatus = eligibilityStatus;
    }
    
    public String getEligibilityErrors() {
        return eligibilityErrors;
    }
    
    public void setEligibilityErrors(String eligibilityErrors) {
        this.eligibilityErrors = eligibilityErrors;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    public boolean isSelfGuarantee() {
        return selfGuarantee;
    }
    
    public void setSelfGuarantee(boolean selfGuarantee) {
        this.selfGuarantee = selfGuarantee;
    }
}
