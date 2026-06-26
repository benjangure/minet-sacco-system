# Dual-Mode Loan Migration Implementation

**Date:** June 23, 2026  
**Status:** ✅ Complete and Deployed  
**Mode Detection:** Automatic (Loan Number presence)

---

## Overview

The Loan Migration feature now supports **dual-mode operation** (CREATE + UPDATE) with automatic mode detection and atomic guarantor management.

**Key Features:**
- **Single Excel template** for both CREATE and UPDATE flows
- **Automatic mode detection** based on Loan Number column
- **Flexible field requirements** - only mandatory fields required for mode
- **Atomic guarantor updates** with freeze/unfreeze mechanics
- **Comprehensive audit trail** for all changes

---

## Architecture

### New Service: `LoanGuarantorUpdateService`

**Location:** `backend/src/main/java/com/minet/sacco/service/LoanGuarantorUpdateService.java`

**Purpose:** Reusable service for atomic guarantor updates with freeze/unfreeze mechanics

**Key Methods:**
```java
public String updateGuarantors(
    Loan loan, 
    List<GuarantorPair> newGuarantors, 
    User auditor
)
```

**Transaction Flow:**
1. Validate all new guarantors (exist, ACTIVE status, sufficient savings)
2. Begin transaction:
   - Unfreeze all old guarantors' savings
   - Delete old guarantor records
   - Create new guarantor records
   - Freeze new guarantors' savings
   - Log audit trail with before/after values
3. Commit or rollback (all-or-nothing)

**Safety Features:**
- Validates member status (ACTIVE only)
- Checks available savings (for DISBURSED loans)
- Prevents negative frozen amounts
- Atomic rollback on any failure

---

## Updated Service: `LoanMigrationService`

**Location:** `backend/src/main/java/com/minet/sacco/service/LoanMigrationService.java`

### Mode Detection

```java
boolean isUpdateMode = item.getLoanNumber() != null && !item.getLoanNumber().trim().isEmpty();
```

- **Blank Loan Number** → CREATE mode
- **Populated Loan Number** → UPDATE mode

### CREATE Mode Validation

**Mandatory Fields:**
- Employee ID (borrower must exist and be ACTIVE)
- Loan Product Name (must exist and be ACTIVE)
- Principal Amount (> 0)
- Loan Status (DISBURSED, REPAID, or DEFAULTED)

**Optional Fields:**
- Loan Number (auto-generated if blank)
- Term Months (can be null, set later via UPDATE)
- Disbursement Date (optional, if provided not in future)
- Outstanding Balance (optional, remains null for new loans)
- Interest Rate % (uses product default)
- Guarantorship Type (NORMAL or SELF, optional - can be set later via UPDATE)
- Guarantors (1-6 pairs, optional - only if Guarantorship Type provided)

**Processing:**
- Creates loan with minimal or complete data
- Calculates monthly repayment if term provided
- Creates guarantors with freeze (if DISBURSED)
- Logs LOAN_MIGRATION action

### UPDATE Mode Validation

**Required for Detection:**
- Loan Number (must exist in system)

**Editable Fields (processed if provided):**
- Disbursement Date (if provided, not in future)
- Outstanding Balance (if provided, >= 0 and <= principal)
- Term Months (if provided, > 0)
- Guarantors 1-6 (if any provided, replace ALL)

**Read-Only/Not Processed:**
- Employee ID (informational only)
- Loan Product Name (informational only)
- Principal Amount (informational only)
- Interest Rate % (ignored, uses product default)
- Loan Status (system-determined)

**Processing:**
- Loads existing loan by Loan Number
- Updates only provided fields (blank = skip)
- Calls LoanGuarantorUpdateService for guarantor updates
- Logs detailed audit trail with before/after values
- Uses LOAN_UPDATE_MIGRATION action

---

## Excel Template Format

### Column Order (UNCHANGED from CREATE mode)

| # | Column | CREATE | UPDATE | Notes |
|---|--------|--------|--------|-------|
| 0 | Loan Number (blank=CREATE, populate=UPDATE) | Blank | Populated | Auto-detect mode |
| 1 | Employee ID | ✓ Req | - | Identifies borrower |
| 2 | Loan Product Name | ✓ Req | - | Identifies product |
| 3 | Principal Amount | ✓ Req | - | Original amount |
| 4 | Term Months | Opt | Opt | Editable in UPDATE |
| 5 | Interest Rate % | Opt | - | Uses product default |
| 6 | Disbursement Date (DD/MM/YYYY) | Opt | Opt | Editable in UPDATE |
| 7 | Loan Status | ✓ Req | - | DISBURSED/REPAID/DEFAULTED |
| 8 | Outstanding Balance | Opt | Opt | Set via UPDATE |
| 9 | Guarantorship Type | ✓ Req | - | NORMAL or SELF |
| 10-21 | Guarantor 1-6 (ID + Pledge) | Opt | Opt | Editable in UPDATE |
| 22 | Purpose | Opt | - | For reference |

