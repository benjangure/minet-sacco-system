# Feature Implementation Roadmap

Three critical features to implement for complete member financial tracking and proper loan constraints:

1. **Historical Monthly Contributions Import** (One-time bulk import of past savings deposits)
2. **Historical Withdrawals Import** (One-time bulk import of past withdrawal activity)
3. **Emergency Loan Tier Constraints** (Aggregate Limits enforcement)

---

## Feature 1: Historical Monthly Contributions Import

### Problem Statement
Members have been contributing to savings since they joined the SACCO, but this historical data is not in the system. We need a one-time bulk import mechanism to:
- Record all past monthly contributions per member
- Update member account balances with historical data
- Enable accurate eligibility calculations and reports going forward
- Similar to loan migration, but for savings deposits

### Current State
- Members' current savings balance exists in Account table
- But WHERE that balance came from (contribution history) is missing
- No breakdown of contributions by month/year
- Affects: eligibility calculations (3× savings), member statement completeness, report accuracy

### Example Data Needed
```
Member: EMP001
Jan 2023: 5,000 (salary deduction)
Feb 2023: 5,000 (salary deduction)
Mar 2023: 5,000 (salary deduction)
...
Dec 2024: 5,000 (salary deduction)
Total: 60,000 (should match or explain difference vs. current balance)
```

### Proposed Solution

#### Phase 1: Data Model (Backend)

**New Entity: MemberContributionHistory**
```java
@Entity
@Table(name = "member_contribution_history")
public class MemberContributionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;
    
    private LocalDate contributionMonth;  // YYYY-MM-01 (first day of month)
    private BigDecimal amount;
    private String paymentMethod;  // SALARY_DEDUCTION, MANUAL, BANK_TRANSFER, etc.
    private String remarks;
    private LocalDateTime importedAt;
    private String importedFrom;  // "HISTORICAL_IMPORT" or similar
}
```

**Repository**
```java
public interface MemberContributionHistoryRepository extends JpaRepository<MemberContributionHistory, Long> {
    List<MemberContributionHistory> findByMemberIdOrderByContributionMonth(Long memberId);
    BigDecimal getTotalContributionsByMember(Long memberId);
    List<MemberContributionHistory> findByContributionMonthBetween(LocalDate start, LocalDate end);
}
```

#### Phase 2: UI Component (Frontend)

**New Page in Bulk Processing: `Historical Contributions` Tab**

Similar to Loan Migration page:
- Download template (CSV/Excel)
- Upload historical contributions file
- View import results
- Display errors per row

#### Phase 3: Service Logic (Backend)

**New Service: HistoricalContributionService** (similar to LoanMigrationService)

```java
@Service
public class HistoricalContributionService {
    
    @Transactional
    public BulkBatch importHistoricalContributions(MultipartFile file, User uploader) {
        // Parse Excel/CSV
        // Validate each row
        // Create contribution history records
        // Update member account balances
        // Return batch summary
    }
    
    private List<String> validateItem(ContributionImportItem item) {
        // Validate member exists
        // Validate month format (MM/YYYY)
        // Validate amount > 0
        // Check no duplicate for same member+month
    }
    
    @Transactional
    private void processItem(ContributionImportItem item) {
        // Create MemberContributionHistory record
        // Add amount to member's savings account
        // Create CONTRIBUTION transaction for audit trail
    }
}
```

#### Phase 4: Template Structure

**Excel Template: `Historical_Contributions_Template.csv`**
```
Employee ID | Contribution Month (MM/YYYY) | Amount | Payment Method | Remarks
EMP001 | 01/2023 | 5000 | SALARY_DEDUCTION | Initial contribution
EMP001 | 02/2023 | 5000 | SALARY_DEDUCTION | Monthly contribution
EMP002 | 01/2023 | 8000 | MANUAL | Manual deposit
```

**Validation Rules:**
- Employee ID must exist in system
- Contribution Month must be valid MM/YYYY format
- Amount must be > 0
- Payment Method: SALARY_DEDUCTION, MANUAL, BANK_TRANSFER, CHEQUE, etc.
- No duplicate for same member + month
- Optional Remarks column

#### Phase 5: Database Migration

```sql
CREATE TABLE member_contribution_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    contribution_month DATE NOT NULL,  -- First day of month
    amount DECIMAL(15,2) NOT NULL,
    payment_method VARCHAR(50),
    remarks TEXT,
    imported_at TIMESTAMP,
    imported_from VARCHAR(50),
    FOREIGN KEY (member_id) REFERENCES members(id),
    UNIQUE KEY unique_member_month (member_id, contribution_month)
);

-- Create batch tracking (reuse BulkBatch with batchType = 'HISTORICAL_CONTRIBUTIONS')
-- Contributions are processed immediately, no approval workflow needed
```

