package com.minet.sacco.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single loan record in a loan migration batch.
 * Used to import historical loan data (DISBURSED, REPAID, DEFAULTED) from the old system.
 * Supports up to 6 guarantors per loan.
 */
@Entity
@Table(name = "loan_migration_items")
public class LoanMigrationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnoreProperties({"items", "memberItems", "loanItems", "disbursementItems", "uploadedBy", "approvedBy"})
    private BulkBatch batch;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    // Borrower
    @Column(name = "employee_id", nullable = true)
    private String employeeId;

    // Loan details
    @Column(name = "loan_number", length = 50)
    private String loanNumber;

    @Column(name = "loan_product_name", nullable = true)
    private String loanProductName;

    @Column(name = "principal_amount", nullable = true, precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "term_months", nullable = true)
    private Integer termMonths;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate; // Optional - uses product default if null

    @Column(name = "disbursement_date", nullable = true)
    private LocalDate disbursementDate;

    @Column(name = "loan_status", nullable = true, length = 20)
    private String loanStatus; // DISBURSED, REPAID, DEFAULTED

    @Column(name = "outstanding_balance", nullable = true, precision = 15, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(name = "interest_collected", nullable = true, precision = 15, scale = 2)
    private BigDecimal interestCollected;

    @Column(name = "guarantorship_type", nullable = true, length = 10)
    private String guarantorshipType; // NORMAL or SELF (optional - can be set via UPDATE later)

    @Column(name = "purpose", length = 500)
    private String purpose;

    // Guarantors (up to 6) - stored as Employee IDs
    @Column(name = "guarantor1_employee_id")
    private String guarantor1EmployeeId;
    @Column(name = "guarantor1_pledge_amount", precision = 15, scale = 2)
    private BigDecimal guarantor1PledgeAmount;

    @Column(name = "guarantor2_employee_id")
    private String guarantor2EmployeeId;
    @Column(name = "guarantor2_pledge_amount", precision = 15, scale = 2)
    private BigDecimal guarantor2PledgeAmount;

    @Column(name = "guarantor3_employee_id")
    private String guarantor3EmployeeId;
    @Column(name = "guarantor3_pledge_amount", precision = 15, scale = 2)
    private BigDecimal guarantor3PledgeAmount;

    @Column(name = "guarantor4_employee_id")
    private String guarantor4EmployeeId;
    @Column(name = "guarantor4_pledge_amount", precision = 15, scale = 2)
    private BigDecimal guarantor4PledgeAmount;

    @Column(name = "guarantor5_employee_id")
    private String guarantor5EmployeeId;
    @Column(name = "guarantor5_pledge_amount", precision = 15, scale = 2)
    private BigDecimal guarantor5PledgeAmount;

    @Column(name = "guarantor6_employee_id")
    private String guarantor6EmployeeId;
    @Column(name = "guarantor6_pledge_amount", precision = 15, scale = 2)
    private BigDecimal guarantor6PledgeAmount;

    // Calculated fields (set during processing)
    @Column(name = "total_interest", precision = 15, scale = 2)
    private BigDecimal totalInterest;

    @Column(name = "total_repayable", precision = 15, scale = 2)
    private BigDecimal totalRepayable;

    @Column(name = "monthly_repayment", precision = 15, scale = 2)
    private BigDecimal monthlyRepayment;

    // Result
    @ManyToOne
    @JoinColumn(name = "loan_id")
    @JsonIgnoreProperties({"member", "loanProduct", "repayments", "guarantors", "createdBy", "approvedBy", "disbursedBy"})
    private Loan loan;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // Migration mode: CREATE (new loan) or UPDATE (existing loan)
    @Column(name = "migration_mode", length = 10)
    private String migrationMode; // CREATE or UPDATE

    // Reference to snapshot of pre-update values (for UPDATE mode only)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "snapshot_id")
    private LoanMigrationSnapshot snapshot;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BulkBatch getBatch() { return batch; }
    public void setBatch(BulkBatch batch) { this.batch = batch; }
    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public String getLoanNumber() { return loanNumber; }
    public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }
    public String getLoanProductName() { return loanProductName; }
    public void setLoanProductName(String loanProductName) { this.loanProductName = loanProductName; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public LocalDate getDisbursementDate() { return disbursementDate; }
    public void setDisbursementDate(LocalDate disbursementDate) { this.disbursementDate = disbursementDate; }
    public String getLoanStatus() { return loanStatus; }
    public void setLoanStatus(String loanStatus) { this.loanStatus = loanStatus; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public BigDecimal getInterestCollected() { return interestCollected; }
    public void setInterestCollected(BigDecimal interestCollected) { this.interestCollected = interestCollected; }
    public String getGuarantorshipType() { return guarantorshipType; }
    public void setGuarantorshipType(String guarantorshipType) { this.guarantorshipType = guarantorshipType; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getGuarantor1EmployeeId() { return guarantor1EmployeeId; }
    public void setGuarantor1EmployeeId(String v) { this.guarantor1EmployeeId = v; }
    public BigDecimal getGuarantor1PledgeAmount() { return guarantor1PledgeAmount; }
    public void setGuarantor1PledgeAmount(BigDecimal v) { this.guarantor1PledgeAmount = v; }
    public String getGuarantor2EmployeeId() { return guarantor2EmployeeId; }
    public void setGuarantor2EmployeeId(String v) { this.guarantor2EmployeeId = v; }
    public BigDecimal getGuarantor2PledgeAmount() { return guarantor2PledgeAmount; }
    public void setGuarantor2PledgeAmount(BigDecimal v) { this.guarantor2PledgeAmount = v; }
    public String getGuarantor3EmployeeId() { return guarantor3EmployeeId; }
    public void setGuarantor3EmployeeId(String v) { this.guarantor3EmployeeId = v; }
    public BigDecimal getGuarantor3PledgeAmount() { return guarantor3PledgeAmount; }
    public void setGuarantor3PledgeAmount(BigDecimal v) { this.guarantor3PledgeAmount = v; }
    public String getGuarantor4EmployeeId() { return guarantor4EmployeeId; }
    public void setGuarantor4EmployeeId(String v) { this.guarantor4EmployeeId = v; }
    public BigDecimal getGuarantor4PledgeAmount() { return guarantor4PledgeAmount; }
    public void setGuarantor4PledgeAmount(BigDecimal v) { this.guarantor4PledgeAmount = v; }
    public String getGuarantor5EmployeeId() { return guarantor5EmployeeId; }
    public void setGuarantor5EmployeeId(String v) { this.guarantor5EmployeeId = v; }
    public BigDecimal getGuarantor5PledgeAmount() { return guarantor5PledgeAmount; }
    public void setGuarantor5PledgeAmount(BigDecimal v) { this.guarantor5PledgeAmount = v; }
    public String getGuarantor6EmployeeId() { return guarantor6EmployeeId; }
    public void setGuarantor6EmployeeId(String v) { this.guarantor6EmployeeId = v; }
    public BigDecimal getGuarantor6PledgeAmount() { return guarantor6PledgeAmount; }
    public void setGuarantor6PledgeAmount(BigDecimal v) { this.guarantor6PledgeAmount = v; }
    public BigDecimal getTotalInterest() { return totalInterest; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }
    public BigDecimal getTotalRepayable() { return totalRepayable; }
    public void setTotalRepayable(BigDecimal totalRepayable) { this.totalRepayable = totalRepayable; }
    public BigDecimal getMonthlyRepayment() { return monthlyRepayment; }
    public void setMonthlyRepayment(BigDecimal monthlyRepayment) { this.monthlyRepayment = monthlyRepayment; }
    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public String getMigrationMode() { return migrationMode; }
    public void setMigrationMode(String migrationMode) { this.migrationMode = migrationMode; }
    public LoanMigrationSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(LoanMigrationSnapshot snapshot) { this.snapshot = snapshot; }
}
