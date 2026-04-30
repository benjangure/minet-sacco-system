# Guarantor Reassignment on Loan Amount Change

## The Issue

**Current Behavior (WRONG):**
```
Initial Loan: 10,000 KES
├─ Brian: 5,000 (PENDING)
└─ Amina: 5,000 (REJECTED)

Samuel reduces to: 8,000 KES
├─ Brian: 5,000 (PENDING) ← UNCHANGED
└─ Amina: 5,000 (REJECTED) ← UNCHANGED
```

**Problem:** The guarantor amounts are still 5,000 each, but the loan is now 8,000. This creates confusion:
- Is Brian still guaranteeing 5,000 for an 8,000 loan?
- What about the remaining 3,000?
- Does Amina need to re-guarantee?

---

## The Correct Behavior

**What Should Happen (CORRECT):**
```
Initial Loan: 10,000 KES
├─ Brian: 5,000 (PENDING)
└─ Amina: 5,000 (REJECTED)

Samuel reduces to: 8,000 KES
    ↓
GUARANTOR REASSIGNMENT TRIGGERED
    ↓
All guarantor assignments CLEARED
├─ Brian: 0 (RESET)
└─ Amina: 0 (RESET)
    ↓
Samuel must RE-ASSIGN guarantors for 8,000 KES
├─ Brian: ? (needs to re-pledge)
└─ Amina: ? (needs to re-pledge)
    ↓
New guarantor approval workflow starts
```

---

## Business Logic

### When Loan Amount Changes

**Trigger:** Member reduces loan amount

**Action:**
1. **Clear all guarantor assignments**
   - Set all guarantor amounts to 0
   - Set all guarantor statuses to RESET or PENDING_REASSIGNMENT
   - Mark guarantors as needing re-approval

2. **Notify all guarantors**
   - "Loan amount changed from 10,000 to 8,000"
   - "Your previous guarantee of 5,000 is no longer valid"
   - "Please re-assign your guarantee amount for the new loan"

3. **Member must re-assign guarantors**
   - Member selects guarantors again
   - Member specifies new guarantee amounts
   - Total guarantees must cover new loan amount (8,000)

4. **Guarantors re-approve**
   - Each guarantor receives new approval request
   - Guarantor can accept, reduce, or reject
   - System waits for all guarantor responses

5. **Credit Committee reviews**
   - Reviews loan with NEW guarantor assignments
   - Ensures total guarantees cover new amount
   - Approves or rejects

---

## Example Workflow

### Scenario: Samuel Reduces 10k to 8k

**Step 1: Initial State**
```
Loan: 10,000 KES
├─ Brian: 5,000 (PENDING)
└─ Amina: 5,000 (REJECTED)
```

**Step 2: Samuel Requests Reduction**
```
New Amount: 8,000 KES
Reason: "Guarantor rejected"
```

**Step 3: System Clears Guarantors**
```
Loan: 8,000 KES
├─ Brian: 0 (RESET)
└─ Amina: 0 (RESET)
Status: PENDING_GUARANTOR_REASSIGNMENT
```

**Step 4: Notifications Sent**
```
To Brian:
"Loan amount changed to 8,000 KES. Your previous 5,000 guarantee is no longer valid.
Please re-assign your guarantee amount."

To Amina:
"Loan amount changed to 8,000 KES. Your previous 5,000 guarantee is no longer valid.
Please re-assign your guarantee amount."

To Samuel:
"Loan reduced to 8,000 KES. Please re-assign guarantor amounts."
```

**Step 5: Samuel Re-assigns Guarantors**
```
Option A: Same guarantors, different amounts
├─ Brian: 4,000 (new pledge)
└─ Amina: 4,000 (new pledge)
Total: 8,000 ✓

Option B: Different guarantors
├─ Brian: 8,000 (full guarantee)
└─ Amina: 0 (removed)
Total: 8,000 ✓

Option C: More guarantors
├─ Brian: 3,000
├─ Amina: 3,000
└─ David: 2,000
Total: 8,000 ✓
```

