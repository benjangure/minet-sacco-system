# Next of Kin Guarantor - Correct Implementation Guide

## Overview
The next of kin should function EXACTLY like a regular guarantor, not as a separate optional feature. They are added to the guarantors list and stored in the guarantors table with a special flag `is_next_of_kin=TRUE`.

## Purpose
Next of kin guarantors provide backup guarantee coverage if regular guarantors leave the SACCO or default on their guarantee obligations.

---

## Current Implementation Issues

### ❌ Wrong Approach (Current)
- Next of kin stored separately in request body `nextOfKin` object
- Validation happens separately from guarantors
- Not added to the guarantors list in the UI
- Treated as optional metadata

### ✅ Correct Approach (Needed)
- Next of kin should be **added to the guarantors array** like regular guarantors
- Stored in `guarantors` table with `is_next_of_kin=TRUE`
- Shown in the "Added Guarantors" list in the UI
- Their guarantee amount counts toward total guarantee requirement
- Can be removed from the list like regular guarantors

---

## Required Frontend Changes

### 1. Add State Variables (Already Done - Lines 68-71, 99-102)
```typescript
// For Loan Application
const [nextOfKinNameLoan, setNextOfKinNameLoan] = useState('');
const [nextOfKinPhoneLoan, setNextOfKinPhoneLoan] = useState('');
const [nextOfKinRelationshipLoan, setNextOfKinRelationshipLoan] = useState('');
const [nextOfKinGuaranteeAmountLoan, setNextOfKinGuaranteeAmountLoan] = useState('');

// For Top-Up
const [nextOfKinName, setNextOfKinName] = useState('');
const [nextOfKinPhone, setNextOfKinPhone] = useState('');
const [nextOfKinRelationship, setNextOfKinRelationship] = useState('');
const [nextOfKinGuaranteeAmount, setNextOfKinGuaranteeAmount] = useState('');
```

### 2. Add Handler Functions

#### For Loan Application (Add after handleAddGuarantor ~line 385)
```typescript
const handleAddNextOfKinGuarantorLoan = () => {
  // Validate fields
  if (!nextOfKinNameLoan || !nextOfKinPhoneLoan || !nextOfKinRelationshipLoan) {
    toast({ 
      title: 'Error', 
      description: 'Please fill all next of kin fields',
      variant: 'destructive' 
    });
    return;
  }

  if (!nextOfKinGuaranteeAmountLoan || parseFloat(nextOfKinGuaranteeAmountLoan) <= 0) {
    toast({ 
      title: 'Error', 
      description: 'Please enter a valid guarantee amount', 
      variant: 'destructive' 
    });
    return;
  }

  // Validate phone format
  const phoneRegex = /^\+254[0-9]{9}$/;
  if (!phoneRegex.test(nextOfKinPhoneLoan)) {
    toast({ 
      title: 'Error', 
      description: 'Please enter a valid phone number (+254XXXXXXXXX)', 
      variant: 'destructive' 
    });
    return;
  }

  // Check if next of kin already added
  if (guarantors.some(g => g.isNextOfKin)) {
    toast({ 
      title: 'Error', 
      description: 'Next of kin already added', 
      variant: 'destructive' 
    });
    return;
  }

  // Add to guarantors array with special flag
  const nextOfKinGuarantor: GuarantorWithAmount = {
    memberId: 0, // Next of kin is not a member
    employeeId: 'NOK', // Special marker for next of kin
    firstName: nextOfKinNameLoan.split(' ')[0] || nextOfKinNameLoan,
    lastName: nextOfKinNameLoan.split(' ').slice(1).join(' ') || '',
    guaranteeAmount: parseFloat(nextOfKinGuaranteeAmountLoan),
    isSelfGuarantee: false,
    isNextOfKin: true, // Special flag
    nextOfKinPhone: nextOfKinPhoneLoan,
    nextOfKinRelationship: nextOfKinRelationshipLoan
  };

  setGuarantors([...guarantors, nextOfKinGuarantor]);
  
  // Clear form
  setNextOfKinNameLoan('');
  setNextOfKinPhoneLoan('');
  setNextOfKinRelationshipLoan('');
  setNextOfKinGuaranteeAmountLoan('');

  toast({ 
    title: 'Success', 
    description: 'Next of kin added as guarantor' 
  });
};
```

#### For Top-Up (Add after handleAddTopupGuarantor)
```typescript
const handleAddNextOfKinGuarantorTopup = () => {
  // Validate fields
  if (!nextOfKinName || !nextOfKinPhone || !nextOfKinRelationship) {
    toast({ 
      title: 'Error', 
      description: 'Please fill all next of kin fields',
      variant: 'destructive' 
    });
    return;
  }

  if (!nextOfKinGuaranteeAmount || parseFloat(nextOfKinGuaranteeAmount) <= 0) {
    toast({ 
      title: 'Error', 
      description: 'Please enter a valid guarantee amount', 
      variant: 'destructive' 
    });
    return;
  }

  // Validate phone format
  const phoneRegex = /^\+254[0-9]{9}$/;
  if (!phoneRegex.test(nextOfKinPhone)) {
    toast({ 
      title: 'Error', 
      description: 'Please enter a valid phone number (+254XXXXXXXXX)', 
      variant: 'destructive' 
    });
    return;
  }

  // Check if next of kin already added
  if (topupGuarantors.some(g => g.isNextOfKin)) {
    toast({ 
      title: 'Error', 
      description: 'Next of kin already added', 
      variant: 'destructive' 
    });
    return;
  }

  // Add to guarantors array with special flag
  const nextOfKinGuarantor: GuarantorWithAmount = {
    memberId: 0, // Next of kin is not a member
    employeeId: 'NOK', // Special marker for next of kin
    firstName: nextOfKinName.split(' ')[0] || nextOfKinName,
    lastName: nextOfKinName.split(' ').slice(1).join(' ') || '',
    guaranteeAmount: parseFloat(nextOfKinGuaranteeAmount),
    isSelfGuarantee: false,
    isNextOfKin: true, // Special flag
    nextOfKinPhone: nextOfKinPhone,
    nextOfKinRelationship: nextOfKinRelationship
  };

  setTopupGuarantors([...topupGuarantors, nextOfKinGuarantor]);
  
  // Clear form
  setNextOfKinName('');
  setNextOfKinPhone('');
  setNextOfKinRelationship('');
  setNextOfKinGuaranteeAmount('');

  toast({ 
    title: 'Success', 
    description: 'Next of kin added as guarantor' 
    });
};
```

