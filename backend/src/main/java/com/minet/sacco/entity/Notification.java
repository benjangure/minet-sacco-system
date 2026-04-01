package com.minet.sacco.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String message;

    @NotBlank
    @Column(length = 50)
    private String type;

    @Column(name = "is_read", columnDefinition = "TINYINT(1)")
    private boolean read = false;

    @Column(length = 50)
    private String targetRole; // Role this notification was sent to (e.g., MEMBER, TELLER, LOAN_OFFICER)

    // Context fields for personalized notifications
    private Long loanId;
    private Long memberId;
    private Long depositRequestId;
    private Long guarantorId;
    private String category; // LOAN_APPLICATION, LOAN_APPROVAL, DEPOSIT_REQUEST, GUARANTOR_REQUEST, etc.

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public Long getDepositRequestId() { return depositRequestId; }
    public void setDepositRequestId(Long depositRequestId) { this.depositRequestId = depositRequestId; }

    public Long getGuarantorId() { return guarantorId; }
    public void setGuarantorId(Long guarantorId) { this.guarantorId = guarantorId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
