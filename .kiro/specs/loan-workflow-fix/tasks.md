# Loan Workflow Standardization & Fix - Implementation Tasks

## Phase 1: Backend - Loan Number Generation & Disbursement

### 1. Create Loan Number Generation Service
- [x] 1.1 Create `LoanNumberGenerationService` class
- [x] 1.2 Implement `generateLoanNumber(Loan loan)` method with year-specific counter
- [x] 1.3 Add database query to count loans by year
- [ ] 1.4 Add unit tests for loan number generation

### 2. Update Loan Entity
- [x] 2.1 Ensure `loanNumber` column is nullable in database
- [x] 2.2 Add migration to make loanNumber nullable if needed (V13)
- [x] 2.3 Verify `disbursementDate` and `disbursedBy` fields exist
- [ ] 2.4 Add index on disbursement_date for performance

### 3. Update Loan Service - Individual Applications
- [x] 3.1 Modify `applyForLoan()` to accept `guarantorIds` list
- [x] 3.2 Add validation that guarantors exist and are ACTIVE
- [x] 3.3 Create Guarantor records for each guarantor
- [x] 3.4 Ensure loan is created WITHOUT loan number (null)
- [ ] 3.5 Add unit tests for individual loan application with guarantors

### 4. Update Loan Service - Approval
- [x] 4.1 Modify `approveLoan()` to validate member eligibility
- [x] 4.2 Add guarantor eligibility validation for all guarantors
- [x] 4.3 Store validation results for credit committee review
- [x] 4.4 Ensure loan number is NOT generated on approval
- [ ] 4.5 Add unit tests for approval with eligibility validation

### 5. Update Loan Service - Disbursement
- [x] 5.1 Modify `disburseLoan()` to generate loan number
- [x] 5.2 Create disbursement transaction
- [x] 5.3 Update member's savings account balance
- [x] 5.4 Set status to DISBURSED
- [ ] 5.5 Add unit tests for disbursement with loan number assignment

### 6. Create Member Validation Service
- [x] 6.1 Create `MemberValidationService` class
- [x] 6.2 Implement `validateMember(Member member)` method
- [x] 6.3 Check member is ACTIVE
- [x] 6.4 Check member has completed KYC
- [x] 6.5 Check member doesn't have defaulted loans (configurable)
- [x] 6.6 Return `MemberValidationResult` with eligibility status

## Phase 2: Backend - Bulk Loan Processing

### 7. Update BulkLoanItem Entity
- [x] 7.1 Add `totalInterest` field to BulkLoanItem
- [x] 7.2 Add `totalRepayable` field to BulkLoanItem
- [x] 7.3 Add `monthlyRepayment` field to BulkLoanItem
- [x] 7.4 Add `guarantor1EligibilityStatus` field
- [x] 7.5 Add `guarantor2EligibilityStatus` field
- [x] 7.6 Create migration for new fields (V14)

### 8. Update Bulk Processing Service - Loan Item Processing
- [x] 8.1 Modify `processLoanItem()` to calculate financials
- [x] 8.2 Store calculations in BulkLoanItem
- [x] 8.3 Create loan WITHOUT loan number
- [x] 8.4 Create Guarantor records for guarantor1 and guarantor2
- [ ] 8.5 Add unit tests for bulk loan item processing

### 9. Update Bulk Processing Service - Batch Approval
- [x] 9.1 Modify `approveBatch()` to validate member eligibility
- [x] 9.2 Add guarantor eligibility validation for each loan
- [x] 9.3 Store eligibility status in BulkLoanItem
- [x] 9.4 Set loan status to APPROVED (not DISBURSED)
- [x] 9.5 Ensure loan numbers are NOT generated
- [ ] 9.6 Add unit tests for batch approval with eligibility

### 10. Create Bulk Disbursement Service
- [x] 10.1 Create `disburseBatch()` method in BulkProcessingService (as `bulkDisburseLoanItems()`)
- [x] 10.2 Generate loan numbers for all loans in batch
- [x] 10.3 Create disbursement transactions for each loan
- [x] 10.4 Update member account balances
- [x] 10.5 Set loan status to DISBURSED
- [x] 10.6 Update batch status to DISBURSED
- [ ] 10.7 Add unit tests for batch disbursement

### 11. Update Loan Controller
- [x] 11.1 Update `applyForLoan()` endpoint to accept guarantorIds
- [x] 11.2 Update `approveLoan()` endpoint documentation
- [x] 11.3 Update `disburseLoan()` endpoint documentation
- [x] 11.4 Add `GET /api/loans/{loanId}/eligibility` endpoint (as `/validate-guarantors`)
- [ ] 11.5 Add integration tests for all endpoints

## Phase 3: Frontend - Individual Loan Application

### 12. Update Loans.tsx - Guarantor Support
- [x] 12.1 Add guarantor selection UI (multi-select or add/remove)
- [x] 12.2 Fetch active members for guarantor dropdown
- [x] 12.3 Display guarantor details (member number, name, balance)
- [x] 12.4 Add button to add/remove guarantors
- [x] 12.5 Validate at least one guarantor is selected (if required)

### 13. Update Loans.tsx - Eligibility Validation
- [ ] 13.1 Add eligibility check on form submission
- [ ] 13.2 Call member validation endpoint
- [ ] 13.3 Call guarantor validation endpoint for each guarantor
- [ ] 13.4 Display validation results to user
- [ ] 13.5 Show errors/warnings before submission
- [ ] 13.6 Prevent submission if validation fails