#### Integration Points

**Where Historical Contributions Feed Into:**
1. **Member Savings Balance** — Contributions are added to Account.savings_balance
2. **Eligibility Calculation** — Uses updated savings for 3× rule
3. **Member Statement** — Shows contribution history per member
4. **Reports** — Cashbook, Trial Balance reflect historical data
5. **Audit Trail** — All contributions tracked via transaction records

---

## Feature 2: Historical Withdrawals Import

### Problem Statement
Members have made withdrawals from their savings over time, but this historical data is not in the system. We need a one-time bulk import mechanism to:
- Record all past withdrawal activity per member
- Reduce member account balances by historical withdrawals
- Enable complete member financial history and accurate reports
- Similar to loan migration, but for withdrawal activity

### Current State
- Members' current savings balance exists but history is missing
- No record of WHY savings decreased over time
- Affects: member statement completeness, audit trail, understanding member behavior

### Example Data Needed
```
Member: EMP001
Jan 2023: 2,000 (withdrawal)
Mar 2023: 1,500 (withdrawal)
Jun 2023: 3,000 (withdrawal)
...
Total Withdrawals: 20,000
```

### Proposed Solution

#### Phase 1: Data Model (Backend)

**New Entity: MemberWithdrawalHistory**
```java
@Entity
@Table(name = "member_withdrawal_history")
public class MemberWithdrawalHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;
    
    private LocalDate withdrawalDate;
    private BigDecimal amount;
    private String purpose;  // PERSONAL, EMERGENCY, etc.
    private String remarks;
    private LocalDateTime importedAt;
    private String importedFrom;  // "HISTORICAL_IMPORT"
}
```

**Repository**
```java
public interface MemberWithdrawalHistoryRepository extends JpaRepository<MemberWithdrawalHistory, Long> {
    List<MemberWithdrawalHistory> findByMemberIdOrderByWithdrawalDate(Long memberId);
    BigDecimal getTotalWithdrawalsByMember(Long memberId);
}
```

#### Phase 2: UI Component (Frontend)

**New Page in Bulk Processing: `Historical Withdrawals` Tab**

Similar to Loan Migration page:
- Download template (CSV/Excel)
- Upload historical withdrawals file
- View import results
- Display errors per row

#### Phase 3: Service Logic (Backend)

**New Service: HistoricalWithdrawalService**

```java
@Service
public class HistoricalWithdrawalService {
    
    @Transactional
    public BulkBatch importHistoricalWithdrawals(MultipartFile file, User uploader) {
        // Parse Excel/CSV
        // Validate each row
        // Create withdrawal history records
        // Update member account balances (SUBTRACT)
        // Return batch summary
    }
    
    private List<String> validateItem(WithdrawalImportItem item) {
        // Validate member exists
        // Validate withdrawal date format (DD/MM/YYYY)
        // Validate amount > 0
        // Optional: check member had sufficient balance at that time
    }
    
    @Transactional
    private void processItem(WithdrawalImportItem item) {
        // Create MemberWithdrawalHistory record
        // SUBTRACT amount from member's savings account
        // Create WITHDRAWAL transaction for audit trail
    }
}
```

#### Phase 4: Template Structure

**Excel Template: `Historical_Withdrawals_Template.csv`**
```
Employee ID | Withdrawal Date (DD/MM/YYYY) | Amount | Purpose | Remarks
EMP001 | 15/01/2023 | 2000 | PERSONAL | Family expenses
EMP001 | 20/03/2023 | 1500 | EMERGENCY | Medical emergency
EMP002 | 10/02/2023 | 5000 | PERSONAL | Personal loan repayment
```

**Validation Rules:**
- Employee ID must exist in system
- Withdrawal Date must be valid DD/MM/YYYY format
- Amount must be > 0
- Purpose: PERSONAL, EMERGENCY, MEDICAL, etc. (optional)
- No future dates allowed
- Optional Remarks column

#### Phase 5: Database Migration

```sql
CREATE TABLE member_withdrawal_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    withdrawal_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    purpose VARCHAR(100),
    remarks TEXT,
    imported_at TIMESTAMP,
    imported_from VARCHAR(50),
    FOREIGN KEY (member_id) REFERENCES members(id)
);

-- Index for performance
CREATE INDEX idx_member_withdrawal_date ON member_withdrawal_history(member_id, withdrawal_date);
```

