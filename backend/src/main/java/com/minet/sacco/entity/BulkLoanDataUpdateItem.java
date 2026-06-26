package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bulk Loan Data Update Item - Phase A: Low-risk field editing (Loan Data Update template)
 * Represents a single row in a bulk loan data update file.
 * 
 * Template columns:
 * - Employee ID (required, used to find member)
 * - Loan Number (required, used to find loan)
 * - Loan Status (optional)
 * - Disbursement Date (optional)
 * - Interest Rate (optional)
 * - Outstanding Balance (optional)
 * - Purpose (optional)
 */
@Entity
@Table(name = "bulk_loan_data_update_items")
public class BulkLoanDataUpdateItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private BulkBatch batch;
    
    @Column(name = "row_number")
    private Integer rowNumber;
    
    // Input fields
    @Column(name = "employee_id", length = 50)
    private String employeeId;
    
    @Column(name = "loan_number", length = 50)
    private String loanNumber;
    
    @Column(name = "loan_status", length = 50)
    private String loanStatus;
    
    @Column(name = "disbursement_date")
    private java.time.LocalDate disbursementDate;
    
    @Column(name = "interest_rate", precision = 10, scale = 2)
    private BigDecimal interestRate;
    
    @Column(name = "outstanding_balance", precision = 15, scale = 2)
    private BigDecimal outstandingBalance;
    
    @Column(name = "purpose", length = 500)
    private String purpose;
    
    // Processing fields
    @Column(name = "status", length = 50)
    private String status; // PENDING, PROCESSED, FAILED
    
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }
    
    // Constructors
    public BulkLoanDataUpdateItem() {}
    
    public BulkLoanDataUpdateItem(BulkBatch batch, Integer rowNumber, String employeeId, String loanNumber) {
        this.batch = batch;
        this.rowNumber = rowNumber;
        this.employeeId = employeeId;
        this.loanNumber = loanNumber;
        this.status = "PENDING";
    }
    
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
    
    public String getLoanStatus() { return loanStatus; }
    public void setLoanStatus(String loanStatus) { this.loanStatus = loanStatus; }
    
    public java.time.LocalDate getDisbursementDate() { return disbursementDate; }
    public void setDisbursementDate(java.time.LocalDate disbursementDate) { this.disbursementDate = disbursementDate; }
    
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }
    
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    /**
     * Check if row has any data to update (at least one Phase A field filled)
     */
    public boolean hasDataToUpdate() {
        return loanStatus != null || 
               disbursementDate != null ||
               interestRate != null ||
               outstandingBalance != null ||
               (purpose != null && !purpose.trim().isEmpty());
    }
}
