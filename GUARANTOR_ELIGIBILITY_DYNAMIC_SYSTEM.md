# Guarantor Eligibility - Dynamic System Implementation

## Overview
The system now dynamically adjusts guarantor eligibility checks based on the loan type and guarantee structure.

## Three Loan Scenarios

### 1. Full Self-Guarantee Loan
- **Member Eligibility**: 3x savings (e.g., 70k savings = 210k eligibility)
- **Guarantor Check**: Only validates member's own savings
- **Workflow**: 
  - Member applies with self-guarantee
  - Loan status: `PENDING_LOAN_OFFICER_REVIEW` (skips guarantor approval)
  - No external notifications sent
  - Loan officer reviews and approves/rejects

### 2. Partial Self-Guarantee + External Guarantors
- **Member Eligibility**: 3x savings (e.g., 70k savings = 210k eligibility)
- **Member Self-Guarantee**: Validates member has savings for their portion (e.g., 50k)
- **External Guarantor Checks**: Each guarantor validated against their specific pledge amount (e.g., Mercy for 30k, not 100k)
- **Workflow**:
  - Member applies with mixed guarantors
  - Loan status: `PENDING_GUARANTOR_APPROVAL`
  - External guarantors receive notifications with specific amounts
  - Each guarantor's eligibility checked against their pledge amount only
  - Once all external guarantors approve → `PENDING_LOAN_OFFICER_REVIEW`
  - Loan officer reviews and approves/rejects

### 3. Normal Loan (External Guarantors Only)
- **Member Eligibility**: 3x savings (e.g., 70k savings = 210k eligibility)
- **Guarantor Checks**: Each guarantor validated against their specific pledge amount
- **Workflow**:
  - Member applies with external guarantors
  - Loan status: `PENDING_GUARANTOR_APPROVAL`
  - Guarantors receive notifications with specific amounts
  - Each guarantor's eligibility checked against their pledge amount only
  - Once all guarantors approve → `PENDING_LOAN_OFFICER_REVIEW`
  - Loan officer reviews and approves/rejects

## Guarantor Eligibility Validation

### Key Changes
1. **Guarantee Amount Parameter**: Backend now accepts `guaranteeAmount` query parameter
2. **Dynamic Validation**: Checks are performed against the specific guarantee amount, not the full loan amount
3. **Backward Compatible**: If no `guaranteeAmount` provided, defaults to full loan amount

### Validation Checks (Per Guarantor)
For each guarantor validating against their specific guarantee amount:

1. **Member Status**: Guarantor must be ACTIVE
2. **Minimum Savings**: Guarantor must have minimum savings (e.g., 10,000)
3. **Available Capacity**: `Available Capacity = Total Savings - Already Pledged`
   - Must be >= Guarantee Amount
   - Example: Mercy has 56k savings, no pledges, asked to guarantee 30k → ✓ Eligible
4. **Savings-to-Guarantee Ratio**: Savings >= 50% of Guarantee Amount
   - Example: Mercy has 56k, asked to guarantee 30k → 56k >= 15k → ✓ Eligible
5. **No Defaulted Loans**: Guarantor cannot have defaulted loans (unless allowed by rules)
6. **Active Guarantorships**: Warning if at max commitments
7. **Outstanding Balance**: Warning if exceeds threshold

### Error Messages
- **Insufficient Capacity**: "Insufficient guarantee capacity. Available: KES 56,000 (Total savings: KES 56,000 minus already pledged: KES 0). Required: KES 30,000"
- **Below Ratio**: "Savings balance (KES 56,000) should be at least 50% of guarantee amount (KES 15,000)"

## Frontend Implementation

### GuarantorApprovalModal
- Extracts `guaranteeAmount` from guarantor request object
- Passes it as query parameter to eligibility endpoint
- Displays specific guarantee amount prominently
- Shows "Partial guarantee" indicator when applicable
- Error messages reference the specific guarantee amount, not full loan

### Example Flow
```
Loan Application: 100k
- Kevin (Member): Self-guarantees 50k
- Mercy (Guarantor): Guarantees 30k
- George (Guarantor): Guarantees 20k

Mercy's Eligibility Check:
- Endpoint: /member/guarantor-eligibility/{mercyId}/100000?guaranteeAmount=30000
- Validates: Can Mercy guarantee 30k? (not 100k)
- Result: ✓ Yes (56k savings >= 30k needed)
```

## Backend Implementation

### MemberPortalController.checkGuarantorEligibility()
```java
// Uses guaranteeAmount if provided, otherwise uses loanAmount
BigDecimal amountToValidate = guaranteeAmount != null ? guaranteeAmount : loanAmount;

// Validates against specific amount
GuarantorValidationResult result = 
  guarantorValidationService.validateGuarantorWithGuaranteeAmount(
    guarantor, loanAmount, amountToValidate, null);
```

### GuarantorValidationService
- `validateGuarantor()`: Backward compatible, uses loanAmount as guaranteeAmount
- `validateGuarantorWithGuaranteeAmount()`: New method, validates against specific guarantee amount
- All checks (capacity, ratio, etc.) use the specific guarantee amount

## Notification Messages

### Self-Guarantee
- No notification sent (auto-approved)

### Partial Guarantee
- "You have been requested to guarantee KES 30,000 of a KES 100,000 loan application for Kevin Otieno..."

### Full Guarantee
- "You have been requested to guarantee a KES 100,000 loan application for Kevin Otieno..."

## Testing Scenarios

### Scenario 1: Full Self-Guarantee
- Member: Kevin (70k savings)
- Applies for: 100k loan
- Self-guarantees: 100k
- Expected: ✓ Eligible (100k <= 210k eligibility)
- Status: PENDING_LOAN_OFFICER_REVIEW

### Scenario 2: Partial Self-Guarantee + Guarantors
- Member: Kevin (70k savings)
- Applies for: 100k loan
- Self-guarantees: 50k
- Mercy guarantees: 30k (56k savings)
- George guarantees: 20k (40k savings)
- Expected: ✓ All eligible
- Mercy check: 56k >= 30k ✓
- George check: 40k >= 20k ✓

### Scenario 3: Guarantor Insufficient Funds
- Member: Kevin (70k savings)
- Applies for: 100k loan
- Mercy guarantees: 30k (but only 20k savings)
- Expected: ✗ Mercy not eligible
- Error: "Insufficient guarantee capacity. Available: KES 20,000. Required: KES 30,000"

## Files Modified

### Backend
- `GuarantorValidationService.java`: Added `validateGuarantorWithGuaranteeAmount()` method
- `MemberPortalController.java`: Updated to use new validation method with guarantee amount

### Frontend
- `GuarantorApprovalModal.tsx`: Updated to pass guarantee amount as query parameter
- `notificationService.ts`: Fixed to use dynamic backend URL

## Status
✅ Implementation Complete
- Guarantor eligibility now validates against specific guarantee amounts
- System dynamically handles all three loan scenarios
- Notifications include specific guarantee amounts
- Error messages reference correct amounts