#### Integration Points

**Where Historical Withdrawals Feed Into:**
1. **Member Savings Balance** — Withdrawals are subtracted from Account.savings_balance
2. **Member Statement** — Shows withdrawal history per member
3. **Reports** — Cashbook, Trial Balance reflect historical data
4. **Audit Trail** — All withdrawals tracked via transaction records

---

## Feature 3: Emergency Loan Tier Constraints

### Problem Statement
Current State:
- **Normal Loan**: No limit, only checks `3 × savings`
- **Emergency Loan 1**: Should have max 150k **cumulative** across all Emergency Loan 1 borrowings
- **Emergency Loan 2**: Should have max 150k **cumulative** across all Emergency Loan 2 borrowings

Current Bug:
- System allows borrowing 150k twice for Emergency Loan 1 (should only allow 150k total across all Emergency Loan 1 loans)
- The constraint is **not enforced** during loan eligibility calculation

### Example Scenario (What should happen)
```
Member borrows Emergency Loan 1 for 100k (OK, within 150k limit)
Member applies again for Emergency Loan 1 for 60k (REJECTED - would exceed 150k total)
Member can borrow Emergency Loan 2 instead (separate 150k limit)
```

### Root Cause
The eligibility calculation in `EligibilityCalculationService` only checks:
1. Savings balance (3× rule)
2. Individual loan amount against product limits
3. Does NOT check cumulative borrowing across same product

### Proposed Solution

#### Phase 1: Data Model (Backend)

**Extend LoanProduct Entity**
```java
@Entity
@Table(name = "loan_products")
public class LoanProduct {
    // ... existing fields ...
    
    // NEW FIELDS:
    private BigDecimal maxTotalBorrowingLimit;  // e.g., 150000 for emergency loans
    // This is CUMULATIVE limit across all active loans of this product
    
    private Boolean enforceAggregateLimit;  // true for Emergency Loan 1 & 2, false for Normal
}
```

**Update Database**
```sql
ALTER TABLE loan_products ADD COLUMN max_total_borrowing_limit DECIMAL(15,2);
ALTER TABLE loan_products ADD COLUMN enforce_aggregate_limit BOOLEAN DEFAULT FALSE;

UPDATE loan_products SET 
    max_total_borrowing_limit = 150000,
    enforce_aggregate_limit = TRUE
WHERE name IN ('Emergency Loan 1', 'Emergency Loan 2');

UPDATE loan_products SET 
    max_total_borrowing_limit = NULL,
    enforce_aggregate_limit = FALSE
WHERE name = 'Normal Loan';
```

#### Phase 2: Eligibility Logic (Backend)

**Update EligibilityCalculationService**

```java
@Service
public class EligibilityCalculationService {
    
    public EligibilityCheckResult checkLoanEligibility(Member member, LoanProduct product, BigDecimal requestedAmount) {
        
        // 1. Basic checks (existing)
        if (!checkSavingsRule(member, requestedAmount)) {
            return fail("Does not meet 3× savings requirement");
        }
        
        if (requestedAmount.compareTo(product.getMaxAmount()) > 0) {
            return fail("Requested amount exceeds product maximum");
        }
        
        // 2. NEW: Check aggregate limit if enforced
        if (product.getEnforceAggregateLimit() && product.getMaxTotalBorrowingLimit() != null) {
            BigDecimal currentBorrowingOnProduct = calculateCurrentBorrowingOnProduct(member, product);
            BigDecimal totalAfterLoan = currentBorrowingOnProduct.add(requestedAmount);
            BigDecimal limit = product.getMaxTotalBorrowingLimit();
            
            if (totalAfterLoan.compareTo(limit) > 0) {
                BigDecimal availableToorrow = limit.subtract(currentBorrowingOnProduct);
                return fail(
                    "Aggregate limit exceeded. You have " + currentBorrowingOnProduct + 
                    " borrowed on " + product.getName() + 
                    ". Maximum allowed is " + limit + 
                    ". You can borrow up to " + availableToBorrow + " more."
                );
            }
        }
        
        // 3. Other checks (guarantors, etc.)
        // ... existing logic ...
        
        return success();
    }
    
    // NEW METHOD: Calculate current outstanding on a specific product
    private BigDecimal calculateCurrentBorrowingOnProduct(Member member, LoanProduct product) {
        return loanRepository.findByMemberAndLoanProductAndStatusIn(
            member, 
            product, 
            List.of(Loan.Status.DISBURSED, Loan.Status.PENDING_APPROVAL)
        ).stream()
         .map(Loan::getOutstandingBalance)
         .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

#### Phase 3: UI Feedback (Frontend)

**Update Loan Application Component**

When member selects Emergency Loan 1:
```
Current Borrowing on Emergency Loan 1: KES 100,000
Maximum Allowed: KES 150,000
Available to Borrow: KES 50,000