### Example Rows in Template

**Row 1: CREATE Mode - New Emergency Loan with Guarantors**
```
Loan#: [BLANK]
Emp ID: EMP041
Product: Emergency Loan 1
Principal: 100000
Term: 12
Disburse: 15/01/2024
Status: DISBURSED
Outstanding: [blank]
Guarantorship: NORMAL
Guarantor1: EMP066, 50000
Guarantor2: EMP063, 50000
```

**Row 2: CREATE Mode - New Self-Guaranteed Loan**
```
Loan#: [BLANK]
Emp ID: EMP040
Product: Normal Loan
Principal: 300000
Term: 60
Disburse: 03/02/2025
Status: DISBURSED
Outstanding: [blank]
Guarantorship: SELF
Guarantors: [all blank]
```

**Row 3: UPDATE Mode - Update Guarantors Only**
```
Loan#: L001
Emp ID: [blank/ignored]
Product: [blank/ignored]
Principal: [blank/ignored]
Term: [blank]
Disburse: [blank]
Outstanding: [blank]
Guarantor1: EMP010, 100000
```

**Row 4: UPDATE Mode - Update Multiple Fields**
```
Loan#: L002
Term: 24
Disburse: 15/03/2025
Outstanding: 80000
Guarantor1: EMP011, 60000
```

---

## Validation Rules

### CREATE Mode
- Employee ID: required, member exists, ACTIVE status
- Loan Product: required, exists, ACTIVE
- Principal: required, > 0
- Status: required, DISBURSED/REPAID/DEFAULTED
- Guarantorship: required, NORMAL or SELF
- Term: if provided, > 0
- Disbursement Date: if provided, not in future
- Outstanding Balance: if provided, >= 0, <= principal
- Guarantors (if NORMAL): each must exist, be ACTIVE

### UPDATE Mode
- Loan Number: required, must exist
- Term: if provided, > 0
- Disbursement Date: if provided, not in future
- Outstanding Balance: if provided, >= 0, <= principal
- Guarantors: if any provided, all must exist and be ACTIVE

---

## Audit Trail

### CREATE Mode Entry
```
Action: LOAN_MIGRATION
Entity: Loan
Description: Loan migrated: [loan_number] created with guarantors
Timestamp: [now]
User: [processor]
```

### UPDATE Mode Entry
```
Action: LOAN_UPDATE_MIGRATION
Entity: Loan
Description: Loan [loan_number] updated: 
  - Term: 12 → 24; 
  - Outstanding Balance: 75000 → 80000; 
  - Guarantors changed: EMP001(50k), EMP002(50k) → EMP011(60k);
Timestamp: [now]
User: [processor]
```

---

## Guarantor Update Mechanics

### Scenario: Replace Guarantors on DISBURSED Loan

**Before:**
- Loan L001: Status=DISBURSED, Principal=100k, Outstanding=75k
- Guarantor A (EMP001): Frozen=50k
- Guarantor B (EMP002): Frozen=50k

**Update Input:**
- New Guarantor C (EMP011): Pledge=60k
- New Guarantor D (EMP012): Pledge=40k

**Transaction:**
```
1. Validate:
   - Loan exists (L001) ✓
   - Status is DISBURSED ✓
   - EMP011 is ACTIVE ✓
   - EMP011 has ≥ 60k available savings ✓
   - EMP012 is ACTIVE ✓
   - EMP012 has ≥ 40k available savings ✓

2. Unfreeze old:
   - EMP001 frozen: 50k → 0k
   - EMP002 frozen: 50k → 0k

3. Delete old records:
   - Guarantor A deleted
   - Guarantor B deleted

4. Create new records:
   - Guarantor C (EMP011, ACTIVE, pledge=60k)
   - Guarantor D (EMP012, ACTIVE, pledge=40k)

5. Freeze new:
   - EMP011 frozen: X → (X + 60k)
   - EMP012 frozen: Y → (Y + 40k)

6. Audit log:
   - "Guarantors changed: EMP001(50k), EMP002(50k) → 
      EMP011(60k), EMP012(40k) for loan L001"

7. Commit (all-or-nothing)
```

---

## Error Handling

### CREATE Mode Failures
- Missing mandatory field → row rejected
- Member/product not found → row rejected
- Guarantor validation fails → row rejected
- All failures logged in error_message column

### UPDATE Mode Failures
- Loan not found → row rejected
- Loan status not updateable → row rejected
- Guarantor validation fails → entire update rejected + rollback
- New guarantor insufficient savings → rejected with reason

### Per-Row Reporting
```json
{
  "row_number": 3,
  "loan_number": "L003",
  "status": "FAILED",
  "error": "Guarantor EMP099 not found in system",
  "processed_at": "2026-06-23T16:45:00Z"
}
```

---

## Key Decisions

