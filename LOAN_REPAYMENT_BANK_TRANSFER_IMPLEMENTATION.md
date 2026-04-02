# Loan Repayment Bank Transfer Implementation

**Date:** April 2, 2026  
**Status:** Implementation Complete  
**Rebuild Required:** YES

---

## Overview

Implemented a bank transfer repayment workflow that mirrors the deposit request process. Members can now repay loans via:
1. **Savings Deduction** - Immediate processing (existing)
2. **Bank Transfer** - Requires teller approval (new)

---

## Changes Made

### Frontend Changes

**File:** `minetsacco-main/src/components/LoanRepaymentForm.tsx`

**Changes:**
- Removed CASH payment option
- Changed default payment method to SAVINGS_DEDUCTION
- Added file upload for BANK_TRANSFER method
- Updated form validation to require proof file for bank transfers
- Split submission logic:
  - SAVINGS_DEDUCTION → Direct API call to `/member/repay-loan` (immediate)
  - BANK_TRANSFER → New API call to `/member/request-loan-repayment` (pending approval)
- Updated info alert to reflect different processing flows

**New Features:**
- Drag-and-drop file upload for proof of payment
- File size validation (max 5MB)
- Supported formats: PDF, JPG, PNG
- Clear messaging about approval workflow

### Backend Changes

**New Entity:** `LoanRepaymentRequest.java`
- Tracks pending bank transfer repayment requests
- Fields: loan, member, amount, paymentMethod, proofFilePath, status, confirmedAmount, approvedBy, etc.
- Status enum: PENDING, APPROVED, REJECTED

**New Repository:** `LoanRepaymentRequestRepository.java`
- Queries for pending requests, member requests, loan requests

**New Controller:** `TellerLoanRepaymentController.java`
- Endpoints for teller to manage repayment requests:
  - `GET /api/teller/loan-repayments/pending` - List pending requests
  - `GET /api/teller/loan-repayments` - List all requests (with filtering)
  - `GET /api/teller/loan-repayments/{requestId}` - Get request details
  - `POST /api/teller/loan-repayments/{requestId}/approve` - Approve with confirmed amount
  - `POST /api/teller/loan-repayments/{requestId}/reject` - Reject with reason

**Updated Controller:** `MemberPortalController.java`
- New endpoint: `POST /member/request-loan-repayment` - Submit bank transfer request with file
- New endpoint: `GET /member/loan-repayment-requests` - Get member's repayment requests
- New endpoint: `GET /member/loan-repayment-requests/{requestId}/proof/download` - Download proof file
- Added repository injection: `LoanRepaymentRequestRepository`
- Added service injection: `AuditService`

**New Database Migration:** `V63__Create_loan_repayment_requests_table.sql`
- Creates `loan_repayment_requests` table
- Indexes on status, member_id, loan_id, created_at

---

## Workflow

### Member Submits Bank Transfer Repayment

```
1. Member opens "Make Loan Repayment" dialog
2. Selects loan and amount
3. Chooses "Bank Transfer" payment method
4. Uploads proof file (receipt/screenshot)
5. Clicks "Make Repayment"
6. Request submitted to backend
7. File saved to uploads/loan-repayments/
8. LoanRepaymentRequest created with status PENDING
9. Audit trail logged
10. Member sees: "Repayment request submitted. Teller will verify and approve."
```

### Teller Approves Bank Transfer Repayment

```
1. Teller navigates to Loan Repayment Requests (new page)
2. Views pending requests
3. Clicks on request to view details
4. Downloads proof file to verify
5. Enters confirmed amount (may differ from requested)
6. Clicks "Approve"
7. Backend:
   - Creates LoanRepayment record
   - Updates loan outstanding balance
   - Creates Transaction record (audit trail)
   - Releases guarantor pledges if fully repaid
   - Updates LoanRepaymentRequest status to APPROVED
   - Logs audit trail
8. Member's loan balance updated
9. Teller sees confirmation
```

### Teller Rejects Bank Transfer Repayment

```
1. Teller views pending request
2. Clicks "Reject"
3. Enters rejection reason
4. Backend:
   - Updates LoanRepaymentRequest status to REJECTED
   - Stores rejection reason
   - Logs audit trail
5. Member can resubmit with corrected information
```

---

## Key Features

✅ **File Upload** - Members upload proof of payment (PDF, JPG, PNG)  
✅ **Teller Verification** - Teller reviews and confirms amount  
✅ **Flexible Amount** - Teller can confirm different amount than requested  
✅ **Audit Trail** - All actions logged with user, timestamp, details  
✅ **Guarantor Release** - Pledges released when loan fully repaid  
✅ **Transaction Recording** - Creates transaction record for accounting  
✅ **Rejection Handling** - Members can resubmit if rejected  
✅ **File Download** - Teller can download proof file for verification  

---

## API Endpoints

### Member Endpoints

**Submit Bank Transfer Repayment Request**
```
POST /api/member/request-loan-repayment
Content-Type: multipart/form-data

Parameters:
- loanId (Long) - Loan ID
- amount (BigDecimal) - Repayment amount
- paymentMethod (String) - "BANK_TRANSFER"
- description (String, optional) - Description
- proofFile (File) - Proof of payment

Response:
{
  "success": true,
  "message": "Repayment request submitted successfully",
  "data": <requestId>
}
```