Please enter amount (max KES 50,000):
[_____________]
```

If they try to borrow more:
```
❌ ERROR: You have KES 100,000 already borrowed on Emergency Loan 1.
   Maximum allowed is KES 150,000. 
   You can only borrow KES 50,000 more on this product.
   
Consider: Emergency Loan 2 (separate KES 150,000 limit)
```

#### Phase 4: Admin Configuration

**Add to Loan Product Configuration UI**

New fields in `LoanProductConfiguration.tsx`:
```
Enforce Aggregate Limit: [Toggle ON/OFF]
Maximum Total Borrowing: [Input field for amount]

Examples:
- Emergency Loan 1: ON, 150000
- Emergency Loan 2: ON, 150000
- Normal Loan: OFF, (empty)
```

#### Phase 5: Migration/Testing

**Query to validate current state (BEFORE fix)**
```sql
-- Find members who violated the constraint
SELECT m.member_number, m.first_name, p.name as product_name,
       COUNT(l.id) as loan_count,
       SUM(l.outstanding_balance) as total_outstanding,
       p.max_total_borrowing_limit as max_limit,
       SUM(l.outstanding_balance) - p.max_total_borrowing_limit as overage
FROM members m
JOIN loans l ON m.id = l.member_id
JOIN loan_products p ON l.loan_product_id = p.id
WHERE p.enforce_aggregate_limit = TRUE
  AND l.status IN ('DISBURSED', 'PENDING_APPROVAL')
GROUP BY m.id, p.id
HAVING SUM(l.outstanding_balance) > p.max_total_borrowing_limit;
```

---

## Integration with Reports

### How Contributions & Withdrawals Improve Reports

**1. Member Statement Report**
- ✅ Now includes: contribution history + withdrawal requests
- ✅ Shows complete financial picture

**2. Cashbook Report**
- ✅ Separates contributions from other deposits
- ✅ Shows withdrawal approvals + actual payments

**3. Trial Balance Report**
- ✅ More accurate member account balances (contributions tracked separately)

**4. New Reports Enabled**
- **Contribution Reconciliation** — Which members haven't contributed this month?
- **Withdrawal Approval Report** — Pending vs. approved withdrawals
- **Member Financial Summary** — Total contributions, total withdrawals, net position

---

## Implementation Priority & Timeline

### Phase 1 (Week 1-2): Emergency Loan Constraint
**Why First?** Fixes a critical bug, prevents over-borrowing, quick to implement
- Backend: Update LoanProduct, modify eligibility logic
- Frontend: Update loan application to show available amount
- Testing: Verify aggregate limits enforced

### Phase 2 (Week 3-4): Historical Contributions Import
**Why Second?** One-time bulk import (like loan migration), essential for accurate member data
- Backend: Create HistoricalContributionService, entities, validation
- Frontend: Add Historical Contributions tab to Bulk Processing
- Integration: Updates member savings balances for eligibility calculations

### Phase 3 (Week 5-6): Historical Withdrawals Import
**Why Third?** One-time bulk import, completes historical financial picture
- Backend: Create HistoricalWithdrawalService, entities, validation
- Frontend: Add Historical Withdrawals tab to Bulk Processing
- Integration: Reduces member savings balances to reflect historical withdrawals

---

## Database Schema Summary

```sql
-- Existing (modified)
ALTER TABLE loan_products ADD COLUMN max_total_borrowing_limit DECIMAL(15,2);
ALTER TABLE loan_products ADD COLUMN enforce_aggregate_limit BOOLEAN DEFAULT FALSE;

-- New tables
CREATE TABLE monthly_contributions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    contribution_month DATE NOT NULL,
    payment_method VARCHAR(50),
    status VARCHAR(20),
    recorded_at TIMESTAMP,
    recorded_by VARCHAR(100),
    remarks TEXT,
    FOREIGN KEY (member_id) REFERENCES members(id),
    UNIQUE KEY unique_member_month (member_id, contribution_month)
);

