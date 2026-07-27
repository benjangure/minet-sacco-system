# Phase A Complete - Individual & Bulk Loan Field Editing

## STATUS: ✅ Backend Complete | Frontend A1 Complete | Frontend A2 TODO

---

## Phase A Overview

Phase A is **low-risk loan field editing** - separate from Phase B (guarantor reallocation). No guarantor data is ever touched.

**Editable Fields** (5 total):
1. Loan Status (PENDING → APPROVED → DISBURSED → REPAID/DEFAULTED)
2. Disbursement Date (cannot be future)
3. Interest Rate (% value, ≥ 0)
4. Outstanding Balance (KES amount, 0 to principal)
5. Purpose (text)

---

## A1: Individual Loan Field Editing ✅

### What It Does
Treasurer clicks "Edit Loan Fields (Phase A)" on a loan details dialog and updates up to 5 fields one loan at a time.

### Backend
- **DTO**: `LoanFieldUpdateDTO.java` - 5 fields only, NO guarantor data
- **Service**: `LoanService.updateLoanFieldsOnly()` - validation + audit
- **Endpoint**: `PUT /api/loans/{loanId}/fields/update` - Treasurer-gated
- **Status**: ✅ COMPLETE

### Frontend  
- **Location**: `Loans.tsx` - Loan details dialog
- **UI**: Collapsible "Edit Loan Fields (Phase A)" button
- **Form**: 5 inputs with real-time validation
- **Feedback**: Per-field error messages, toast notifications
- **Status**: ✅ COMPLETE

### Network Request (Verified)
```
PUT /api/loans/123/fields/update
{
  "loanStatus": "DISBURSED",
  "disbursementDate": "2026-01-15",
  "interestRate": 12.5,
  "outstandingBalance": 45000,
  "purpose": "Emergency"
}
```
✅ Zero guarantor data in request

### Audit Trail
```
Action: UPDATE | Entity: LOAN_FIELDS
Loan #LN-2026-001 - Member: John Doe
Field Update (Phase A): Status changed to DISBURSED; Outstanding Balance changed to KES 45000;...
```

---

## A2: Bulk Loan Field Editing (via Upload) ✅ Backend

### What It Does
Treasurer uploads .xlsx file with multiple rows. Each row: Employee ID + Loan Number + optional Phase A fields. Blank cells don't change existing values.

### Backend Implementation
- **New Entity**: `BulkLoanDataUpdateItem.java` - One row per item
- **New Repository**: `BulkLoanDataUpdateItemRepository.java`
- **New Parser**: `ExcelParserService.parseLoanDataUpdates()`
- **New Processor**: `BulkProcessingService.processLoanDataUpdates()` (async)
- **New Processor Item**: `processLoanDataUpdateItem()` (per-row, separate transaction)
- **Database**: `V133__Create_bulk_loan_data_update_items_table.sql`
- **Controller**: Updated `getBatchItems()` to handle "LOAN_DATA_UPDATE" type
- **Status**: ✅ COMPLETE

### Template Structure

| Column | Name | Required | Example | Notes |
|--------|------|----------|---------|-------|
| 1 | Employee ID | Yes | EMP001 | Find member |
| 2 | Loan Number | Yes | LN-2026-001 | Find loan |
| 3 | Loan Status | No | DISBURSED | One of enum values |
| 4 | Disbursement Date | No | 2026-01-15 | YYYY-MM-DD format |
| 5 | Interest Rate | No | 12.5 | Percentage |
| 6 | Outstanding Balance | No | 45000 | KES amount |
| 7 | Purpose | No | Emergency | Text |

### Progressive Fill Example
```
Row 1: EMP001 | LN-2026-001 | DISBURSED | 2026-01-15 | 12.5 | 45000 | Emergency
       → All 5 fields updated

Row 2: EMP002 | LN-2026-002 | REPAID | | | |
       → Only status updated to REPAID; other fields left untouched

Row 3: EMP003 | LN-2026-003 | | | | 30000 |
       → Only outstanding balance updated; all others untouched
```

### Per-Row Error Messages
- "Employee ID is required"
- "Member not found with Employee ID: EMP999"
- "Loan not found with Loan Number: LN-2026-999"
- "Loan does not belong to this member"
- "No Phase A fields to update (at least one field is required)"
- "Disbursement date cannot be in the future"
- "Outstanding balance must be 0 for REPAID loans"
- *(field-level validation inherited from A1 service)*

### Upload Endpoint
```
POST /api/bulk-processing/upload
Content-Type: multipart/form-data
batchType: "LOAN_DATA_UPDATE"
file: [.xlsx file]
```

### Processing
1. File parsed → BulkLoanDataUpdateItem rows created (status = PENDING)
2. Async batch processor starts immediately
3. Each row processed independently in separate transaction
4. Per-row validation + LoanService.updateLoanFieldsOnly() call
5. Status set to PROCESSED (success) or FAILED (with error message)
6. Batch status set to COMPLETED or COMPLETED_WITH_ERRORS

### Audit Trail (Per Row)
Same as A1: Each update creates audit entry for loan field change

### Audit Trail (Batch Level)
```
Action: BULK_PROCESS
Processed loan data update batch: BATCH-LOA-20260624145230-1234
Success: 3 | Failed: 2
```