### 14. Update Loans.tsx - Loan Number Display
- [x] 14.1 Only show loan number if status is DISBURSED or later
- [x] 14.2 Show "—" or "Pending" for PENDING/APPROVED status
- [x] 14.3 Update loan details view to hide loan number for early statuses

### 15. Update Loans.tsx - Guarantor Information
- [x] 15.1 Display list of guarantors in loan details view
- [x] 15.2 Show guarantor member number and name
- [x] 15.3 Show guarantor eligibility status
- [x] 15.4 Show guarantor validation errors/warnings

## Phase 4: Frontend - Bulk Loan Processing

### 16. Update BulkProcessing.tsx - Calculations Display
- [x] 16.1 Add columns for totalInterest, totalRepayable, monthlyRepayment
- [x] 16.2 Display calculations in batch review table
- [x] 16.3 Format currency values properly
- [x] 16.4 Add summary row with totals

### 17. Update BulkProcessing.tsx - Eligibility Display
- [x] 17.1 Add columns for member eligibility status
- [x] 17.2 Add columns for guarantor1 and guarantor2 eligibility
- [x] 17.3 Display eligibility status with color coding
- [x] 17.4 Show validation errors/warnings on hover or in details
- [x] 17.5 Add filter to show only failed items

### 18. Update BulkProcessing.tsx - Loan Number Display
- [x] 18.1 Only show loan number if status is DISBURSED
- [x] 18.2 Show "—" for PENDING/APPROVED status
- [x] 18.3 Update batch details view accordingly

### 19. Update BulkProcessing.tsx - Disbursement UI
- [x] 19.1 Add "Disburse Batch" button for APPROVED batches
- [x] 19.2 Show confirmation dialog before disbursement
- [x] 19.3 Display loan numbers after disbursement
- [x] 19.4 Show success message with loan numbers assigned

## Phase 5: Testing & Validation

### 20. Unit Tests - Backend
- [ ] 20.1 Test loan number generation with year-specific counter
- [ ] 20.2 Test member eligibility validation
- [ ] 20.3 Test guarantor eligibility validation
- [ ] 20.4 Test individual loan application with guarantors
- [ ] 20.5 Test bulk loan processing with calculations
- [ ] 20.6 Test approval with eligibility validation
- [ ] 20.7 Test disbursement with loan number assignment

### 21. Integration Tests - Backend
- [ ] 21.1 Test complete individual loan workflow
- [ ] 21.2 Test complete bulk loan workflow
- [ ] 21.3 Test loan number only appears after disbursement
- [ ] 21.4 Test eligibility validation blocks ineligible loans
- [ ] 21.5 Test calculations are accurate

### 22. Frontend Tests
- [ ] 22.1 Test guarantor selection in individual form
- [ ] 22.2 Test eligibility validation display
- [ ] 22.3 Test loan number visibility based on status
- [ ] 22.4 Test bulk calculations display
- [ ] 22.5 Test bulk eligibility display

### 23. End-to-End Tests
- [ ] 23.1 Complete individual loan workflow (loan officer → credit committee → treasurer)
- [ ] 23.2 Complete bulk loan workflow (treasurer → credit committee → treasurer)
- [ ] 23.3 Verify loan numbers only appear after disbursement
- [ ] 23.4 Verify eligibility validation works correctly
- [ ] 23.5 Verify calculations are consistent

## Phase 6: Documentation & Deployment

### 24. Documentation
- [ ] 24.1 Update API documentation for modified endpoints
- [ ] 24.2 Document new endpoints (eligibility check, batch disburse)
- [ ] 24.3 Document loan number generation logic
- [ ] 24.4 Document eligibility validation rules
- [ ] 24.5 Create user guide for new guarantor workflow

### 25. Database Migrations
- [ ] 25.1 Create migration to make loanNumber nullable
- [ ] 25.2 Create migration to add BulkLoanItem calculation fields
- [ ] 25.3 Create migration to add eligibility status fields
- [ ] 25.4 Create index on disbursement_date
- [ ] 25.5 Test migrations on staging database

### 26. Deployment
- [ ] 26.1 Code review and approval
- [ ] 26.2 Merge to main branch
- [ ] 26.3 Run database migrations
- [ ] 26.4 Deploy backend
- [ ] 26.5 Deploy frontend
- [ ] 26.6 Monitor for issues

## Correctness Properties to Validate

### CP1: Loan Number Assignment Timing
**Property**: Loan numbers are only assigned during disbursement, not before
**Test**:
- Create individual loan → verify loanNumber is null
- Approve loan → verify loanNumber is still null
- Disburse loan → verify loanNumber is assigned
- Same for bulk loans

### CP2: Guarantor Support Consistency
**Property**: Both individual and bulk applications support guarantors
**Test**:
- Individual application allows adding guarantors
- Bulk application has guarantor fields
- Both create Guarantor records
- Both validate guarantor eligibility

### CP3: Eligibility Validation Completeness
**Property**: All loans are validated for member and guarantor eligibility
**Test**:
- Individual loan validation on approval
- Bulk loan validation on batch approval
- Ineligible loans are rejected
- Validation results are displayed to credit committee

### CP4: Calculation Accuracy
**Property**: Calculations are consistent between individual and bulk
**Test**:
- Same loan amount/term produces same interest/repayable/monthly
- Calculations match formula: Interest = Principal × Rate × Time
- Monthly = (Principal + Interest) / Months

### CP5: Workflow Consistency
**Property**: Individual and bulk workflows follow same approval/disbursement path
**Test**:
- Both start in PENDING status
- Both move to APPROVED after credit committee approval
- Both move to DISBURSED after treasurer disbursement
- Both get loan numbers on disbursement
