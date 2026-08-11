# Optional Guarantors and Next of Kin Feature

## Date: August 10, 2026

## Overview
Made guarantors and next of kin optional when creating loans or requesting top-ups to provide more flexibility in loan applications.

---

## Changes Made

### 1. Loans.tsx (Staff Portal - Loan Application)
**Location:** `minetsacco-main/src/pages/Loans.tsx`

**Changes:**
- ✅ Removed validation requiring next of kin for all guarantors
- ✅ Changed label from "Next of Kin (Backup) Guarantor (Required)" to "Next of Kin (Backup) Guarantor (Optional)"
- ✅ Loans can now be submitted with or without next of kin backup guarantors

**Lines Modified:**
- Line 556-562: Removed NOK validation
- Line 1813: Updated label to show "(Optional)"

---

### 2. MemberLoanApplication.tsx (Member Portal - Loan & Top-Up Application)
**Location:** `minetsacco-main/src/pages/MemberLoanApplication.tsx`

**Changes:**
- ✅ Removed validation requiring at least one guarantor for top-up requests
- ✅ Made guarantee amount validation conditional (only if guarantors are provided)
- ✅ Updated submit button to allow submission without guarantors
- ✅ Top-ups can now be submitted without any guarantors

**Lines Modified:**
- Line 598-600: Removed guarantor requirement validation
- Line 602-614: Made guarantee total validation conditional
- Line 1210: Removed `guarantors.length === 0` from button disabled condition

---

### 3. LoanTopUpRequestDialog.tsx (Top-Up Request Dialog)
**Location:** `minetsacco-main/src/components/LoanTopUpRequestDialog.tsx`

**Changes:**
- ✅ Removed validation requiring at least one guarantor
- ✅ Made guarantee amount validation conditional (only if guarantors are provided)
- ✅ Updated submit button logic to allow submission without guarantors
- ✅ Button only validates total guarantee amount if guarantors are actually provided

**Lines Modified:**
- Line 141-143: Removed guarantor requirement validation
- Line 145-153: Made guarantee total validation conditional
- Line 346: Updated button disabled condition

---

## Behavior Changes

### Before:
❌ **Loans:** Required all guarantors to have a next of kin backup
❌ **Top-Ups:** Required at least one guarantor
❌ **Validation:** Blocked submission if requirements not met

### After:
✅ **Loans:** Next of kin backup is optional for each guarantor
✅ **Top-Ups:** Can be submitted with zero guarantors
✅ **Validation:** Only validates guarantee totals if guarantors are actually provided
✅ **Flexibility:** Allows loans without guarantors or next of kin

---

## Use Cases

### 1. Loan Without Next of Kin
- Staff can create a loan with guarantors but no next of kin backup
- System accepts the application without NOK validation error

### 2. Top-Up Without Guarantors
- Member can request a top-up with amount and purpose only
- No guarantors required for the top-up request
- Useful for small top-ups or trusted members

### 3. Partial Guarantors
- Can add some guarantors with NOK and some without
- Flexible guarantee structure based on loan requirements

---

## Technical Details

### Validation Logic

**Old Logic:**
```typescript
if (topupGuarantors.length === 0) {
  toast({ title: 'Error', description: 'Please add at least one guarantor', variant: 'destructive' });
  return;
}
```

**New Logic:**
```typescript
// Guarantors are now optional - allow submission without guarantors
// Only validate total if guarantors are provided
if (topupGuarantors.length > 0) {
  // Calculate and validate guarantee amounts
  const totalGuaranteeAmount = topupGuarantors.reduce((sum, g) => sum + g.guaranteeAmount, 0);
  
  if (Math.abs(totalGuaranteeAmount - requestedAmount) > 0.01) {
    toast({ 
      title: 'Error', 
      description: `Total guarantee amount must equal top-up amount`, 
      variant: 'destructive' 
    });
    return;
  }
}
```

### Button State Logic

**Old Logic:**
```typescript
disabled={submitting || guarantors.length === 0 || getTotalGuaranteeAmount() !== parseFloat(amount)}
```

**New Logic:**
```typescript
disabled={submitting || (guarantors.length > 0 && getTotalGuaranteeAmount() !== parseFloat(amount))}
```

---

## Backend Compatibility

The backend already supports optional guarantors:
- Empty guarantor arrays are accepted
- NULL next of kin values are handled
- No server-side validation requires guarantors

---

## Testing Checklist

- [ ] Create a loan with guarantors but no next of kin
- [ ] Create a loan with no guarantors at all
- [ ] Request a top-up with no guarantors
- [ ] Request a top-up with partial guarantors
- [ ] Verify guarantee amount validation only works when guarantors are present
- [ ] Ensure backend accepts empty guarantor arrays

---

## Files Modified

1. `minetsacco-main/src/pages/Loans.tsx`
2. `minetsacco-main/src/pages/MemberLoanApplication.tsx`
3. `minetsacco-main/src/components/LoanTopUpRequestDialog.tsx`

---

## Summary

✅ **Guarantors:** Now optional for loans and top-ups
✅ **Next of Kin:** Optional backup guarantor (removed requirement)
✅ **Validation:** Conditional - only validates if guarantors are provided
✅ **Flexibility:** Supports various guarantee structures
✅ **User Experience:** No blocking errors for missing guarantors

The system now provides maximum flexibility in loan and top-up applications while maintaining data integrity through conditional validation.
