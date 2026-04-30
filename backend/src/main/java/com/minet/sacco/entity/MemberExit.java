package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "member_exits")
public class MemberExit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "exit_reason", nullable = false, length = 50)
    private String exitReason; // RETIREMENT, RESIGNATION, TERMINATION, DECEASED, OTHER

    @ManyToOne
    @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "savings_balance")
    private BigDecimal savingsBalance;

    @Column(name = "outstanding_loan")
    private BigDecimal outstandingLoan;

    @Column(name = "loan_deduction")
    private BigDecimal loanDeduction;

    @Column(name = "remaining_payout")
    private BigDecimal remainingPayout;

    @Column(name = "shares_refund")
    private BigDecimal sharesRefund = new BigDecimal("3000.00");

    @Column(name = "total_payout")
    private BigDecimal totalPayout;

    @Column(name = "exit_date")
    private LocalDateTime exitDate;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active_guarantor")
    private Boolean isActiveGuarantor = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public String getExitReason() { return exitReason; }
    public void setExitReason(String exitReason) { this.exitReason = exitReason; }

    public User getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(User initiatedBy) { this.initiatedBy = initiatedBy; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public BigDecimal getSavingsBalance() { return savingsBalance; }
    public void setSavingsBalance(BigDecimal savingsBalance) { this.savingsBalance = savingsBalance; }

    public BigDecimal getOutstandingLoan() { return outstandingLoan; }
    public void setOutstandingLoan(BigDecimal outstandingLoan) { this.outstandingLoan = outstandingLoan; }

    public BigDecimal getLoanDeduction() { return loanDeduction; }
    public void setLoanDeduction(BigDecimal loanDeduction) { this.loanDeduction = loanDeduction; }

    public BigDecimal getRemainingPayout() { return remainingPayout; }
    public void setRemainingPayout(BigDecimal remainingPayout) { this.remainingPayout = remainingPayout; }

    public BigDecimal getSharesRefund() { return sharesRefund; }
    public void setSharesRefund(BigDecimal sharesRefund) { this.sharesRefund = sharesRefund; }

    public BigDecimal getTotalPayout() { return totalPayout; }
    public void setTotalPayout(BigDecimal totalPayout) { this.totalPayout = totalPayout; }

    public LocalDateTime getExitDate() { return exitDate; }
    public void setExitDate(LocalDateTime exitDate) { this.exitDate = exitDate; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getIsActiveGuarantor() { return isActiveGuarantor; }
    public void setIsActiveGuarantor(Boolean isActiveGuarantor) { this.isActiveGuarantor = isActiveGuarantor; }
}
