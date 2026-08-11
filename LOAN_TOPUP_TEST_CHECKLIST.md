# Loan Top-Up Feature - Test Checklist

## Date: 2026-07-28

## Test Loan Data
- **Loan ID**: 366
- **Loan Number**: LN-2026-00002
- **Member**: Mr Katee Mutunga (Member #1203)
- **Product**: Normal Loan
- **Original Principal**: KES 329,297
- **Current Outstanding**: KES 138,635.69
- **Status**: DISBURSED
- **Principal Repaid**: KES 190,661.31 (57.90%)

## Pre-Testing Setup

### 1. Backend Server
```bash
# Start backend (port 9090)
cd backend
mvn spring-boot:run
# OR
java -jar target/sacco-0.0.1-SNAPSHOT.jar
```

**Verify**:
- ✅ Server starts on http://localhost:9090
- ✅ Database connection successful
- ✅ No errors in console

### 2. Frontend Server
```bash
# Start frontend (port 3000)
cd minetsacco-main
npm run dev
# OR
npm start
```

**Verify**:
- ✅ Dev server starts on http://localhost:3000
- ✅ No compilation errors
- ✅ No TypeScript errors

### 3. Login as TREASURER
- **URL**: http://localhost:3000/login
- **Credentials**: 
  - Username: `treasurer` or appropriate treasurer account
  - Password: (your treasurer password)

---

## Test Suite 1: Top-Up Section Visibility

### TC1.1: Top-Up Section Appears Above Repayment Progress
**Steps**:
1. Login as TREASURER
2. Navigate to Loans page
3. Find Loan 366 (Mr Katee Mutunga)
4. Click Eye icon to view loan details

**Expected Result**:
- ✅ Loan details dialog opens
- ✅ Top-up section visible with purple/indigo theme
- ✅ Top-up section positioned ABOVE "Repayment Progress" section
- ✅ Section title shows "💰 Loan Top-Up"
- ✅ "Add Top-Up" button visible (purple)

**Actual Result**: _____________

---

### TC1.2: Top-Up History Display
**Steps**:
1. View Loan 366 details
2. Check top-up history section

**Expected Result** (if top-up exists):
- ✅ Shows "Top-Up History (1)" or similar count
- ✅ Displays top-up card with:
  - Amount: KES 50,000
  - Date: (when added)
  - Before: KES 138,635.69
  - After: KES 188,635.69
  - Purpose: (your test purpose)
  - Principal paid before: KES 190,661.31
- ✅ Card has purple theme

**Expected Result** (if no top-up):
- ✅ Shows message: "No top-ups yet. Click 'Add Top-Up' to add funds to this loan."

**Actual Result**: _____________

---

## Test Suite 2: Add Top-Up Functionality

### TC2.1: Open Top-Up Dialog
**Steps**:
1. View Loan 366 details
2. Click "+ Add Top-Up" button

**Expected Result**:
- ✅ Top-up dialog opens
- ✅ Dialog title: "Add Loan Top-Up"
- ✅ Shows loan summary:
  - Loan: LN-2026-00002
  - Member: Mr Katee Mutunga
  - Original Principal: KES 329,297
  - Current Outstanding: KES 138,635.69 (or current value)

**Actual Result**: _____________

---

### TC2.2: Top-Up Amount Input with Preview
**Steps**:
1. In top-up dialog, enter amount: `25000`
2. Wait for preview to load

**Expected Result**:
- ✅ Preview section appears with blue theme
- ✅ Shows:
  - Current Outstanding: KES 138,635.69
  - Top-Up Amount: +KES 25,000
  - New Outstanding: KES 163,635.69
  - Principal Paid So Far: KES 190,661.31 (preserved!)
- ✅ Message: "✓ This top-up will be added to the loan. Previous payments are preserved."

**Actual Result**: _____________

---

### TC2.3: Submit Top-Up
**Steps**:
1. Enter amount: `25000`
2. Enter purpose: "Additional business capital"
3. Click "Add Top-Up" button

**Expected Result**:
- ✅ Loading state shows "Adding..."
- ✅ Success toast: "Loan top-up added successfully"
- ✅ Dialog closes
- ✅ Loan details refresh
- ✅ New top-up appears in history
- ✅ Outstanding balance updated to KES 163,635.69
- ✅ Principal repaid still shows KES 190,661.31 (preserved!)

**Actual Result**: _____________

---

### TC2.4: Verify Calculation Accuracy
**Steps**:
1. After adding KES 25,000 top-up
2. Check "Repayment Progress" section

**Expected Result**:
- ✅ Principal: KES 329,297 (original, unchanged)
- ✅ Outstanding: KES 163,635.69 (138,635.69 + 25,000)
- ✅ Principal Repaid: KES 190,661.31 (PRESERVED from before)
- ✅ Repayment Percentage: 57.90% (based on original principal)
- ✅ Total Repaid: KES 206,021.91 (principal repaid + interest collected)

**Actual Result**: _____________

---

## Test Suite 3: Delete Button Functionality

### TC3.1: Delete Button Visible
**Steps**:
1. Login as TREASURER
2. Navigate to Loans page
3. Find any loan row in the table

**Expected Result**:
- ✅ Delete button (Trash2 icon) visible in actions column
- ✅ Icon color: red
- ✅ Tooltip shows "Delete Loan" on hover
- ✅ Button appears after Edit button

**Actual Result**: _____________

---

### TC3.2: Delete Button Hidden for Non-Treasurer
**Steps**:
1. Logout
2. Login as LOAN_OFFICER or MEMBER
3. Navigate to Loans page

**Expected Result**:
- ✅ Delete button NOT visible
- ✅ Only Eye and View Guarantors buttons visible

**Actual Result**: _____________

---

### TC3.3: Open Delete Dialog
**Steps**:
1. Login as TREASURER
2. Click delete icon on any test loan (NOT Loan 366!)

**Expected Result**:
- ✅ Delete dialog opens
- ✅ Title: "⚠️ Delete Loan" (red)
- ✅ Shows loan details in red theme
- ✅ Warning banner: "This action cannot be undone. All loan data, guarantors, and repayment history will be permanently deleted."
- ✅ Reason textarea with placeholder
- ✅ "Confirm Delete" button (red, disabled if no reason)

**Actual Result**: _____________

---

### TC3.4: Delete Loan (Use Test Loan Only!)
**Steps**:
1. Open delete dialog for a TEST loan
2. Enter reason: "Testing delete functionality"
3. Click "Confirm Delete"

**Expected Result**:
- ✅ Loading state: "Deleting..."
- ✅ Success toast: "Loan deleted successfully"
- ✅ Dialog closes
- ✅ Loan removed from table
- ✅ Page refreshes

**⚠️ WARNING**: Only test on non-production loans!

**Actual Result**: _____________

---

## Test Suite 4: Phase A Edit Restrictions

### TC4.1: Phase A Edit Section Visible
**Steps**:
1. Login as TREASURER
2. View Loan 366 details
3. Scroll to Phase A edit section

**Expected Result**:
- ✅ Section visible with indigo theme
- ✅ Title: "📝 Phase A: Edit Loan Fields"
- ✅ Subtitle: "(No guarantor changes)"
- ✅ "Edit Loan Fields (Phase A)" button visible

**Actual Result**: _____________

---

### TC4.2: Phase A Fields Are Editable
**Steps**:
1. Click "Edit Loan Fields (Phase A)"
2. Check available fields

**Expected Result**:
- ✅ Loan Status (dropdown)
- ✅ Disbursement Date (date picker)
- ✅ Interest Rate (number input)
- ✅ Outstanding Balance (number input)
- ✅ Interest Collected (only if migrated loan)
- ✅ Purpose (textarea)
- ✅ Alert banner: "✅ This form only sends Phase A fields. No guarantor data will be included in the request."

**Actual Result**: _____________

---

### TC4.3: Update Phase A Fields
**Steps**:
1. Edit any Phase A field (e.g., Purpose)
2. Enter new purpose: "Updated for testing"
3. Click "Update Loan Fields"

**Expected Result**:
- ✅ Loading state: "Updating..."
- ✅ Success toast: "Loan fields updated successfully (Phase A - No guarantor data sent)"
- ✅ Form closes
- ✅ Loan details refresh with new values
- ✅ Guarantors NOT affected

**Actual Result**: _____________

---

### TC4.4: Guarantors NOT Editable in Phase A
**Steps**:
1. Open Phase A edit form
2. Look for guarantor fields

**Expected Result**:
- ✅ NO guarantor fields visible
- ✅ NO guarantor selection dropdowns
- ✅ NO guarantor amount inputs
- ✅ Guarantors section only visible in separate "Edit Loan" dialog (full edit)

**Actual Result**: _____________

---

## Test Suite 5: Role-Based Access Control

### TC5.1: TREASURER Access
**Login**: TREASURER
**Expected**:
- ✅ Can see top-up section
- ✅ Can add top-ups
- ✅ Can delete loans
- ✅ Can edit Phase A fields
- ✅ Can edit full loan (existing)

**Actual Result**: _____________

---

### TC5.2: LOAN_OFFICER Access
**Login**: LOAN_OFFICER
**Expected**:
- ❌ Cannot see top-up section
- ❌ Cannot delete loans
- ❌ Cannot edit Phase A fields
- ✅ Can view loan details (read-only)
- ✅ Can create new loans

**Actual Result**: _____________

---

### TC5.3: MEMBER Access
**Login**: MEMBER
**Expected**:
- ❌ Cannot see top-up section
- ❌ Cannot delete loans
- ❌ Cannot edit Phase A fields
- ✅ Can view own loan details (read-only)

**Actual Result**: _____________

---

## Test Suite 6: API Integration

### TC6.1: Top-Up Preview API
**Endpoint**: GET `/api/loans/366/topup-preview?amount=25000`
**Expected Response**:
```json
{
  "data": {
    "currentOutstanding": 138635.69,
    "topupAmount": 25000,
    "newOutstanding": 163635.69,
    "principalPaidBeforeTopup": 190661.31
  }
}
```

**Actual Result**: _____________

---

### TC6.2: Add Top-Up API
**Endpoint**: POST `/api/loans/366/add-topup`
**Body**:
```json
{
  "topupAmount": 25000,
  "purpose": "Additional business capital",
  "newGuarantors": null
}
```

**Expected Response**: 200 OK with success message

**Actual Result**: _____________

---

### TC6.3: Top-Up History API
**Endpoint**: GET `/api/loans/366/topup-history`
**Expected Response**:
```json
{
  "data": [
    {
      "id": 1,
      "topupAmount": 50000,
      "topupDate": "2026-07-28T...",
      "outstandingBeforeTopup": 138635.69,
      "outstandingAfterTopup": 188635.69,
      "principalPaidBeforeTopup": 190661.31,
      "purpose": "Test top-up"
    }
  ]
}
```

**Actual Result**: _____________

---

### TC6.4: Delete Loan API
**Endpoint**: DELETE `/api/loans/{testLoanId}`
**Body**:
```json
{
  "reason": "Testing delete functionality"
}
```

**Expected Response**: 200 OK

**⚠️ WARNING**: Only test on non-production loans!

**Actual Result**: _____________

---

## Test Suite 7: Edge Cases

### TC7.1: Top-Up with Zero Amount
**Steps**:
1. Open top-up dialog
2. Leave amount empty or enter 0
3. Try to submit

**Expected Result**:
- ✅ "Add Top-Up" button disabled
- ✅ No preview shown

**Actual Result**: _____________

---

### TC7.2: Delete Without Reason
**Steps**:
1. Open delete dialog
2. Leave reason empty
3. Try to click "Confirm Delete"

**Expected Result**:
- ✅ "Confirm Delete" button disabled
- ✅ No deletion occurs

**Actual Result**: _____________

---

### TC7.3: Phase A Edit Without Changes
**Steps**:
1. Open Phase A edit form
2. Don't change anything
3. Try to submit

**Expected Result**:
- ✅ Error message: "At least one field must be updated"
- ✅ Form stays open

**Actual Result**: _____________

---

## Regression Tests

### RT1: Existing Loan Creation Still Works
**Steps**:
1. Click "New Loan Application"
2. Fill form completely
3. Submit

**Expected Result**:
- ✅ Loan created successfully
- ✅ No errors

**Actual Result**: _____________

---

### RT2: Existing Loan Editing Still Works
**Steps**:
1. Find a DISBURSED loan
2. Click Edit icon
3. Make changes
4. Save

**Expected Result**:
- ✅ Loan updated successfully
- ✅ No interference with top-up feature

**Actual Result**: _____________

---

### RT3: Loan Approval Flow Still Works
**Steps**:
1. Login as CREDIT_COMMITTEE
2. Find PENDING loan
3. Approve it

**Expected Result**:
- ✅ Loan approved successfully
- ✅ No errors

**Actual Result**: _____________

---

## Sign-Off

### Functional Testing
- [ ] All test cases passed
- [ ] No blocking issues found
- [ ] Edge cases handled correctly

**Tester**: ________________
**Date**: ________________

### User Acceptance Testing
- [ ] Top-up section visible and functional
- [ ] Delete button visible and functional
- [ ] Phase A restrictions verified
- [ ] Role-based access working correctly

**User**: ________________
**Date**: ________________

### Production Readiness
- [ ] Code reviewed
- [ ] Documentation complete
- [ ] Backend APIs tested
- [ ] Frontend tested
- [ ] No console errors
- [ ] Ready for deployment

**Developer**: ________________
**Date**: ________________

---

## Notes / Issues Found

_____________________________________________
_____________________________________________
_____________________________________________

---

**Test Document Version**: 1.0
**Created**: 2026-07-28
**Last Updated**: 2026-07-28
