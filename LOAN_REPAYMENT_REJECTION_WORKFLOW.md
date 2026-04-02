# Loan Repayment Rejection Workflow

**Date:** April 2, 2026  
**Status:** Implementation Complete  
**Rebuild Required:** YES

---

## Overview

Implemented a complete rejection workflow for invalid bank transfer loan repayments. When a teller rejects a repayment request, the member is notified and can resubmit with corrected information.

---

## What Happens When Teller Rejects

### 1. Teller Action
```
Teller views pending repayment request
    ↓
Teller downloads proof file
    ↓
Teller identifies issue (e.g., amount mismatch, unclear proof, wrong account)
    ↓
Teller clicks "Reject" button
    ↓
Teller enters rejection reason
    ↓
Teller clicks "Reject"
```

### 2. Backend Processing
```
Request status changed to REJECTED
    ↓
Rejection reason stored
    ↓
Teller name and timestamp recorded
    ↓
Notification sent to member
    ↓
Audit trail logged
```

### 3. Member Notification
```
Member receives notification:
  Title: "Loan Repayment Request Rejected"
  Message: "Your bank transfer repayment request for KES [Amount] has been rejected. 
            Reason: [Rejection Reason]. 
            Please review and resubmit with corrected information."
```

### 4. Member Can Resubmit
```
Member clicks notification
    ↓
Navigates to rejection details page
    ↓
Sees rejection reason
    ↓
Can resubmit with:
  - Different amount (if needed)
  - New proof file
  - Description
    ↓
New request created (original not modified)
    ↓
Teller receives notification of resubmission
```

---

## Backend Implementation

### Rejection Endpoint Enhanced

**File:** `TellerLoanRepaymentController.java`

**Changes:**
- Sends notification to member when rejecting
- Includes rejection reason in notification
- Stores rejection details for member reference
- Logs audit trail with rejection reason

```java
// When teller rejects:
1. Update request status to REJECTED
2. Store rejection reason
3. Record teller name and timestamp
4. Send notification to member with reason
5. Log audit trail
```

### New Member Endpoints

**File:** `MemberPortalController.java`

**Endpoints Added:**

1. **Get Rejection Details**
```
GET /api/member/loan-repayment-requests/{requestId}/rejection-details

Response:
{
  "requestId": 1,
  "loanId": 5,
  "loanNumber": "LN-001",
  "requestedAmount": 50000,
  "rejectionReason": "Proof document is unclear",
  "rejectedBy": "teller_username",
  "rejectedAt": "2026-04-02T10:30:00",
  "outstandingBalance": 100000,
  "canResubmit": true
}
```

2. **Resubmit Rejected Request**
```
POST /api/member/loan-repayment-requests/{requestId}/resubmit
Content-Type: multipart/form-data

Parameters:
- amount (BigDecimal) - New amount
- description (String, optional) - Description
- proofFile (File) - New proof file

Response:
{
  "success": true,
  "message": "Repayment request resubmitted successfully",
  "data": <newRequestId>
}
```

---

## Frontend Implementation

### New Member Page

**File:** `MemberLoanRepaymentStatus.tsx`

**Features:**
- Display rejection details
- Show rejection reason prominently
- Display who rejected and when
- Show outstanding balance
- Allow resubmission with new amount and proof
- File upload with validation
- Tips for successful resubmission

**UI Components:**
- Red alert card showing rejection details
- Rejection reason in highlighted box
- Resubmission form with:
  - Amount input (max = outstanding balance)
  - Description field
  - File upload with drag-and-drop
  - Tips section
- Navigation back to loan balances

### Route Added

**File:** `App.tsx`

**Route:**
```
/member/loan-repayment-status/:requestId
```

---

## Complete Rejection Workflow

### Step-by-Step

```
1. MEMBER SUBMITS BANK TRANSFER
   ├─ Uploads proof file
   ├─ Request created with status PENDING
   └─ Teller receives notification

2. TELLER REVIEWS REQUEST
   ├─ Views pending request
   ├─ Downloads proof file
   ├─ Identifies issue
   └─ Clicks "Reject"

3. TELLER ENTERS REJECTION REASON
   ├─ Enters reason (e.g., "Proof document is unclear")
   ├─ Clicks "Reject"
   └─ Request status changed to REJECTED

4. MEMBER RECEIVES NOTIFICATION
   ├─ Notification title: "Loan Repayment Request Rejected"
   ├─ Message includes rejection reason
   ├─ Member can click to view details
   └─ Member can resubmit

5. MEMBER VIEWS REJECTION DETAILS
   ├─ Sees rejection reason
   ├─ Sees who rejected and when
   ├─ Sees outstanding balance
   └─ Can resubmit form

6. MEMBER RESUBMITS REQUEST
   ├─ Enters new amount (if needed)
   ├─ Uploads corrected proof file
   ├─ Adds description
   ├─ Clicks "Resubmit"
   └─ New request created

7. NEW REQUEST CREATED
   ├─ Original request remains REJECTED (not modified)
   ├─ New request created with status PENDING
   ├─ New request ID different from original
   └─ Teller receives notification of resubmission

8. TELLER REVIEWS RESUBMISSION
   ├─ Sees "Resubmitted Loan Repayment Request" notification
   ├─ Views new request
   ├─ Downloads new proof file
   ├─ Approves or rejects again
   └─ Process repeats if rejected
```