**Get Member's Repayment Requests**
```
GET /api/member/loan-repayment-requests

Response:
[
  {
    "id": 1,
    "loan": {...},
    "amount": 50000,
    "status": "PENDING",
    "createdAt": "2026-04-02T10:30:00",
    ...
  }
]
```

**Download Proof File**
```
GET /api/member/loan-repayment-requests/{requestId}/proof/download

Response: File download
```

### Teller Endpoints

**Get Pending Repayment Requests**
```
GET /api/teller/loan-repayments/pending

Response:
[
  {
    "id": 1,
    "loan": {...},
    "member": {...},
    "amount": 50000,
    "status": "PENDING",
    ...
  }
]
```

**Approve Repayment Request**
```
POST /api/teller/loan-repayments/{requestId}/approve
Content-Type: application/json

Parameters:
- confirmedAmount (BigDecimal) - Amount to confirm

Response:
{
  "success": true,
  "message": "Repayment request approved successfully",
  "data": <updatedRequest>
}
```

**Reject Repayment Request**
```
POST /api/teller/loan-repayments/{requestId}/reject
Content-Type: application/json

Parameters:
- rejectionReason (String) - Reason for rejection

Response:
{
  "success": true,
  "message": "Repayment request rejected successfully",
  "data": <updatedRequest>
}
```

---

## Database Schema

### loan_repayment_requests Table

```sql
CREATE TABLE loan_repayment_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    proof_file_path VARCHAR(500),
    proof_file_name VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    approved_by VARCHAR(255),
    confirmed_amount DECIMAL(19, 2),
    rejection_reason TEXT,
    
    FOREIGN KEY (loan_id) REFERENCES loans(id),
    FOREIGN KEY (member_id) REFERENCES members(id),
    
    INDEX idx_status (status),
    INDEX idx_member_id (member_id),
    INDEX idx_loan_id (loan_id),
    INDEX idx_created_at (created_at)
);
```

---

## Frontend Rebuild Required

**YES - Frontend rebuild is required.**

**Reason:** The LoanRepaymentForm component has been significantly updated with:
- New file upload functionality
- New API endpoint calls
- Updated form logic and validation
- New UI elements (file upload area)

**Steps to Rebuild:**

```bash
# 1. Build frontend
cd minetsacco-main
npm run build

# 2. Sync with Capacitor (for mobile)
npx cap sync android

# 3. Rebuild APK (if needed)
cd android
./gradlew.bat assembleDebug

# 4. Install on device
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

**For Web Testing:**
```bash
# Start dev server
npm run dev

# Visit http://localhost:3000/member
```

---

## Backend Deployment

**Steps:**

```bash
# 1. Rebuild backend
cd backend
mvn clean package -DskipTests

# 2. Restart backend
# Stop current instance (Ctrl+C)
# Start new instance
mvn spring-boot:run

# Flyway will automatically run V63 migration
```

---

## Testing Checklist

### Member Side
- [ ] Can select SAVINGS_DEDUCTION and repay immediately
- [ ] Can select BANK_TRANSFER and upload proof file
- [ ] File upload validation works (size, format)
- [ ] Repayment request submitted successfully
- [ ] Can view pending repayment requests
- [ ] Can download proof file

### Teller Side
- [ ] Can view pending repayment requests
- [ ] Can view request details
- [ ] Can download proof file
- [ ] Can approve with confirmed amount
- [ ] Can reject with reason
- [ ] Loan balance updates correctly after approval
- [ ] Guarantor pledges released if fully repaid
- [ ] Audit trail records all actions

### Data Integrity
- [ ] Transaction record created for each approval
- [ ] Loan status updated correctly
- [ ] Outstanding balance calculated correctly
- [ ] Guarantor status updated to RELEASED
- [ ] Audit trail entries complete and accurate

---

## Files Modified/Created

### Created
- `backend/src/main/java/com/minet/sacco/entity/LoanRepaymentRequest.java`
- `backend/src/main/java/com/minet/sacco/repository/LoanRepaymentRequestRepository.java`
- `backend/src/main/java/com/minet/sacco/controller/TellerLoanRepaymentController.java`
- `backend/src/main/resources/db/migration/V63__Create_loan_repayment_requests_table.sql`
- `LOAN_REPAYMENT_BANK_TRANSFER_IMPLEMENTATION.md` (this file)

### Modified
- `minetsacco-main/src/components/LoanRepaymentForm.tsx`
- `backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java`

---

## Next Steps

1. **Rebuild Frontend** - Run `npm run build` and test
2. **Rebuild Backend** - Run `mvn clean package` and restart
3. **Test Workflows** - Follow testing checklist
4. **Create Teller UI** - Build page for teller to manage repayment requests
5. **Update Documentation** - Add to USAGE_GUIDE.md
6. **Commit to GitHub** - Push all changes

---

## Notes

- Files are stored in `uploads/loan-repayments/` directory
- Ensure directory exists and is writable
- File cleanup policy: Consider implementing periodic cleanup of old files
- Security: Validate file types on backend (already done)
- Audit trail: All teller actions are logged for compliance

---

**Implementation Status:** ✅ Complete  
**Testing Status:** ⏳ Pending  
**Deployment Status:** ⏳ Pending
