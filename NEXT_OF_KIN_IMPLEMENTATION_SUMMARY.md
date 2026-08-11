# Next of Kin Guarantor Implementation Summary

## Changes Made

### 1. State Variables Added

**For Regular Loan Application:**
```typescript
const [useNextOfKinGuarantorLoan, setUseNextOfKinGuarantorLoan] = useState(false);
const [nextOfKinNameLoan, setNextOfKinNameLoan] = useState('');
const [nextOfKinPhoneLoan, setNextOfKinPhoneLoan] = useState('');
const [nextOfKinRelationshipLoan, setNextOfKinRelationshipLoan] = useState('');
```

**For Loan Top-Up:**
```typescript
const [useNextOfKinGuarantor, setUseNextOfKinGuarantor] = useState(false);
const [nextOfKinName, setNextOfKinName] = useState('');
const [nextOfKinPhone, setNextOfKinPhone] = useState('');
const [nextOfKinRelationship, setNextOfKinRelationship] = useState('');
```

### 2. Validation Logic

Both `handleSubmit` (loan) and `handleTopupSubmit` (top-up) now validate:
- Next of kin name is required if checkbox is checked
- Next of kin phone is required and must match format `+254XXXXXXXXX`
- Next of kin relationship is required

### 3. API Request Structure

**Loan Application Request:**
```json
{
  "loanProductId": 1,
  "amount": 100000,
  "termMonths": 12,
  "guarantors": [...],
  "nextOfKin": {
    "name": "Jane Doe",
    "phone": "+254712345678",
    "relationship": "Spouse"
  }
}
```

**Top-Up Request:**
```json
{
  "requestedAmount": 50000,
  "purpose": "Business expansion",
  "guarantors": [...],
  "nextOfKin": {
    "name": "Jane Doe",
    "phone": "+254712345678",
    "relationship": "Spouse"
  }
}
```

### 4. Form Reset

Both forms now reset next of kin fields after successful submission.

## UI Components Needed

You need to add the following UI sections to the form:

### For Loan Application (Apply Tab)
Add after the guarantor section, before the submit button:

```tsx
{/* Next of Kin as Optional Guarantor */}
<Card>
  <CardHeader>
    <div className="flex items-center justify-between">
      <CardTitle>Next of Kin as Optional Guarantor</CardTitle>
      <div className="flex items-center space-x-2">
        <input
          type="checkbox"
          id="useNextOfKinLoan"
          checked={useNextOfKinGuarantorLoan}
          onChange={(e) => setUseNextOfKinGuarantorLoan(e.target.checked)}
          className="h-4 w-4"
        />
        <Label htmlFor="useNextOfKinLoan" className="text-sm font-normal cursor-pointer">
          Add Next of Kin Information (Optional)
        </Label>
      </div>
    </div>
    <p className="text-sm text-gray-600">
      Optionally provide next of kin details as backup contact for this loan
    </p>
  </CardHeader>
  
  {useNextOfKinGuarantorLoan && (
    <CardContent className="space-y-4">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="space-y-2">
          <Label>Full Name *</Label>
          <Input
            value={nextOfKinNameLoan}
            onChange={(e) => setNextOfKinNameLoan(e.target.value)}
            placeholder="e.g. Jane Doe"
          />
        </div>
        
        <div className="space-y-2">
          <Label>Phone Number *</Label>
          <Input
            value={nextOfKinPhoneLoan}
            onChange={(e) => {
              const formatted = formatPhoneNumber(e.target.value);
              setNextOfKinPhoneLoan(formatted);
            }}
            placeholder="+254712345678"
            maxLength={13}
          />
          <p className="text-xs text-gray-500">Format: +254XXXXXXXXX (9 digits)</p>
        </div>
        
        <div className="space-y-2">
          <Label>Relationship *</Label>
          <Select value={nextOfKinRelationshipLoan} onValueChange={setNextOfKinRelationshipLoan}>
            <SelectTrigger>
              <SelectValue placeholder="Select relationship" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="Spouse">Spouse</SelectItem>
              <SelectItem value="Parent">Parent</SelectItem>
              <SelectItem value="Sibling">Sibling</SelectItem>
              <SelectItem value="Child">Child</SelectItem>
              <SelectItem value="Friend">Friend</SelectItem>
              <SelectItem value="Other">Other</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>
      
      <Alert>
        <AlertDescription>
          ℹ️ Next of kin information is optional and will be used as backup contact for loan recovery purposes. They don't need to be a SACCO member.
        </AlertDescription>
      </Alert>
    </CardContent>
  )}
</Card>
```

### For Top-Up (TopUp Tab)
Same structure but use the top-up state variables:
- `useNextOfKinGuarantor` instead of `useNextOfKinGuarantorLoan`
- `nextOfKinName` instead of `nextOfKinNameLoan`
- `nextOfKinPhone` instead of `nextOfKinPhoneLoan`
- `nextOfKinRelationship` instead of `nextOfKinRelationshipLoan`

## Phone Number Formatter

Add this helper function in the component (already exists for member phone, can reuse):

```typescript
const formatPhoneNumber = (value: string): string => {
  // Remove all non-digits
  let digits = value.replace(/\D/g, '');
  
  // If starts with 0, replace with 254
  if (digits.startsWith('0')) {
    digits = '254' + digits.substring(1);
  }
  
  // If starts with 254, add +
  if (digits.startsWith('254')) {
    return '+' + digits.substring(0, 12); // +254 + 9 digits = 12 chars
  }
  
  // If starts with 7 or 1 (common Kenyan prefixes), add +254
  if (digits.startsWith('7') || digits.startsWith('1')) {
    return '+254' + digits.substring(0, 9);
  }
  
  return '+254' + digits.substring(0, 9);
};
```

## Backend Requirements

The backend needs to handle the `nextOfKin` field in both endpoints:

1. **POST /member/apply-loan**
2. **POST /loans/{loanId}/request-topup**

Expected behavior:
- If `nextOfKin` is provided, create a guarantor record with:
  - `is_next_of_kin = TRUE`
  - `next_of_kin_name = request.nextOfKin.name`
  - `next_of_kin_phone = request.nextOfKin.phone`
  - `next_of_kin_relationship = request.nextOfKin.relationship`
  - `member_id = NULL` (not a SACCO member)
  - `guarantee_amount = 0` or loan amount

## Testing Checklist

### Loan Application
- [ ] Navigate to member portal → Apply for Loan
- [ ] Fill in loan details (product, amount, duration)
- [ ] Add regular guarantors
- [ ] Check "Add Next of Kin Information"
- [ ] Fill in next of kin details
- [ ] Submit - should succeed
- [ ] Verify guarantor record in database with `is_next_of_kin = TRUE`

### Top-Up
- [ ] Navigate to member portal → Request Top-Up
- [ ] Select existing loan
- [ ] Enter top-up amount and purpose
- [ ] Optionally add regular guarantors
- [ ] Check "Add Next of Kin Information"
- [ ] Fill in next of kin details
- [ ] Submit - should succeed
- [ ] Verify guarantor record in database

### Phone Validation
- [ ] Try entering phone with leading 0 (07XXXXXXXX) - should format to +254
- [ ] Try entering phone with leading 254 - should add +
- [ ] Try entering invalid format - should show error
- [ ] Try submitting with empty phone when checkbox checked - should show error

## Benefits

1. **Backup Contact**: Provides additional contact information for loan recovery
2. **Optional**: Members can choose whether to provide this information
3. **Non-Member Friendly**: Next of kin doesn't need to be a SACCO member
4. **Consistent UI**: Same experience for both loans and top-ups
5. **Validation**: Ensures proper phone format and required fields
