package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to track member credentials and email delivery status
 * Provides admin visibility into password distribution
 */
@Entity
@Table(name = "member_credentials")
public class MemberCredential {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    
    @Column(name = "username", nullable = false, length = 50)
    private String username;
    
    @Column(name = "member_name", nullable = false, length = 100)
    private String memberName;
    
    @Column(name = "email", length = 100)
    private String email;
    
    @Column(name = "has_national_id", nullable = false)
    private boolean hasNationalId;
    
    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;
    
    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;
    
    @Column(name = "password_changed", nullable = false)
    private boolean passwordChanged = false;
    
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    // Constructors
    public MemberCredential() {}
    
    public MemberCredential(Long memberId, String username, String memberName, String email, boolean hasNationalId, Long createdBy) {
        this.memberId = memberId;
        this.username = username;
        this.memberName = memberName;
        this.email = email;
        this.hasNationalId = hasNationalId;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
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
    
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}