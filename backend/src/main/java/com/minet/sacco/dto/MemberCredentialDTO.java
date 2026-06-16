package com.minet.sacco.dto;

import java.time.LocalDateTime;

public class MemberCredentialDTO {
    private Long id;
    private Long memberId;
    private String username;
    private String memberName;
    private String email;
    private boolean hasNationalId;
    private boolean emailSent;
    private LocalDateTime emailSentAt;
    private boolean passwordChanged;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime createdAt;
    
    public MemberCredentialDTO() {}
    
    public MemberCredentialDTO(Long id, Long memberId, String username, String memberName, 
                              String email, boolean hasNationalId, boolean emailSent, 
                              LocalDateTime emailSentAt, boolean passwordChanged, 
                              LocalDateTime passwordChangedAt, LocalDateTime createdAt) {
        this.id = id;
        this.memberId = memberId;
        this.username = username;
        this.memberName = memberName;
        this.email = email;
        this.hasNationalId = hasNationalId;
        this.emailSent = emailSent;
        this.emailSentAt = emailSentAt;
        this.passwordChanged = passwordChanged;
        this.passwordChangedAt = passwordChangedAt;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public boolean isHasNationalId() { return hasNationalId; }
    public void setHasNationalId(boolean hasNationalId) { this.hasNationalId = hasNationalId; }
    
    public boolean isEmailSent() { return emailSent; }
    public void setEmailSent(boolean emailSent) { this.emailSent = emailSent; }
    
    public LocalDateTime getEmailSentAt() { return emailSentAt; }
    public void setEmailSentAt(LocalDateTime emailSentAt) { this.emailSentAt = emailSentAt; }
    
    public boolean isPasswordChanged() { return passwordChanged; }
    public void setPasswordChanged(boolean passwordChanged) { this.passwordChanged = passwordChanged; }
    
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}