# Loan Workflow Standardization & Fix - Design

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                    LOAN APPLICATION FLOW                         │
└─────────────────────────────────────────────────────────────────┘

INDIVIDUAL LOAN (Loan Officer)          BULK LOAN (Treasurer)
├─ Select Member                         ├─ Upload Excel
├─ Select Product                        ├─ Parse & Validate
├─ Enter Amount & Term                   ├─ Calculate Financials
├─ Add Guarantors (NEW)                  ├─ Add Guarantors (existing)
├─ Calculate Financials                  ├─ Display Calculations (NEW)
├─ Validate Eligibility (NEW)            ├─ Validate Eligibility (NEW)
├─ Submit → PENDING (no loan#)           ├─ Submit → PENDING (no loan#)
│
├─ Credit Committee Review
│  ├─ View Member & Guarantor Details
│  ├─ View Eligibility Results (NEW)
│  ├─ Approve/Reject → APPROVED (no loan#)
│
├─ Treasurer Disburse
│  ├─ Assign Loan Numbers (NEW)
│  ├─ Create Transactions
│  ├─ Update Status → DISBURSED
```

## Backend Changes

### 1. Loan Number Generation Service

**New Service**: `LoanNumberGenerationService`

```java
public class LoanNumberGenerationService {
    // Generate loan number on disbursement
    public String generateLoanNumber(Loan loan) {
        // Format: LN-YYYY-NNNNN
        // Use year-specific counter to avoid duplicates
        int year = LocalDateTime.now().getYear();
        long yearCount = loanRepository.countByYearAndDisbursed(year);
        return String.format("LN-%d-%05d", year, yearCount + 1);
    }
}
```

### 2. Loan Service Updates

**Method**: `applyForLoan()` - Add guarantor support
```java
public Loan applyForLoan(LoanApplicationRequest request, User createdBy) {
    // Existing validation...
    
    // NEW: Validate guarantors exist and are ACTIVE
    if (request.getGuarantorIds() != null && !request.getGuarantorIds().isEmpty()) {
        for (Long guarantorId : request.getGuarantorIds()) {
            Member guarantor = memberRepository.findById(guarantorId)
                .orElseThrow(() -> new RuntimeException("Guarantor not found"));
            if (guarantor.getStatus() != Member.Status.ACTIVE) {
                throw new RuntimeException("Guarantor is not ACTIVE");
            }
        }
    }
    
    // Create loan WITHOUT loan number
    Loan loan = new Loan();
    // ... set fields ...
    loan.setLoanNumber(null); // Explicitly null
    loan = loanRepository.save(loan);
    
    // Create guarantor records
    if (request.getGuarantorIds() != null) {
        for (Long guarantorId : request.getGuarantorIds()) {
            Member guarantor = memberRepository.findById(guarantorId).get();
            Guarantor g = new Guarantor();
            g.setLoan(loan);
            g.setMember(guarantor);
            g.setStatus(Guarantor.Status.PENDING);
            guarantorRepository.save(g);
        }
    }
    
    return loan;
}
```

**Method**: `approveLoan()` - Add eligibility validation
```java
public Loan approveLoan(Long loanId, boolean approved, String comments, User approvedBy) {
    Loan loan = loanRepository.findById(loanId)
        .orElseThrow(() -> new RuntimeException("Loan not found"));
    
    if (approved) {
        // NEW: Validate member eligibility
        MemberValidationResult memberResult = memberValidationService.validateMember(loan.getMember());
        if (!memberResult.isEligible()) {
            throw new RuntimeException("Member not eligible: " + memberResult.getErrors());
        }
        
        // NEW: Validate all guarantors
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loanId);
        for (Guarantor guarantor : guarantors) {
            GuarantorValidationResult result = guarantorValidationService
                .validateGuarantor(guarantor.getMember(), loan.getAmount());
            if (!result.isEligible()) {
                throw new RuntimeException("Guarantor not eligible: " + result.getErrors());
            }
            // Store validation result for credit committee review
            guarantor.setValidationResult(result);
        }
        
        // Do NOT generate loan number here
        loan.setStatus(Loan.Status.APPROVED);
        loan.setApprovalDate(LocalDateTime.now());
        loan.setApprovedBy(approvedBy.getId());
    } else {
        loan.setStatus(Loan.Status.REJECTED);
        loan.setRejectionReason(comments);
    }
    
    return loanRepository.save(loan);
}
```

**Method**: `disburseLoan()` - Assign loan number on disbursement
```java
public Loan disburseLoan(Long loanId, User disbursingBy) {
    Loan loan = loanRepository.findById(loanId)
        .orElseThrow(() -> new RuntimeException("Loan not found"));
    
    if (loan.getStatus() != Loan.Status.APPROVED) {
        throw new RuntimeException("Loan must be APPROVED before disbursement");
    }
    
    // NEW: Generate and assign loan number
    String loanNumber = loanNumberGenerationService.generateLoanNumber(loan);
    loan.setLoanNumber(loanNumber);
    
    // Create disbursement transaction
    Transaction transaction = new Transaction();
    transaction.setMember(loan.getMember());
    transaction.setType(Transaction.Type.LOAN_DISBURSEMENT);
    transaction.setAmount(loan.getAmount());
    transaction.setDescription("Loan disbursement: " + loanNumber);
    transaction.setCreatedAt(LocalDateTime.now());
    transactionRepository.save(transaction);
    
    // Update member's account
    Account account = accountRepository.findByMemberIdAndAccountType(
        loan.getMember().getId(), Account.AccountType.SAVINGS)
        .orElseThrow(() -> new RuntimeException("Member account not found"));
    account.setBalance(account.getBalance().add(loan.getAmount()));
    accountRepository.save(account);
    
    // Update loan status
    loan.setStatus(Loan.Status.DISBURSED);
    loan.setDisbursementDate(LocalDateTime.now());
    loan.setDisbursedBy(disbursingBy.getId());
    
    return loanRepository.save(loan);
}
```

### 3. Bulk Processing Service Updates

**Method**: `processLoanItem()` - Add calculations and eligibility validation
```java
private void processLoanItem(BulkLoanItem item) {
    // Existing validation...
    
    // NEW: Calculate financials
    BigDecimal principal = item.getAmount();
    BigDecimal annualRate = loanProduct.getInterestRate();
    Integer termMonths = item.getTermMonths() != null ? item.getTermMonths() : loanProduct.getMinTermMonths();
    
    BigDecimal rate = annualRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    BigDecimal timeInYears = BigDecimal.valueOf(termMonths).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
    BigDecimal totalInterest = principal.multiply(rate).multiply(timeInYears).setScale(2, RoundingMode.HALF_UP);
    BigDecimal totalRepayable = principal.add(totalInterest);
    BigDecimal monthlyRepayment = totalRepayable.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
    
    // Store calculations in BulkLoanItem
    item.setTotalInterest(totalInterest);
    item.setTotalRepayable(totalRepayable);
    item.setMonthlyRepayment(monthlyRepayment);
    
    // Create loan WITHOUT loan number
    Loan loan = new Loan();
    loan.setMember(member);
    loan.setLoanProduct(loanProduct);
    loan.setAmount(principal);
    loan.setInterestRate(annualRate);
    loan.setTermMonths(termMonths);
    loan.setMonthlyRepayment(monthlyRepayment);
    loan.setTotalInterest(totalInterest);
    loan.setTotalRepayable(totalRepayable);
    loan.setOutstandingBalance(totalRepayable);
    loan.setPurpose(item.getPurpose());
    loan.setStatus(Loan.Status.PENDING);
    loan.setLoanNumber(null); // Explicitly null
    loan = loanRepository.save(loan);
    
    // Create guarantor records
    if (item.getGuarantor1() != null) {
        createGuarantorRecord(loan, item.getGuarantor1());
    }
    if (item.getGuarantor2() != null) {
        createGuarantorRecord(loan, item.getGuarantor2());
    }
    
    item.setLoan(loan);
}
```

**Method**: `approveBatch()` - Add eligibility validation
```java
public void approveBatch(Long batchId, User approvedBy) {
    BulkBatch batch = bulkBatchRepository.findById(batchId)
        .orElseThrow(() -> new RuntimeException("Batch not found"));
    
    List<BulkLoanItem> items = bulkLoanItemRepository.findByBatchId(batchId);
    
    for (BulkLoanItem item : items) {
        try {
            // NEW: Validate member eligibility
            MemberValidationResult memberResult = memberValidationService
                .validateMember(item.getMember());
            if (!memberResult.isEligible()) {
                item.setStatus(BulkLoanItem.Status.FAILED);
                item.setErrorMessage("Member not eligible: " + memberResult.getErrors());
                continue;
            }
            
            // NEW: Validate guarantor eligibility
            if (item.getGuarantor1() != null) {
                GuarantorValidationResult result = guarantorValidationService
                    .validateGuarantor(item.getGuarantor1(), item.getAmount());
                item.setGuarantor1EligibilityStatus(result.isEligible() ? "ELIGIBLE" : "INELIGIBLE");
                if (!result.isEligible()) {
                    item.setStatus(BulkLoanItem.Status.FAILED);
                    item.setErrorMessage("Guarantor 1 not eligible: " + result.getErrors());
                    continue;
                }
            }
            
            if (item.getGuarantor2() != null) {
                GuarantorValidationResult result = guarantorValidationService
                    .validateGuarantor(item.getGuarantor2(), item.getAmount());
                item.setGuarantor2EligibilityStatus(result.isEligible() ? "ELIGIBLE" : "INELIGIBLE");
                if (!result.isEligible()) {
                    item.setStatus(BulkLoanItem.Status.FAILED);
                    item.setErrorMessage("Guarantor 2 not eligible: " + result.getErrors());
                    continue;
                }
            }
            
            // Approve the loan
            Loan loan = item.getLoan();
            loan.setStatus(Loan.Status.APPROVED);
            loan.setApprovalDate(LocalDateTime.now());
            loan.setApprovedBy(approvedBy.getId());
            loanRepository.save(loan);
            
            item.setStatus(BulkLoanItem.Status.PROCESSED);
            item.setProcessedAt(LocalDateTime.now());
        } catch (Exception e) {
            item.setStatus(BulkLoanItem.Status.FAILED);
            item.setErrorMessage(e.getMessage());
        }
    }
    
    batch.setStatus(BulkBatch.Status.APPROVED);
    batch.setApprovedBy(approvedBy.getId());
    batch.setApprovedAt(LocalDateTime.now());
    bulkBatchRepository.save(batch);
}
```

### 4. Bulk Disbursement Service

**New Method**: `disburseBatch()` - Assign loan numbers to all loans in batch
```java
public void disburseBatch(Long batchId, User disbursingBy) {
    BulkBatch batch = bulkBatchRepository.findById(batchId)
        .orElseThrow(() -> new RuntimeException("Batch not found"));
    
    List<BulkLoanItem> items = bulkLoanItemRepository.findByBatchId(batchId);
    
    for (BulkLoanItem item : items) {
        if (item.getStatus() == BulkLoanItem.Status.PROCESSED) {
            Loan loan = item.getLoan();
            
            // Generate and assign loan number
            String loanNumber = loanNumberGenerationService.generateLoanNumber(loan);
            loan.setLoanNumber(loanNumber);
            
            // Create disbursement transaction
            Transaction transaction = new Transaction();
            transaction.setMember(loan.getMember());
            transaction.setType(Transaction.Type.LOAN_DISBURSEMENT);
            transaction.setAmount(loan.getAmount());
            transaction.setDescription("Loan disbursement: " + loanNumber);
            transaction.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            
            // Update member's account
            Account account = accountRepository.findByMemberIdAndAccountType(
                loan.getMember().getId(), Account.AccountType.SAVINGS)
                .orElseThrow(() -> new RuntimeException("Member account not found"));
            account.setBalance(account.getBalance().add(loan.getAmount()));
            accountRepository.save(account);
            
            // Update loan status
            loan.setStatus(Loan.Status.DISBURSED);
            loan.setDisbursementDate(LocalDateTime.now());
            loan.setDisbursedBy(disbursingBy.getId());
            loanRepository.save(loan);
            
            item.setStatus(BulkLoanItem.Status.DISBURSED);
        }
    }
    
    batch.setStatus(BulkBatch.Status.DISBURSED);
    batch.setDisbursedBy(disbursingBy.getId());
    batch.setDisbursedAt(LocalDateTime.now());
    bulkBatchRepository.save(batch);
}
```

## Frontend Changes

### 1. Individual Loan Application Form (Loans.tsx)

**Add Guarantor Selection**:
- Multi-select dropdown for guarantors
- Show guarantor details (member number, name, balance)
- Add/remove guarantor buttons
- Display guarantor eligibility status

**Add Eligibility Validation**:
- On form submission, validate member and guarantor eligibility
- Show validation results before submission
- Display errors/warnings to user

### 2. Bulk Loan Review UI (BulkProcessing.tsx)

**Add Calculations Display**:
- Show totalInterest, totalRepayable, monthlyRepayment for each loan
- Display in batch review table

**Add Eligibility Results**:
- Show member eligibility status
- Show guarantor1 and guarantor2 eligibility status
- Display validation errors/warnings

### 3. Loan Details View

**Remove Loan Number from PENDING/APPROVED**:
- Only show loan number if status is DISBURSED or later
- Show "Pending" or "—" for earlier statuses

**Add Guarantor Information**:
- Display list of guarantors with their details
- Show guarantor eligibility validation results

## Database Changes

### Loan Table
```sql
-- Make loanNumber nullable (if not already)
ALTER TABLE loans MODIFY COLUMN loan_number VARCHAR(50) NULL;

-- Add index for year-specific counter
CREATE INDEX idx_loans_disbursement_date ON loans(YEAR(disbursement_date));
```

### BulkLoanItem Table
```sql
-- Add calculation fields
ALTER TABLE bulk_loan_items ADD COLUMN total_interest DECIMAL(19,2);
ALTER TABLE bulk_loan_items ADD COLUMN total_repayable DECIMAL(19,2);
ALTER TABLE bulk_loan_items ADD COLUMN monthly_repayment DECIMAL(19,2);

-- Add eligibility status fields
ALTER TABLE bulk_loan_items ADD COLUMN guarantor1_eligibility_status VARCHAR(20);
ALTER TABLE bulk_loan_items ADD COLUMN guarantor2_eligibility_status VARCHAR(20);
```

### Guarantor Table
```sql
-- Ensure pledge_amount is tracked
-- Already exists, just ensure it's used
```

## API Endpoints

### New/Modified Endpoints

1. **POST /api/loans/apply** (Modified)
   - Add `guarantorIds` to request body
   - Add eligibility validation
   - Return loan without loan number

2. **POST /api/loans/approve** (Modified)
   - Add member and guarantor eligibility validation
   - Return loan without loan number

3. **POST /api/loans/disburse/{loanId}** (Modified)
   - Generate and assign loan number
   - Create disbursement transaction
   - Return loan with loan number

4. **POST /api/bulk-processing/disburse/{batchId}** (New)
   - Disburse entire batch
   - Assign loan numbers to all loans
   - Create transactions for all

5. **GET /api/loans/{loanId}/eligibility** (New)
   - Get member and guarantor eligibility results
   - Used by credit committee review

## Testing Strategy

1. **Unit Tests**
   - Loan number generation (year-specific counter)
   - Eligibility validation (member and guarantor)
   - Calculation accuracy

2. **Integration Tests**
   - Individual loan application with guarantors
   - Bulk loan application with calculations
   - Approval with eligibility validation
   - Disbursement with loan number assignment

3. **End-to-End Tests**
   - Complete individual loan workflow
   - Complete bulk loan workflow
   - Verify loan numbers only appear after disbursement
