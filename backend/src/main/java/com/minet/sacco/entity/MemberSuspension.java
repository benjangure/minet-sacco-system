package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_suspensions")
public class MemberSuspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @ManyToOne
    @JoinColumn(name = "suspended_by", nullable = false)
    private User suspendedBy;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @ManyToOne
    @JoinColumn(name = "lifted_by")
    private User liftedBy;

    @Column(name = "lifted_at")
    private LocalDateTime liftedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        suspendedAt = LocalDateTime.now();
        isActive = true;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public User getSuspendedBy() { return suspendedBy; }
    public void setSuspendedBy(User suspendedBy) { this.suspendedBy = suspendedBy; }

    public LocalDateTime getSuspendedAt() { return suspendedAt; }
    public void setSuspendedAt(LocalDateTime suspendedAt) { this.suspendedAt = suspendedAt; }

    public User getLiftedBy() { return liftedBy; }
    public void setLiftedBy(User liftedBy) { this.liftedBy = liftedBy; }

    public LocalDateTime getLiftedAt() { return liftedAt; }
    public void setLiftedAt(LocalDateTime liftedAt) { this.liftedAt = liftedAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