**Step 6: Guarantors Re-approve**
```
Brian receives: "Please approve guarantee of 4,000 for 8,000 loan"
├─ ACCEPT: "I pledge 4,000"
├─ REDUCE: "I'll only pledge 2,000"
└─ REJECT: "I can't guarantee"

Amina receives: "Please approve guarantee of 4,000 for 8,000 loan"
├─ ACCEPT: "I pledge 4,000"
├─ REDUCE: "I'll only pledge 2,000"
└─ REJECT: "I can't guarantee"
```

**Step 7: Credit Committee Reviews**
```
Loan: 8,000 KES
├─ Brian: 4,000 (ACCEPTED)
└─ Amina: 4,000 (ACCEPTED)
Total Guarantee: 8,000 ✓
Status: PENDING_CREDIT_COMMITTEE
```

**Step 8: Credit Committee Approves**
```
Loan: 8,000 KES
├─ Brian: 4,000 (ACCEPTED)
└─ Amina: 4,000 (ACCEPTED)
Status: PENDING_TREASURER
```

---

## Implementation Requirements

### Database Changes
```sql
-- Add new status for guarantors
ALTER TABLE guarantors ADD COLUMN previous_guarantee_amount DECIMAL(19,2);
ALTER TABLE guarantors ADD COLUMN reassignment_reason VARCHAR(255);

-- Track guarantee history
CREATE TABLE guarantee_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guarantor_id BIGINT,
    loan_id BIGINT,
    old_amount DECIMAL(19,2),
    new_amount DECIMAL(19,2),
    reason VARCHAR(255),
    changed_at TIMESTAMP,
    changed_by BIGINT,
    FOREIGN KEY (guarantor_id) REFERENCES guarantors(id),
    FOREIGN KEY (loan_id) REFERENCES loans(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
);
```

### Service Layer Changes
```java
// In LoanService.reduceLoanAmount()

// 1. Clear all guarantor assignments
List<Guarantor> guarantors = guarantorRepository.findByLoanId(loanId);
for (Guarantor g : guarantors) {
    g.setPreviousGuaranteeAmount(g.getGuaranteeAmount());
    g.setGuaranteeAmount(BigDecimal.ZERO);
    g.setStatus(Guarantor.Status.PENDING_REASSIGNMENT);
    g.setReassignmentReason("Loan amount reduced from " + originalAmount + " to " + newAmount);
    guarantorRepository.save(g);
    
    // Log in guarantee history
    guaranteeHistoryRepository.save(new GuaranteeHistory(
        g.getId(), loanId, g.getGuaranteeAmount(), BigDecimal.ZERO,
        "Loan amount reduced", LocalDateTime.now(), requestedBy.getId()
    ));
}

// 2. Update loan status
loan.setStatus(Loan.Status.PENDING_GUARANTOR_REASSIGNMENT);

// 3. Notify guarantors
for (Guarantor g : guarantors) {
    notificationService.notifyUser(g.getMember().getId(),
        "Loan amount changed from " + originalAmount + " to " + newAmount + ". " +
        "Your previous guarantee of " + g.getPreviousGuaranteeAmount() + " is no longer valid. " +
        "Please re-assign your guarantee amount.",
        "GUARANTOR_REASSIGNMENT", loanId, loan.getMember().getId(), "GUARANTOR_REASSIGNMENT");
}

// 4. Notify member
notificationService.notifyUser(memberUser.getId(),
    "Loan reduced to " + newAmount + ". Please re-assign guarantor amounts.",
    "LOAN_REDUCTION", loanId, loan.getMember().getId(), "LOAN_REDUCTION");
```