CREATE TABLE withdrawal_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    request_date TIMESTAMP,
    status VARCHAR(20),
    purpose VARCHAR(100),
    bank_details VARCHAR(100),
    approved_by_id BIGINT,
    approval_date TIMESTAMP,
    approval_reason TEXT,
    transaction_id BIGINT,
    processed_at TIMESTAMP,
    remarks TEXT,
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (approved_by_id) REFERENCES users(id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(id)
);
```

---

## Success Criteria

✅ **Emergency Loan Constraint**
- [ ] Aggregate limit enforced during loan application
- [ ] Members see available borrow amount
- [ ] System prevents over-borrowing (e.g., can't borrow 100k + 100k on 150k limit)
- [ ] Helpful error message shows remaining available balance

✅ **Historical Contributions Import**
- [ ] Can bulk import past monthly contributions via Excel
- [ ] Member savings balances correctly updated
- [ ] Contributions appear in member statements
- [ ] Used in 3× savings eligibility calculation
- [ ] Audit trail maintained for all imports

✅ **Historical Withdrawals Import**
- [ ] Can bulk import past withdrawals via Excel
- [ ] Member savings balances correctly reduced
- [ ] Withdrawals appear in member statements
- [ ] Complete financial history available per member
- [ ] Audit trail maintained for all imports

✅ **Reports Integration**
- [ ] Member Statement shows complete history (contributions + withdrawals)
- [ ] Cashbook shows contribution and withdrawal transactions
- [ ] Trial Balance reflects correct member account balances
- [ ] Eligibility calculations use updated savings figures
- [ ] All data reconciles with historical records

---

## How This Enables Better Reporting

### Member Statement Report - NOW COMPLETE
```
Member: EMP001 (John Doe)
Period: Jan 2023 - Dec 2024

HISTORICAL ACTIVITY:
Contributions:
  Jan 2023: 5,000
  Feb 2023: 5,000
  ... (continues for all months)
  Dec 2024: 5,000
  Subtotal: 60,000

Withdrawals:
  Mar 2023: 2,000
  Jun 2023: 1,500
  Subtotal: (3,500)

LOANS:
Disbursements:
  Loan LN-2024-001: 50,000
Repayments:
  Loan LN-2024-001: 15,000

CURRENT BALANCE: 101,500 ✓ (60,000 - 3,500 + 50,000 - 15,000)
```

### Cashbook Report - NOW SHOWS COMPLETE PICTURE
```
Daily Transactions:
- 01/01/2023: Contribution (EMP001): 5,000
- 15/03/2023: Withdrawal (EMP001): 2,000
- 01/02/2024: Loan Disbursement (EMP001): 50,000
- 15/02/2024: Loan Repayment (EMP001): 2,500
...
```

### Eligibility Calculation - NOW ACCURATE
```
Member: EMP001
Current Savings Balance: 101,500 (includes historical contributions & withdrawals)
3× Savings Limit: 304,500 ✓
Can borrow up to: 304,500 (subject to product and Emergency Loan tier limits)
```

---

## Database Schema Summary

```sql
-- Modified table
ALTER TABLE loan_products ADD COLUMN max_total_borrowing_limit DECIMAL(15,2);
ALTER TABLE loan_products ADD COLUMN enforce_aggregate_limit BOOLEAN DEFAULT FALSE;

-- New tables
CREATE TABLE member_contribution_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    contribution_month DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    payment_method VARCHAR(50),
    remarks TEXT,
    imported_at TIMESTAMP,
    imported_from VARCHAR(50),
    FOREIGN KEY (member_id) REFERENCES members(id),
    UNIQUE KEY unique_member_month (member_id, contribution_month)
);

CREATE TABLE member_withdrawal_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    withdrawal_date DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    purpose VARCHAR(100),
    remarks TEXT,
    imported_at TIMESTAMP,
    imported_from VARCHAR(50),
    FOREIGN KEY (member_id) REFERENCES members(id),
    INDEX idx_member_withdrawal_date (member_id, withdrawal_date)
);
```

---

## Summary

These three features work together to complete the member financial picture:

1. **Emergency Loan Constraints** — Prevents over-borrowing within tiers
2. **Historical Contributions** — Records past savings deposits to accurate member balance
3. **Historical Withdrawals** — Records past savings withdrawals to complete the picture

**Net Result:** Members have complete, accurate financial history in the system, enabling proper eligibility calculations and comprehensive reports for decision-making.
