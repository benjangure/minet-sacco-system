# GUARANTOR PLEDGE AMOUNT FREEZING - EXACT TRACE AND FIX

## STEP 1: EXACT CALL PATH TRACE

### Frontend Call Flow
**File:** `minetsacco-main/src/pages/Loans.tsx` (lines 717-813)
**Function:** `handleEditLoan()`

**Exact JSON Request Payload Sent to Backend:**
```json
{
  "disbursementDate": null,
  "outstandingBalance": 75000,
  "guarantorshipType": "NORMAL",
  "guarantors": [
    { "employeeId": "EMP001", "pledgeAmount": 37500 },
    { "employeeId": "EMP002", "pledgeAmount": 37500 }
  ]
}
```
**Key:** Lines 757-793 — The payload is constructed with user-entered `pledgeAmount` values (no ratio applied here).

---

### Backend Controller
**File:** `backend/src/main/java/com/minet/sacco/controller/LoanController.java` (line 728)
**Endpoint:** `PUT /loans/{loanId}/update`
```java
public ResponseEntity<ApiResponse<Map<String, Object>>> updateLoan(
    @PathVariable Long loanId,
    @RequestBody LoanUpdateRequestDTO updateRequest,
    Authentication authentication)
```
**Action:** Line 740 — Calls `loanService.updateLoan(loanId, updateRequest, user)`

---

### Backend Service - The Root Cause
**File:** `backend/src/main/java/com/minet/sacco/service/LoanService.java` (lines 1363-1540)
**Method:** `public Loan updateLoan(Long loanId, LoanUpdateRequestDTO updateRequest, User updatedBy)`

#### THE PROBLEM - Lines 1470-1530 (Guarantor Update Block)

**Lines 1468-1475:** Delete ALL old guarantors
```java
List<Guarantor> oldGuarantors = guarantorRepository.findByLoanId(loanId);
for (Guarantor oldGuarantor : oldGuarantors) {
    // Unfreeze savings
    Optional<Account> oldGuarantorAccount = accountRepository.findByMemberIdAndAccountType(
        oldGuarantor.getMember().getId(), Account.AccountType.SAVINGS);
    if (oldGuarantorAccount.isPresent() && oldGuarantor.getPledgeAmount() != null) {
        Account account = oldGuarantorAccount.get();
        account.setFrozenSavings(account.getFrozenSavings().subtract(oldGuarantor.getPledgeAmount()));
        accountRepository.save(account);
    }
    guarantorRepository.delete(oldGuarantor);  // <-- DELETES ALL
}
```

**Lines 1483-1530:** Recreate ALL guarantors from scratch
```java
for (int i = 0; i < updateRequest.getGuarantors().size(); i++) {
    com.minet.sacco.dto.LoanUpdateRequestDTO.GuarantorPairDTO guarantorPair = updateRequest.getGuarantors().get(i);
    Member guarantorMember = newGuarantorMembers.get(i);
    
    // Line 1509 - Gets user-entered pledge amount
    BigDecimal pledgeToFreeze = guarantorPair.getPledgeAmount();
    
    Guarantor newGuarantor = new Guarantor();
    // ... setup code ...
    // Line 1519 - Saves with the exact amount (NO RATIO APPLIED HERE)
    newGuarantor.setPledgeAmount(loan.getStatus().equals(Loan.Status.DISBURSED) ? 
        pledgeToFreeze : BigDecimal.ZERO);
    // Line 1520 - Sets the flag correctly
    newGuarantor.setPledgeFrozenAtFullAmount(true);
    // ... more setup ...
    guarantorRepository.save(newGuarantor);  // <-- CREATES NEW WITH FULL AMOUNT
    
    // Lines 1526-1530 - Freeze savings with the exact amount
    if (loan.getStatus().equals(Loan.Status.DISBURSED)) {
        Optional<Account> guarantorAccount = accountRepository.findByMemberIdAndAccountType(...);
        if (guarantorAccount.isPresent()) {
            Account account = guarantorAccount.get();
            account.setFrozenSavings(account.getFrozenSavings().add(pledgeToFreeze));
            accountRepository.save(account);
        }
    }
}
```

---

## WHY EDITING ONE GUARANTOR AFFECTS THE OTHER

**Root Cause:** Lines 1468-1475 and 1483-1530

When you submit guarantor changes:
1. **All existing guarantors are deleted** (line 1475: `guarantorRepository.delete(oldGuarantor)`)
2. **All their frozen savings are unfrozen** (lines 1472-1474)
3. **All guarantors are recreated** as new records (line 1526: `guarantorRepository.save(newGuarantor)`)
4. **New frozen amounts are applied** (lines 1527-1530)

**Result:** If you edit guarantor A, guarantor B is DELETED and RECREATED. This is why you see:
- Guarantor B's `id` changes
- Guarantor B's `created_at` changes  
- Guarantor B's frozen amount changes (because it's an all-or-nothing replacement)
- Even untouched guarantors are affected

---

## WHY THE 28,125 ISN'T HAPPENING AT SAVE (Investigation Result)

Actually, the code at line 1519 **CORRECTLY saves the exact amount with NO ratio applied**. The 28,125 value you reported earlier suggests:

1. **Either** it's being applied during loan repayment via `GuarantorTrackingService.trackPledgeReduction()` (lines 117-122), which checks the `pledgeFrozenAtFullAmount` flag
2. **Or** it was already wrong in historical data before the V137 migration

The `pledgeFrozenAtFullAmount` flag IS being set to `true` at line 1520, so during repayment it **should** skip the ratio. But you reported it still applies the ratio, so we need to verify the flow isn't broken.

---

## STEP 2: FIX ACCORDING TO EXACT RULES

### Rule 1 ✓ ALREADY CORRECT
Lines 1509-1519 correctly save the exact amount with no ratio applied at save time.

### Rule 2 ✗ NEEDS FIX
**Problem:** All guarantors deleted and recreated on every save (lines 1468-1530)
**Solution:** Update only the specific guarantor records that changed. Keep unchanged guarantors intact.

### Rule 3 ✓ ALREADY CORRECT
`GuarantorTrackingService.trackPledgeReduction()` is only called during repayment (not at edit time).

### Rule 4 ⚠️ NEEDS CHECK
Need to identify if any existing guarantor records have wrong pledge amounts from this bug.

---

## THE FIX - IMPLEMENT TARGETED GUARANTOR UPDATES

Replace lines 1468-1530 in `LoanService.updateLoan()` with logic that:
1. Compare new guarantors list with existing guarantors
2. Delete only guarantors no longer in the list
3. Update guarantors whose pledge amounts changed
4. Create only new guarantors being added
5. Leave untouched guarantors completely undisturbed

---

## SQL VERIFICATION QUERY (Step 4)

To check if historical data was corrupted:

```sql
-- Find all guarantor records with mismatched pledge_frozen_at_full_amount flag
SELECT 
    g.id,
    g.loan_id,
    g.member_id,
    g.guarantee_amount,
    g.pledge_amount,
    g.pledge_frozen_at_full_amount,
    l.loan_number,
    l.amount as original_principal,
    l.outstanding_balance
FROM guarantors g
JOIN loans l ON g.loan_id = l.id
WHERE g.pledge_frozen_at_full_amount = true
  AND l.status = 'DISBURSED'
  AND g.pledge_amount < (g.guarantee_amount * 0.95)  -- Off by more than 5%
ORDER BY g.loan_id;
```