### Frontend Changes
```typescript
// New component: GuarantorReassignmentDialog
// Shows:
// 1. Old guarantor assignments (read-only)
// 2. New loan amount
// 3. Form to re-assign guarantors
// 4. Validation that total guarantees cover new amount

// New endpoint: POST /loans/{loanId}/reassign-guarantors
// Accepts:
// {
//   guarantors: [
//     { memberId: 1, guaranteeAmount: 4000 },
//     { memberId: 2, guaranteeAmount: 4000 }
//   ]
// }
```

---

## Loan Status Flow

### Current (WRONG)
```
PENDING_GUARANTOR_REPLACEMENT
    ↓ (Reduce Amount)
PENDING_CREDIT_COMMITTEE
```

### Correct (NEW)
```
PENDING_GUARANTOR_REPLACEMENT
    ↓ (Reduce Amount)
PENDING_GUARANTOR_REASSIGNMENT
    ↓ (Member re-assigns guarantors)
PENDING_GUARANTOR_APPROVAL
    ↓ (Guarantors approve new amounts)
PENDING_CREDIT_COMMITTEE
```

---

## Guarantor Status Flow

### Current (WRONG)
```
PENDING → ACCEPTED/REJECTED (unchanged after loan reduction)
```

### Correct (NEW)
```
PENDING → ACCEPTED/REJECTED
    ↓ (Loan amount changes)
PENDING_REASSIGNMENT
    ↓ (Member re-assigns)
PENDING (new approval request)
    ↓ (Guarantor responds)
ACCEPTED/REJECTED/REDUCED
```

---

## Audit Trail

### What Should Be Logged

```
Event: LOAN_AMOUNT_REDUCED
├─ Loan ID: 8
├─ Original Amount: 10,000
├─ New Amount: 8,000
├─ Reason: Guarantor rejected
├─ Guarantors Cleared:
│  ├─ Brian: 5,000 → 0
│  └─ Amina: 5,000 → 0
├─ Notifications Sent:
│  ├─ Brian: ✅ Notified
│  └─ Amina: ✅ Notified
├─ Timestamp: 2026-04-30 10:04:21
└─ Requested By: Samuel Ochieng

Event: GUARANTOR_REASSIGNED
├─ Loan ID: 8
├─ New Assignments:
│  ├─ Brian: 4,000
│  └─ Amina: 4,000
├─ Total Guarantee: 8,000
├─ Timestamp: 2026-04-30 10:15:00
└─ Requested By: Samuel Ochieng

Event: GUARANTOR_APPROVAL_REQUEST
├─ Guarantor: Brian
├─ Loan ID: 8
├─ Amount: 4,000
├─ Status: PENDING
├─ Timestamp: 2026-04-30 10:15:05
└─ Notification Sent: ✅ Yes

Event: GUARANTOR_APPROVAL_RESPONSE
├─ Guarantor: Brian
├─ Loan ID: 8
├─ Amount: 4,000
├─ Response: ACCEPTED
├─ Timestamp: 2026-04-30 10:20:00
└─ Responded By: Brian
```

---

## Summary

### What Needs to Change

1. **When loan amount is reduced:**
   - Clear all guarantor amounts (set to 0)
   - Mark guarantors as PENDING_REASSIGNMENT
   - Notify all guarantors

2. **Member must re-assign guarantors:**
   - Select guarantors again
   - Specify new guarantee amounts
   - Ensure total covers new loan amount

3. **Guarantors re-approve:**
   - Receive new approval requests
   - Can accept, reduce, or reject
   - System waits for responses

4. **Credit Committee reviews:**
   - Reviews with new guarantor assignments
   - Ensures total guarantees cover new amount

5. **Audit trail:**
   - Logs all guarantor changes
   - Tracks reassignment process
   - Records all notifications and responses

### Benefits

✅ Clear audit trail of guarantor changes
✅ Guarantors explicitly approve new amounts
✅ No confusion about guarantee amounts
✅ Compliant with SACCO best practices
✅ Transparent process for all parties

### Current Status

❌ **NOT IMPLEMENTED** - This is a business logic enhancement needed for full SACCO compliance.
