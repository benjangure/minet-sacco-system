# Treasurer Loan Delete Functionality Guide

## Overview
The Treasurer has the ability to **permanently delete loans** from the system. This is a powerful administrative action that should be used carefully, as it affects multiple system components.

---

## How It Works

### 1. **Access Control**
Only users with the `ROLE_TREASURER` role can delete loans.

**Frontend Check:**
```typescript
const canEditDeleteLoans = role === "TREASURER";
```

**Backend Check:**
```java
@DeleteMapping("/{loanId}")
@PreAuthorize("hasRole('ROLE_TREASURER')")
public ResponseEntity<ApiResponse<String>> deleteLoan(...)
```

---

### 2. **User Interface**

#### **Loans Page - Delete Button**
- Navigate to: **Loans** page
- Each loan row shows:
  - 👁️ View button (all roles)
  - 💰 Disburse button (Treasurer only, for approved loans)
  - ✏️ **Edit button** (Treasurer only) - Blue icon
  - 🗑️ **Delete button** (Treasurer only) - Red trash icon

#### **Delete Process:**
1. Click the **red Trash2 icon** next to any loan
2. A confirmation dialog appears asking you to confirm deletion
3. Click "Delete" to proceed
4. System validates if deletion is allowed
5. Success or error message is displayed

---

### 3. **Business Logic - What Happens When You Delete**

When you delete a loan, the system performs these actions **in order**:

#### **Step 1: Validation - Check if Deletion is Allowed**
The system checks if the loan has repayments:

```java
if (loan.getStatus() == Loan.Status.DISBURSED) {
    BigDecimal totalRepaid = loanRepaymentRepository.getTotalRepaidAmount(loanId);
    if (totalRepaid != null && totalRepaid.compareTo(BigDecimal.ZERO) > 0) {
        throw new RuntimeException("Cannot delete loan with existing repayments. Total repaid: KES " + totalRepaid);
    }
}
```

**❌ Deletion is BLOCKED if:**
- Loan status is `DISBURSED` (or any status)
- Loan has any repayments recorded (total repaid > 0)

**✅ Deletion is ALLOWED if:**
- Loan has no repayments
- Loan is in any status (PENDING, APPROVED, DISBURSED, REJECTED, etc.)

#### **Step 2: Release Guarantor Pledges**
For each guarantor of the loan:
- Unfreeze their pledged savings amount
- Reduce their `frozenSavings` by the pledge amount
- Send notification to guarantor about loan deletion
- Example: If guarantor pledged KES 50,000, their frozen savings is reduced by KES 50,000

#### **Step 3: Delete Guarantor Records**
All guarantor records linked to this loan are permanently removed from the database.

#### **Step 4: Delete Loan Repayments**
All repayment records for this loan are deleted (this should only happen for loans with zero repayments due to Step 1 validation).

#### **Step 5: Reverse Disbursement Transaction**
If the loan was disbursed:
- Find the disbursement transaction in member's savings account
- Deduct the loan amount from member's current balance
- Check if member has sufficient balance for reversal
- If balance is insufficient, deletion is **blocked** with error message
- Delete all transactions related to this loan

**Example:**
- Loan amount: KES 100,000
- Member's current balance: KES 150,000
- After reversal: KES 50,000
- If balance was only KES 80,000, deletion would fail

#### **Step 6: Notify Member**
Send notification to the loan member informing them that their loan has been deleted by the Treasurer.

#### **Step 7: Audit Logging**
Log the deletion action with:
- User who performed deletion (Treasurer)
- Action: "DELETE"
- Entity: "LOAN"
- Loan details: Loan number, member name, amount, status

#### **Step 8: Delete the Loan**
Finally, the loan record is permanently removed from the database.

---

## Error Messages

### **Error 1: Loan Has Repayments**
```
⚠ Cannot Delete Loan with Repayments

Cannot delete loan with existing repayments. Total repaid: KES 25,000

This loan cannot be deleted because it has existing repayment records. 
Only loans without any repayments can be deleted.

💡 Alternative: Consider marking the loan as "Written Off" or adjusting 
the outstanding balance instead.
```

**When this occurs:**
- Loan has at least one repayment recorded
- System displays total amount repaid
- Shows alternative actions (Write Off, Edit Outstanding Balance)
- Error displayed for 10 seconds with multi-line helpful message

### **Error 2: Insufficient Balance for Reversal**
```
Cannot delete loan: Member account has insufficient balance to reverse 
disbursement. Current balance: KES 30,000, Loan amount: KES 100,000
```

**When this occurs:**
- Loan was disbursed
- Member has spent the disbursed amount
- Current balance is less than the loan amount
- Deletion would result in negative balance

### **Error 3: Network/Permission Error**
```
Network error: Unable to delete loan. Please check your connection 
and try again.
```

**When this occurs:**
- Server is unreachable
- Authentication token expired
- User doesn't have TREASURER role

---

## Success Message

```
✓ Loan Deleted Successfully

Loan #L-2024-001 has been permanently removed from the system.
```

Displayed for 5 seconds after successful deletion.

---

## Use Cases

### **Valid Use Cases for Deletion:**

