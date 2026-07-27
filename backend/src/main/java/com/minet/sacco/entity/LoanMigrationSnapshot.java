package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Snapshot of loan fields BEFORE a migration update is applied.
 * Used for mode-aware rollback: when an UPDATE mode migration is rolled back,
 * the loan fields are restored to their pre-update values instead of deleting the loan.
 *
 * Only created for UPDATE mode migrations. CREATE mode migrations don't need snapshots
 * because the entire loan is deleted on rollback.
 */
@Entity
@Table(name = "loan_migration_snapshots")
public class LoanMigrationSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    // Pre-update field values
    @Column(name = "original_outstanding_balance", precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "original_term_months")
    private Integer termMonths;

    @Column(name = "original_interest_collected", precision = 15, scale = 2)
    private BigDecimal interestCollected;

    @Column(name = "original_disbursement_date")
    private LocalDateTime disbursementDate;

    // Audit fields
    @Column(name = "snapshot_created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "snapshot_reason", length = 500)
    private String reason; // e.g., "UPDATE mode migration snapshot before applying changes"

    // Constructors
    public LoanMigrationSnapshot() {}

    public LoanMigrationSnapshot(Loan loan, String reason) {
        this.loan = loan;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
        
        // Capture current loan state
        this.outstandingBalance = loan.getOutstandingBalance();
        this.termMonths = loan.getTermMonths();
        this.interestCollected = loan.getInterestCollected();
        this.disbursementDate = loan.getDisbursementDate();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }

    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public BigDecimal getInterestCollected() { return interestCollected; }
    public void setInterestCollected(BigDecimal interestCollected) { this.interestCollected = interestCollected; }

    public LocalDateTime getDisbursementDate() { return disbursementDate; }
    public void setDisbursementDate(LocalDateTime disbursementDate) { this.disbursementDate = disbursementDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