---

## Rejection Reasons (Examples)

Tellers can enter any reason, but common ones include:

- "Proof document is unclear or illegible"
- "Amount in proof doesn't match requested amount"
- "Recipient account details don't match"
- "Transaction reference not found"
- "Proof shows different date than submission"
- "Bank transfer not yet received"
- "Duplicate submission detected"
- "Proof file is corrupted or invalid"

---

## Data Integrity

### Original Request Preserved
- Original rejected request is NOT modified
- Original request ID remains the same
- Original rejection reason stored
- Original teller name and timestamp recorded

### New Request Created
- New request ID generated
- New request status = PENDING
- New request linked to same loan
- Audit trail shows relationship to original request

### Audit Trail
```
Original rejection:
  Action: LOAN_REPAYMENT_REJECTED
  Details: "Bank transfer repayment rejected for loan 5 - 
            Amount: 50000 - Reason: Proof document is unclear"

Resubmission:
  Action: LOAN_REPAYMENT_REQUEST_RESUBMITTED
  Details: "Bank transfer repayment request resubmitted for loan 5 
            (original request 1 was rejected)"
```

---

## Notification Flow

### Rejection Notification
```
To: Member
Title: "Loan Repayment Request Rejected"
Message: "Your bank transfer repayment request for KES 50,000 has been rejected. 
          Reason: Proof document is unclear. 
          Please review and resubmit with corrected information."
Type: LOAN_REPAYMENT_REJECTED
Entity: LoanRepaymentRequest
Entity ID: Original request ID
```

### Resubmission Notification
```
To: All Tellers
Title: "Resubmitted Loan Repayment Request"
Message: "Member John Doe resubmitted a bank transfer repayment request for 
          KES 50,000 (previously rejected)"
Type: LOAN_REPAYMENT_REQUEST
Entity: LoanRepaymentRequest
Entity ID: New request ID
```

---

## Testing Checklist

### Teller Rejection
- [ ] Can click "Reject" button on pending request
- [ ] Can enter rejection reason
- [ ] Request status changes to REJECTED
- [ ] Rejection reason stored correctly
- [ ] Teller name and timestamp recorded
- [ ] Audit trail logged

### Member Notification
- [ ] Member receives rejection notification
- [ ] Notification shows correct message
- [ ] Notification includes rejection reason
- [ ] Member can click notification

### Member Rejection Details Page
- [ ] Can navigate to rejection details page
- [ ] Sees rejection reason prominently
- [ ] Sees who rejected and when
- [ ] Sees outstanding balance
- [ ] Can view original requested amount

### Member Resubmission
- [ ] Can enter new amount
- [ ] Can upload new proof file
- [ ] File validation works (size, format)
- [ ] Can add description
- [ ] Can click "Resubmit"
- [ ] New request created
- [ ] Original request not modified
- [ ] Teller receives resubmission notification

### Data Integrity
- [ ] Original request remains REJECTED
- [ ] New request has different ID
- [ ] Audit trail shows both requests
- [ ] Rejection reason preserved
- [ ] Teller name and timestamp preserved

---

## Files Modified/Created

### Created
- `minetsacco-main/src/pages/MemberLoanRepaymentStatus.tsx` - Member rejection details page
- `LOAN_REPAYMENT_REJECTION_WORKFLOW.md` - This file

### Modified
- `backend/src/main/java/com/minet/sacco/controller/TellerLoanRepaymentController.java` - Enhanced rejection with notification
- `backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java` - Added rejection details and resubmission endpoints
- `minetsacco-main/src/pages/LoanRepaymentRequests.tsx` - Enhanced rejection display
- `minetsacco-main/src/App.tsx` - Added new route

---

## Frontend Rebuild Required

**YES - Frontend rebuild is required.**

**Reason:**
- New member page created
- New route added
- App.tsx updated

**Steps:**

```bash
cd minetsacco-main
npm run build
npx cap sync android
cd android
./gradlew.bat assembleDebug
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Answer to Your Question

**Q: How have you handled the case in which the teller sees that the payment was not valid?**

**A: Complete rejection workflow with member notification and resubmission capability:**

1. **Teller Rejects** - Enters rejection reason
2. **Member Notified** - Gets notification with reason
3. **Member Views Details** - Can see why it was rejected
4. **Member Resubmits** - Can upload corrected proof and resubmit
5. **New Request Created** - Original preserved, new request created
6. **Teller Reviews Again** - Can approve or reject resubmission
7. **Audit Trail** - All actions logged for compliance

This ensures:
- ✅ Members understand why their payment was rejected
- ✅ Members can correct and resubmit
- ✅ Tellers have clear audit trail
- ✅ Original request preserved for compliance
- ✅ No data loss or modification

---

**Implementation Status:** ✅ Complete  
**Testing Status:** ⏳ Pending  
**Deployment Status:** ⏳ Pending