1. **Duplicate Loan Applications**
   - Member accidentally applied twice
   - Status: PENDING or APPROVED
   - No disbursement made yet

2. **Test Loans**
   - Created during testing/training
   - Never disbursed or no repayments made

3. **Erroneous Loan Records**
   - Incorrect data entered
   - Loan approved but not yet disbursed
   - Need to re-create with correct information

4. **Cancelled Before Disbursement**
   - Loan approved but member no longer needs it
   - Better to delete than reject (if no business need to keep record)

### **Invalid Use Cases (Should NOT Delete):**

1. **Active Loans with Repayments**
   - System will block this automatically
   - Use "Write Off" status instead

2. **Fully Paid Loans**
   - Keep for historical records
   - Use reports to filter out completed loans

3. **Defaulted Loans**
   - Keep for audit and reporting purposes
   - Mark as "WRITTEN_OFF" if needed

---

## Alternatives to Deletion

If you cannot delete a loan (has repayments), consider these alternatives:

### **1. Edit Outstanding Balance**
- Use the **Edit button** (✏️ blue icon)
- Adjust the outstanding balance to zero
- System recalculates totals

### **2. Mark as Written Off**
- Change loan status to `WRITTEN_OFF`
- Loan is excluded from active reports
- Historical record is preserved

### **3. Keep for Audit**
- Leave loan as-is
- Use reporting filters to exclude from analysis
- Maintain complete audit trail

---

## Security & Permissions

### **Who Can Delete:**
- ✅ Treasurer (ROLE_TREASURER)

### **Who CANNOT Delete:**
- ❌ Loan Officer
- ❌ Credit Committee
- ❌ Teller
- ❌ Customer Support
- ❌ Admin (unless also has Treasurer role)
- ❌ Member

### **Backend Enforcement:**
```java
@PreAuthorize("hasRole('ROLE_TREASURER')")
```

### **Frontend Enforcement:**
```typescript
const canEditDeleteLoans = role === "TREASURER";
```

---

## Database Impact

### **Tables Affected:**
1. **loans** - Loan record deleted
2. **guarantors** - All guarantor records deleted
3. **loan_repayments** - All repayment records deleted (should be zero)
4. **transactions** - Disbursement and repayment transactions deleted
5. **accounts** - Member and guarantor balances adjusted
6. **notifications** - New notifications created for member and guarantors
7. **audit_logs** - Deletion action logged

### **Data Integrity:**
- All foreign key constraints are handled
- Balances are recalculated correctly
- No orphaned records are left
- Transaction is atomic (all-or-nothing)

---

## API Endpoint

**DELETE** `/api/loans/{loanId}`

**Authorization:** `Bearer {token}` with `ROLE_TREASURER`

**Request:**
```http
DELETE /api/loans/123
Authorization: Bearer eyJhbGc...
```

**Success Response (200 OK):**
```json
{
  "success": true,
  "message": "Loan deleted successfully",
  "data": "Loan has been removed from the system"
}
```

**Error Response (400 Bad Request):**
```json
{
  "success": false,
  "message": "Cannot delete loan with existing repayments. Total repaid: KES 25000",
  "data": null
}
```

---

## Testing

### **Test Case 1: Delete Pending Loan**
1. Create new loan application
2. Don't approve or disburse
3. As Treasurer, click delete button
4. ✅ Should succeed

### **Test Case 2: Delete Approved But Not Disbursed**
1. Create loan and approve it
2. Don't disburse
3. As Treasurer, click delete button
4. ✅ Should succeed
5. Verify guarantor pledges released

### **Test Case 3: Delete Disbursed Loan Without Repayments**
1. Create, approve, and disburse loan
2. Don't make any repayments
3. Verify member has sufficient balance
4. As Treasurer, click delete button
5. ✅ Should succeed
6. Verify member balance reduced by loan amount

### **Test Case 4: Try to Delete Loan With Repayments**
1. Create, approve, and disburse loan
2. Make at least one repayment
3. As Treasurer, click delete button
4. ❌ Should fail with multi-line error message
5. Error should show total repaid amount

### **Test Case 5: Permission Check**
1. Login as Loan Officer
2. Go to Loans page
3. ❌ Delete button should NOT appear
4. Attempting API call directly should return 403 Forbidden

---

## Best Practices

1. **✅ DO:**
   - Verify loan details before deletion
   - Check why loan needs to be deleted
   - Document reason in audit system
   - Inform stakeholders before deleting important loans

2. **❌ DON'T:**
   - Delete loans with repayment history
   - Delete loans for record-keeping purposes (use Write Off instead)
   - Delete without checking member and guarantor impact
   - Use deletion as a shortcut for status changes

3. **⚠️ CAUTION:**
   - Deletion is permanent and cannot be undone
   - Affects member and guarantor balances
   - Impacts financial reports
   - Audit trail is preserved but loan data is gone

---

## Summary

The Treasurer can delete loans through:
- **UI:** Red trash icon (🗑️) on Loans page
- **API:** DELETE /api/loans/{loanId}
- **Restriction:** Only loans without repayments
- **Effect:** Permanent removal with full cleanup (guarantors, transactions, balances)
- **Safety:** Multi-step validation and comprehensive error messages
- **Audit:** All actions logged for compliance

