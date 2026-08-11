package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity to track the audit trail of loan top-up transactions
 * Each record represents a single top-up event on a loan
 */
@Entity
@Table(name = "loan_topup_history")
public class LoanTopUpHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "topup_amount", nullable = false)
    private BigDecimal topupAmount;

    @Column(name = "outstanding_before_topup", nullable = false)
    private BigDecimal outstandingBeforeTopup;

    @Column(name = "outstanding_after_topup", nullable = false)
    private BigDecimal outstandingAfterTopup;

    @Column(name = "principal_paid_before_topup", nullable = false)
    private BigDecimal principalPaidBeforeTopup;

    @Column(name = "new_guarantors_added")
    private Integer newGuarantorsAdded = 0;

    @Column(name = "topup_date", nullable = false)
    private LocalDateTime topupDate;

    @ManyToOne
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public LoanTopUpHistory() {
        this.topupDate = LocalDateTime.now();
    }

    public LoanTopUpHistory(Loan loan, BigDecimal topupAmount, BigDecimal outstandingBefore,
                            BigDecimal outstandingAfter, BigDecimal principalPaid,
                            Integer newGuarantors, User processedBy, String notes) {
        this.loan = loan;
        this.topupAmount = topupAmount;
        this.outstandingBeforeTopup = outstandingBefore;
        this.outstandingAfterTopup = outstandingAfter;
        this.principalPaidBeforeTopup = principalPaid;
        this.newGuarantorsAdded = newGuarantors;
        this.processedBy = processedBy;
        this.notes = notes;
        this.topupDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public BigDecimal getTopupAmount() {
        return topupAmount;
    }

    public void setTopupAmount(BigDecimal topupAmount) {
        this.topupAmount = topupAmount;
    }

    public BigDecimal getOutstandingBeforeTopup() {
        return outstandingBeforeTopup;
    }

    public void setOutstandingBeforeTopup(BigDecimal outstandingBeforeTopup) {
        this.outstandingBeforeTopup = outstandingBeforeTopup;
    }

    public BigDecimal getOutstandingAfterTopup() {
        return outstandingAfterTopup;
    }

    public void setOutstandingAfterTopup(BigDecimal outstandingAfterTopup) {
        this.outstandingAfterTopup = outstandingAfterTopup;
    }

    public BigDecimal getPrincipalPaidBeforeTopup() {
        return principalPaidBeforeTopup;
    }

    public void setPrincipalPaidBeforeTopup(BigDecimal principalPaidBeforeTopup) {
        this.principalPaidBeforeTopup = principalPaidBeforeTopup;
    }

    public Integer getNewGuarantorsAdded() {
        return newGuarantorsAdded;
    }

    public void setNewGuarantorsAdded(Integer newGuarantorsAdded) {
        this.newGuarantorsAdded = newGuarantorsAdded;
    }

    public LocalDateTime getTopupDate() {
        return topupDate;
    }

    public void setTopupDate(LocalDateTime topupDate) {
        this.topupDate = topupDate;
    }

    public User getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(User processedBy) {
        this.processedBy = processedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
