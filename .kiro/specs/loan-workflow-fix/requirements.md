# Loan Workflow Standardization & Fix - Requirements

## Problem Statement

The loan system has critical inconsistencies between individual loan applications (Loan Officer) and bulk loan applications (Treasurer):

1. **Missing Guarantor Support in Individual Applications**
   - Loan officers cannot add guarantors when applying for individual loans
   - Bulk template has guarantor fields, but individual form doesn't
   - Creates workflow inconsistency

2. **Loan Number Assignment Timing Issue**
   - Loan numbers are assigned on APPROVAL, not DISBURSEMENT
   - Should only be assigned AFTER disbursement is complete
   - Currently visible in pending/approved status before treasurer disburses

3. **Missing Eligibility Checks in Individual Applications**
   - Bulk loans validate member and guarantor eligibility during processing
   - Individual loans skip eligibility validation entirely
   - No validation happens until credit committee approval

4. **Missing Calculations in Bulk Applications**
   - Individual loans show: Total Interest, Total Repayable, Monthly Repayment
   - Bulk loans don't display these calculations
   - Creates inconsistent user experience

5. **Inconsistent Workflow Paths**
   - Loan Officer → Individual (no guarantors, no eligibility, calculations shown)
   - Treasurer → Bulk (has guarantors, eligibility checked, calculations missing)
   - Credit Committee → Reviews both but sees loan numbers already assigned
   - Treasurer → Disburses (but loan number already exists)

## Desired Workflow

### Individual Loan Application (Loan Officer)
1. Loan officer selects member and loan product
2. Enters amount and term
3. **NEW**: Adds guarantors (1 or more)
4. System calculates and displays: interest, repayable, monthly payment
5. Loan officer submits application
6. Loan status: PENDING (no loan number yet)
7. Credit committee reviews and validates member + guarantor eligibility
8. If approved: status = APPROVED (still no loan number)
9. Treasurer disburses loan
10. **NEW**: Loan number assigned on disbursement
11. Status = DISBURSED

### Bulk Loan Application (Treasurer)
1. Treasurer uploads Excel with multiple loans
2. Each row has: member, product, amount, purpose, guarantor1, guarantor2
3. **NEW**: System calculates and displays: interest, repayable, monthly payment for each
4. System validates: member exists, product exists, amount in range, guarantors exist
5. Batch status: PENDING
6. Credit committee reviews batch
7. **NEW**: System validates member and guarantor eligibility for each loan
8. If approved: batch status = APPROVED (loans still have no loan numbers)
9. Treasurer disburses batch
10. **NEW**: Loan numbers assigned on disbursement
11. Status = DISBURSED

## Key Changes Required

### 1. Loan Number Assignment
- **Current**: Generated on approval using `LN-YYYY-NNNNN` format
- **New**: Generated on disbursement
- **Fix**: Make loanNumber nullable, generate only during disbursement
- **Improvement**: Use year-specific counter to avoid duplicates

### 2. Individual Loan Application Form
- **Add**: Guarantor selection (multi-select or add/remove)
- **Add**: Guarantor eligibility validation on form submission
- **Keep**: Calculation preview (already working)
- **Add**: Display eligibility check results before submission

### 3. Bulk Loan Processing
- **Add**: Calculate and display interest/repayable/monthly for each loan
- **Add**: Guarantor eligibility validation during batch approval
- **Add**: Show eligibility results in batch review UI
- **Fix**: Ensure loan numbers not assigned until disbursement

### 4. Credit Committee Approval
- **Add**: Display guarantor eligibility results
- **Add**: Show calculated fields (interest, repayable, monthly)
- **Add**: Option to view guarantor details and validation results

### 5. Treasurer Disbursement
- **Add**: Assign loan numbers during disbursement
- **Add**: Confirmation that loan numbers were assigned
- **Add**: Bulk disbursement should assign numbers to all loans

## Data Model Changes

### Loan Entity
- `loanNumber`: Change from auto-generated on creation to nullable, generated on disbursement
- Add `disbursedAt` field if not present
- Ensure `totalInterest`, `totalRepayable`, `monthlyRepayment` are always calculated

### BulkLoanItem Entity
- Add `totalInterest` field
- Add `totalRepayable` field
- Add `monthlyRepayment` field
- Add `guarantor1EligibilityStatus` field
- Add `guarantor2EligibilityStatus` field

### Guarantor Entity
- Ensure `pledgeAmount` is set during application (currently not set)

## Validation Rules

### Member Eligibility (on individual application)
- Member must be ACTIVE
- Member must have completed KYC
- Member must not have defaulted loans (configurable)

### Guarantor Eligibility (on individual application)
- Guarantor must be ACTIVE
- Guarantor must have minimum savings balance (from LoanEligibilityRules)
- Guarantor's total balance ≥ minGuarantorSavingsToLoanRatio × loanAmount
- Guarantor must not have defaulted loans (unless allowed)
- Guarantor commitments < maxGuarantorCommitments
- Guarantor's outstanding balance < maxGuarantorOutstandingToSavingsRatio × totalBalance

### Bulk Loan Validation (on batch approval)
- Same as individual application
- Applied to each loan in batch
- Results displayed in batch review UI

## Success Criteria

1. ✓ Individual loan applications support guarantors
2. ✓ Loan numbers only assigned on disbursement
3. ✓ Eligibility validation performed for both individual and bulk applications
4. ✓ Calculations displayed consistently in both workflows
5. ✓ Credit committee can see eligibility results before approval
6. ✓ Treasurer can see loan numbers assigned after disbursement
7. ✓ No loan numbers visible in PENDING or APPROVED status
8. ✓ Bulk and individual workflows are consistent