### 3. Update TypeScript Interfaces (Add to GuarantorWithAmount interface ~line 36)
```typescript
interface GuarantorWithAmount extends GuarantorInfo {
  guaranteeAmount: number;
  isSelfGuarantee: boolean;
  isNextOfKin?: boolean; // NEW: Flag for next of kin guarantors
  nextOfKinPhone?: string; // NEW: Phone for next of kin
  nextOfKinRelationship?: string; // NEW: Relationship for next of kin
}
```

### 4. Update handleSubmit - Remove Separate nextOfKin Object
The validation and submission code in `handleSubmit` and `handleTopupSubmit` should NOT add a separate `nextOfKin` object to the request body. The next of kin is already in the guarantors array!

**Remove these lines from handleSubmit (~line 470-490):**
```typescript
// REMOVE THIS BLOCK:
if (useNextOfKinGuarantorLoan) {
  if (!nextOfKinNameLoan || !nextOfKinPhoneLoan || !nextOfKinRelationshipLoan) {
    toast({ /* ... */ });
    return;
  }
  // Phone validation...
  requestBody.nextOfKin = { /* ... */ };
}
```

**Remove similar block from handleTopupSubmit.**

The guarantors array already contains the next of kin with all their information!

### 5. Update UI - Show in Guarantors List
The next of kin guarantor will automatically appear in the "Added Guarantors" list because they're in the guarantors array. To make them visually distinct, update the guarantor display to show a badge:

```typescript
{guarantors.map((guarantor, index) => (
  <div key={index} className="flex items-center justify-between bg-blue-50 p-3 rounded border border-blue-200">
    <div>
      <p className="text-sm font-medium">
        {guarantor.firstName} {guarantor.lastName}
        {guarantor.isNextOfKin && (
          <Badge className="ml-2 bg-purple-100 text-purple-800">Next of Kin</Badge>
        )}
      </p>
      <p className="text-xs text-muted-foreground">
        {guarantor.isNextOfKin ? (
          `Phone: ${guarantor.nextOfKinPhone} | Relationship: ${guarantor.nextOfKinRelationship} | Guarantee: ${formatCurrency(guarantor.guaranteeAmount)}`
        ) : (
          `Employee ID: ${guarantor.employeeId} | Guarantee: ${formatCurrency(guarantor.guaranteeAmount)}`
        )}
      </p>
    </div>
    <button /* remove button */ >
  </div>
))}
```

---

## Backend Changes

### The backend already handles this correctly!

The backend expects guarantors array where each item can have:
- Regular guarantor: `memberId`, `guaranteeAmount`
- Next of kin: `memberId=0` or `null`, `isNextOfKin=true`, `nextOfKinName`, `nextOfKinPhone`, `nextOfKinRelationship`, `guaranteeAmount`

The guarantors table already has these columns (from ADD_NEXT_OF_KIN_COLUMNS.sql):
- `is_next_of_kin` BOOLEAN
- `next_of_kin_name` VARCHAR(255)
- `next_of_kin_phone` VARCHAR(20)
- `next_of_kin_relationship` VARCHAR(50)

---

## Summary

**The key insight:** Next of kin IS a guarantor, just a special type. They should be:
1. ✅ Added to the guarantors array
2. ✅ Displayed in the guarantors list (with a badge)
3. ✅ Counted in total guarantee amount
4. ✅ Stored in the guarantors table with `is_next_of_kin=TRUE`
5. ✅ Removable like other guarantors

**NOT:**
- ❌ A separate optional field
- ❌ Stored in a separate `nextOfKin` object in request
- ❌ Validated separately
- ❌ Hidden from guarantors list

---

## Files to Modify

1. **MemberLoanApplication.tsx**
   - Add `isNextOfKin?`, `nextOfKinPhone?`, `nextOfKinRelationship?` to `GuarantorWithAmount` interface
   - Add `nextOfKinGuaranteeAmountLoan` state variable
   - Add `nextOfKinGuaranteeAmount` state variable (for topup)
   - Add `handleAddNextOfKinGuarantorLoan()` function
   - Add `handleAddNextOfKinGuarantorTopup()` function
   - Update UI card to have "Add to Guarantors" button instead of checkbox
   - Add guarantee amount input field
   - Update guarantor display to show badge for next of kin
   - Remove separate nextOfKin validation and submission code

2. **Backend** (Already done!)
   - Run `ADD_NEXT_OF_KIN_COLUMNS.sql` on production

---

**Next Steps:**
Given the extensive changes needed, would you like me to create a completely new version of the MemberLoanApplication.tsx file with the correct implementation?
