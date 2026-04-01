# Minet SACCO System Audit & Requirements Alignment

## Executive Summary

I understand the Kenyan SACCO workflow requirements precisely. I've audited the current system and identified what's working correctly and what needs to be fixed.

---

## 1. LOAN ELIGIBILITY (Savings-Based Only)

### Requirement
- Max loan = 3x savings balance (NOT total balance)
- Savings is the ONLY reference point
- Defaulted loans reduce eligibility

### Current Implementation Status: ✅ CORRECT
**File**: `backend/src/main/java/com/minet/sacco/service/LoanEligibilityValidator.java`

**What's Working**:
- Line 113: `BigDecimal totalBalance = savingsBalance;` - Only savings counted
- Line 117: `BigDecimal frozenPledges = guarantorRepository.sumActivePledgesByMemberId(member.getId());` - Frozen pledges subtracted
- Line 119: `BigDecimal availableBalance = totalBalance.subtract(frozenPledges);` - Correct calculation
- Checks for defaulted loans (Line 155-162)
- Checks minimum savings requirement (Line 165-170)

**Status**: No changes needed - this is correctly implemented.

---

## 2. GUARANTOR SYSTEM (Dynamic Freezing)

### Requirement
- Guarantor can guarantee multiple loans
- Each guarantee freezes that amount from their savings
- Available for guarantorship = savings - frozen amounts
- Guarantor's own loan eligibility = remaining unfrozen savings
- Guarantor can see if they're capable before approving

### Current Implementation Status: ✅ MOSTLY CORRECT

**File**: `backend/src/main/java/com/minet/sacco/service/GuarantorValidationService.java`

**What's Working**:
- Line 113: `BigDecimal totalBalance = savingsBalance;` - Only savings counted
- Line 130-135: Frozen pledges calculation
  ```java
  BigDecimal alreadyPledged = excludeLoanId != null
      ? guarantorRepository.sumActivePledgesByMemberIdExcludingLoan(guarantor.getId(), excludeLoanId)
      : guarantorRepository.sumActivePledgesByMemberId(guarantor.getId());
  BigDecimal availableCapacity = totalBalance.subtract(alreadyPledged);
  ```
- Guarantor can see available capacity before approving

**Status**: No changes needed - this is correctly implemented.

---

## 3. LOAN APPLICATION WORKFLOW (Member Portal)

### Requirement
1. Member applies → selects product → sets terms/amount
2. System shows max eligible amount in real-time
3. Member selects guarantor(s)
4. On submit → status = PENDING
5. Guarantor gets notification with loan details
6. Guarantor sees if they're capable
7. If approve → goes to Loan Officer
8. Loan Officer approves → goes to Credit Committee
9. Credit Committee approves → goes to Treasurer
10. Rejections handled at each stage with notifications

### Current Implementation Status: ⚠️ PARTIALLY IMPLEMENTED

**File**: `backend/src/main/java/com/minet/sacco/service/LoanService.java`

**What's Working**:
- Line 86-160: `applyForLoan()` method creates loan with status
- Line 157-158: Sets status to `PENDING_GUARANTOR_APPROVAL` if guarantors exist
- Line 175-178: Creates guarantor records with PENDING status
- Guarantor validation works correctly

**What's Missing/Needs Fixing**:

1. **Approval Chain Not Implemented**
   - Current: Only 2 statuses - PENDING and APPROVED
   - Needed: PENDING → LOAN_OFFICER → CREDIT_COMMITTEE → TREASURER → DISBURSED
   - **Action**: Need to add intermediate approval statuses and workflow

2. **Guarantor Approval Workflow**
   - Current: Guarantor status is PENDING but no approval endpoint
   - Needed: Guarantor can approve/reject with notification
   - **Action**: Need to implement guarantor approval endpoint

3. **Notifications Not Sent at Each Stage**
   - Current: No notifications sent to Loan Officer, Credit Committee, Treasurer
   - Needed: Notifications at each approval stage
   - **Action**: Add notification calls in approval chain

4. **Member Portal Doesn't Show Eligibility**
   - Current: No real-time eligibility display
   - Needed: Show max eligible amount before submission
   - **Action**: Add eligibility calculation endpoint for frontend

---

## 4. DEPOSIT VERIFICATION WORKFLOW

### Requirement
1. Member claims deposit → specifies amount → attaches receipt
2. Status = PENDING REVIEW
3. Teller gets notification
4. Teller views receipt document
5. Teller enters confirmed amount (what they see in receipt)
6. Optional: Teller adds notification message to member
7. On confirmation → account updated + member notified
8. If invalid → Teller rejects with notification to member

### Current Implementation Status: ⚠️ PARTIALLY IMPLEMENTED

**File**: `backend/src/main/java/com/minet/sacco/service/DepositRequestService.java`

**What's Working**:
- Line 35-52: `submitDepositRequest()` - Member submits with receipt
- Line 54-55: Notification sent to tellers
- Line 60-100: `approveDepositRequest()` - Teller confirms amount
- Line 102-105: Account balance updated
- Line 107-110: Member notified of approval
- Line 115-135: `rejectDepositRequest()` - Teller can reject
- Line 137-140: Member notified of rejection

**What's Missing/Needs Fixing**:

1. **Optional Teller Message Field**
   - Current: No field for teller to add custom message
   - Needed: `tellerMessage` or `tellerNotes` field in DepositRequest
   - **Action**: Add optional message field to DepositRequest entity and UI

2. **Notification Content**
   - Current: Generic messages
   - Needed: More detailed messages with teller notes if provided
   - **Action**: Update notification messages to include teller notes

3. **Frontend UI for Teller**
   - Current: No UI shown for teller to review deposits
   - Needed: Teller dashboard to view pending deposits with receipt
   - **Action**: Create teller deposit review component

**Status**: Core logic is correct, just needs UI and optional message field.

---

## SUMMARY OF ACTIONS NEEDED

### Priority 1 (Critical - Loan Workflow)
1. **Add Loan Approval Chain Statuses**
   - Add new Loan.Status values: PENDING_LOAN_OFFICER, PENDING_CREDIT_COMMITTEE, PENDING_TREASURER
   - Update LoanService to handle multi-stage approval

2. **Implement Guarantor Approval Endpoint**
   - Create endpoint for guarantor to approve/reject
   - Send notification to Loan Officer when guarantor approves

3. **Add Notifications at Each Stage**
   - Loan Officer notification when loan ready for review
   - Credit Committee notification when Loan Officer approves
   - Treasurer notification when Credit Committee approves

4. **Add Member Portal Eligibility Display**
   - Create endpoint to calculate max eligible amount
   - Show in real-time as member fills form

### Priority 2 (Important - Deposit Workflow)
1. **Add Teller Message Field**
   - Add `tellerMessage` field to DepositRequest entity
   - Update migration to add column

2. **Create Teller Deposit Review UI**
   - Dashboard showing pending deposits
   - Ability to view receipt
   - Form to enter confirmed amount + optional message

3. **Update Notification Messages**
   - Include teller message in member notifications

### Priority 3 (Nice to Have)
1. Audit trail for all approval stages
2. Rejection reason tracking at each stage
3. Loan application history for member

---

## NEXT STEPS

1. **Confirm** you want me to proceed with implementing the loan approval chain
2. **Specify** if you want all 4 approval stages (Loan Officer → Credit Committee → Treasurer) or a different chain
3. **Confirm** the teller message field for deposits
4. I'll create the necessary database migrations, backend endpoints, and frontend components

Would you like me to proceed with these implementations?