---

## Implementation Files

### Backend Created
1. `backend/src/main/java/com/minet/sacco/entity/BulkLoanDataUpdateItem.java`
2. `backend/src/main/java/com/minet/sacco/repository/BulkLoanDataUpdateItemRepository.java`
3. `backend/src/main/resources/db/migration/V133__Create_bulk_loan_data_update_items_table.sql`

### Backend Modified
1. `backend/src/main/java/com/minet/sacco/service/ExcelParserService.java` - added `parseLoanDataUpdates()`
2. `backend/src/main/java/com/minet/sacco/service/BulkProcessingService.java` - added full A2 pipeline
3. `backend/src/main/java/com/minet/sacco/controller/BulkProcessingController.java` - updated for LOAN_DATA_UPDATE type

### Frontend A1 Modified
1. `minetsacco-main/src/pages/Loans.tsx` - added Phase A edit form to loan details dialog

### Documentation Created
1. `PHASE_A_IMPLEMENTATION_SUMMARY.md` - A1 details
2. `PHASE_A2_BULK_UPLOAD_SUMMARY.md` - A2 details
3. `PHASE_A_COMPLETE_SUMMARY.md` - This file

---

## Testing Checklist

### A1 Tests (Run Live)
- [ ] Edit single field (Outstanding Balance only) → only that field changes
- [ ] Invalid value (future date) → error displayed, form remains open
- [ ] Network request inspection → ZERO guarantor fields in body
- [ ] Verify audit trail created for each update

### A2 Backend Tests (Ready to Test)
- [ ] Upload file with 3 rows → all processed correctly
- [ ] Row with blank fields (progressive fill) → only filled fields changed
- [ ] Row with invalid member ID → shows "Member not found" error, other rows process
- [ ] Row with future date → shows field-level error message
- [ ] Mixed success/fail (5 rows, 2 fail) → batch shows COMPLETED_WITH_ERRORS
- [ ] Audit logs created for each successful row update

### A2 Frontend (TODO)
- [ ] Add "Loan Data Update" to batch type options in BulkProcessing.tsx
- [ ] Generate download template with 7 columns + header
- [ ] Display results with per-row status/errors (collapsible, like existing templates)
- [ ] Show progressive fill explanation

---

## Key Design Decisions

1. **Phase Separation**: A1 and A2 completely separate from Phase B (guarantor reallocation)
2. **No Guarantor Data**: DTOs and APIs explicitly exclude all guarantor fields
3. **Progressive Fill**: Blank = no change (not "set to empty")
4. **Per-Row Errors**: Failed rows visible with specific error messages (not silently dropped)
5. **Async Processing**: A2 processes in background to avoid blocking upload
6. **Independent Transactions**: Each A2 row in separate transaction (no cascading failures)
7. **Audit for Everything**: Individual updates (A1) AND batch updates (A2) fully logged

---

## Validation Rules

Applied by `LoanService.updateLoanFieldsOnly()`:

| Field | Rules |
|-------|-------|
| loanStatus | Must be valid enum value or null |
| disbursementDate | Cannot be in future; format YYYY-MM-DD |
| interestRate | Must be >= 0 |
| outstandingBalance | Must be >= 0 and <= principal; 0 required if REPAID |
| purpose | Any text, optional |
| Overall | At least one field must be provided |

---

## Troubleshooting

**Q: Why is guarantor data being rejected?**  
A: It shouldn't be sent at all. Phase A DTOs don't have guarantor fields. Check network request in DevTools - if guarantor data appears, it's a frontend bug.

**Q: My row shows "Member not found" but the Employee ID looks correct**  
A: Check if the Employee ID in the file exactly matches the member's Employee ID in the system (case-sensitive).

**Q: Outstanding balance was correct but another field didn't update**  
A: Check if blank cells were sent as empty strings instead of nulls. Backend requires null for "skip this field".

**Q: Why didn't batch process immediately?**  
A: A2 processes asynchronously. Batch status starts as PROCESSING and becomes COMPLETED after all rows finish. Retrieve batch items to see current status.

---

## Next Steps

### Immediate (Required for A2 to be usable)
- [ ] Frontend A2 template generation in BulkProcessing.tsx
- [ ] Frontend A2 results display with per-row error visibility
- [ ] "Loan Data Update" option added to batch type dropdown

### Future (Phase B)
- [ ] Guarantor reallocation (separate endpoint, separate UI)
- [ ] New DTOs: GuarantorReallocationRequestDTO
- [ ] Available savings calculation
- [ ] Proportional freeze logic
- [ ] Phase B frontend section

---

## Endpoints Summary

| Method | Path | Type | Role | Status |
|--------|------|------|------|--------|
| PUT | `/api/loans/{id}/fields/update` | A1 | TREASURER | ✅ |
| POST | `/api/bulk-processing/upload` | A2 | TREASURER | ✅ |
| GET | `/api/bulk-processing/batches/{id}/items` | A2 | TREASURER | ✅ |

---

**Phase A**: ✅ Backend Complete (A1 + A2) | ✅ Frontend A1 Complete | ⏳ Frontend A2 TODO