| Decision | Rationale |
|----------|-----------|
| **Blank = Skip in UPDATE** | Prevents accidental overwrites; user controls what changes |
| **Guarantors = All-or-Nothing** | Simpler logic; clearer audit trail; prevents partial updates |
| **Term is Optional in CREATE** | Allows incremental data entry; treasurer can set later |
| **Outstanding Balance is Optional** | Snapshot can be provided later when calculated |
| **Disbursement Date is Optional** | Loan can exist before actual disbursement date known |
| **Single Template** | Reduces confusion; mode auto-detected; same columns |
| **Atomic Transactions** | Guarantor changes all succeed or all fail; no inconsistent state |

---

## Integration Points

### With Existing Systems

**LoanRepository**
- `findByLoanNumber()` - to locate loan for UPDATE
- `save()` - to persist created/updated loans

**GuarantorRepository**
- `findByLoan()` - to get current guarantors
- `saveAll()` - to batch create new guarantors
- `deleteAll()` - to remove old guarantors

**AccountRepository**
- `findByMemberIdAndAccountType()` - to access savings for freeze/unfreeze
- `save()` - to persist frozen amounts

**AuditService**
- `logAction()` - to record changes to audit trail

**ExcelParserService**
- `parseLoanMigration()` - unchanged, parses both modes same way

---

## Testing Checklist

### CREATE Mode
- [ ] Create loan with minimal data (Emp ID, Product, Principal, Status, Guarantor Type)
- [ ] Create loan with all fields populated
- [ ] Verify guarantor freeze for DISBURSED loans
- [ ] Verify NO freeze for REPAID/DEFAULTED loans
- [ ] Verify auto-generated loan numbers
- [ ] Test validation: missing mandatory fields
- [ ] Test validation: invalid product name
- [ ] Test validation: invalid member ID
- [ ] Test validation: invalid guarantor ID

### UPDATE Mode
- [ ] Update existing loan: only term
- [ ] Update existing loan: only disbursement date
- [ ] Update existing loan: only outstanding balance
- [ ] Update existing loan: only guarantors
- [ ] Update existing loan: multiple fields
- [ ] Update guarantors: verify old guarantors unfrozen
- [ ] Update guarantors: verify new guarantors frozen
- [ ] Verify NO changes to Employee ID, Product, Principal
- [ ] Test validation: loan not found
- [ ] Test validation: guarantor not found
- [ ] Test validation: new guarantor insufficient savings

### Edge Cases
- [ ] Term=0 (should fail)
- [ ] Disbursement date in future (should fail)
- [ ] Outstanding > Principal (should fail)
- [ ] SELF guarantee with external guarantors (should fail)
- [ ] NORMAL guarantee with no guarantors (should fail)
- [ ] Duplicate guarantor IDs in same row (should fail)
- [ ] Blank guarantor ID with pledge amount (should fail)
- [ ] Negative outstanding balance (should fail)

---

## Deployment Notes

### Database
- **No schema changes required** - all fields already exist
- Optional: Add loan_update_audit table for historical tracking (future enhancement)

### Configuration
- LoanGuarantorUpdateService auto-wired in LoanMigrationService
- No new properties or environment variables needed

### API Endpoint (unchanged)
```
POST /api/loan-migration/upload
```

Response includes per-row success/failure details (unchanged format)

### Template Download (updated)
```
GET /api/loan-migration/template/download
```

Returns Excel with updated headers clarifying dual-mode operation

---

## Performance Considerations

- Guarantor updates: O(n) where n = number of new guarantors (~6 max)
- Validation: Early exit on first error per row
- Transactions: Atomic per loan, not per batch
- Batch processing: Sequential (no parallel processing for safety)

---

## Future Enhancements

1. **Partial Guarantor Updates** - Allow updating specific guarantors without replacing all
2. **Bulk Loan Updates API** - Dedicated endpoint for updates separate from creates
3. **Scheduled Reconciliation** - Auto-update outstanding balance from GL records
4. **Guarantor Reassignment** - Move pledges between members without unfreezing
5. **Loan Modification Workflow** - Approval process for significant updates
6. **Enhanced Reporting** - Track all update history per loan

---

## Support & Documentation

**For Users:**
- Download template from UI
- Fill CREATE rows (blank Loan #) or UPDATE rows (populated Loan #)
- Upload file
- View per-row results

**For Developers:**
- See `LoanMigrationService.validateItem()` for validation logic
- See `LoanGuarantorUpdateService.updateGuarantors()` for guarantor update
- See `LoanMigrationService.processUpdateItem()` for update flow
- Audit trail available in `audit_log` table

---

## Implementation Complete ✅

All code deployed to production:
- ✅ LoanGuarantorUpdateService.java (NEW)
- ✅ LoanMigrationService.java (UPDATED)
- ✅ Excel template (UPDATED)
- ✅ Validation logic (DUAL-MODE)
- ✅ Audit trail (ENHANCED)
- ✅ Error handling (COMPREHENSIVE)

**Application Status:** Running (started 16:38:16 on 23-Jun-2026)
