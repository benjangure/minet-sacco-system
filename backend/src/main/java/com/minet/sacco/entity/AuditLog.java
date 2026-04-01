package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_entity_id", columnList = "entity_id")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Who performed the action

    @Column(length = 50)
    private String action; // APPROVE, REJECT, DISBURSE, CREATE, UPDATE, DELETE, etc.

    @Column(length = 50)
    private String entityType; // LOAN, DEPOSIT_REQUEST, GUARANTOR, MEMBER, etc.

    private Long entityId; // ID of the entity being acted upon

    @Column(columnDefinition = "TEXT")
    private String entityDetails; // JSON or text description of the entity

    @Column(columnDefinition = "TEXT")
    private String comments; // Reason/comments for the action

    private LocalDateTime timestamp;

    @Column(length = 50)
    private String status; // SUCCESS, FAILURE, etc.

    @Column(columnDefinition = "TEXT", name = "error_message")
    private String errorMessage; // If status is FAILURE

    private String ipAddress; // IP address of the requester

    @Column(length = 100)
    private String userAgent; // Browser/client info

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public String getEntityDetails() { return entityDetails; }
    public void setEntityDetails(String entityDetails) { this.entityDetails = entityDetails; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
