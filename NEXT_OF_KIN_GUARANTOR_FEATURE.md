# Next of Kin Guarantor Feature for Loan Top-Ups

## Issue
When a member is topping up a loan, there's no option to add a next of kin as an optional guarantor. The current form only allows adding regular guarantors who must be SACCO members with employee IDs.

## Current Behavior
- Top-up form has guarantor lookup by employee ID
- Guarantors must equal the top-up amount OR be left empty (optional)
- No field for next of kin information

## Desired Behavior
- Add an optional "Next of Kin as Guarantor" section
- Allow member to specify next of kin name, phone, and relationship
- Next of kin doesn't need to be a SACCO member
- This is stored as a backup/optional guarantor

## Database Schema
The `guarantors` table already has columns for next of kin:
- `is_next_of_kin` (tinyint) - flag to mark as NOK guarantor
- `next_of_kin_name` (varchar255)
- `next_of_kin_phone` (varchar20)
- `next_of_kin_relationship` (varchar100)

## Implementation Plan

### Frontend Changes (MemberLoanApplication.tsx)

1. **Add State Variables:**
```typescript
const [useNextOfKinGuarantor, setUseNextOfKinGuarantor] = useState(false);
const [nextOfKinName, setNextOfKinName] = useState('');
const [nextOfKinPhone, setNextOfKinPhone] = useState('');
const [nextOfKinRelationship, setNextOfKinRelationship] = useState('');
```

2. **Add UI Section in Top-Up Form:**
- Add checkbox: "Add Next of Kin as Optional Guarantor"
- Show fields when checked: Name, Phone, Relationship
- Place this section AFTER the regular guarantors section
- Make it clear this is optional

3. **Update handleTopupSubmit:**
- If next of kin is provided, add to guarantor requests with special flag
- Backend will recognize this and store with `is_next_of_kin = TRUE`

### Backend Changes

The backend already supports this through the guarantors table structure. Just need to ensure the endpoint accepts next of kin data:

**Endpoint**: `POST /loans/{loanId}/request-topup`

**Request Body:**
```json
{
  "requestedAmount": 50000,
  "purpose": "Business expansion",
  "guarantors": [
    {
      "memberNumber": "EMP001",
      "guaranteeAmount": 50000
    }
  ],
  "nextOfKin": {
    "name": "Jane Doe",
    "phone": "+254712345678",
    "relationship": "Spouse"
  }
}
```

The backend should create a guarantor record with:
- `is_next_of_kin = TRUE`
- `next_of_kin_name = "Jane Doe"`
- `next_of_kin_phone = "+254712345678"`
- `next_of_kin_relationship = "Spouse"`
- `member_id = NULL` (since NOK is not a SACCO member)
- `guarantee_amount = 0` or the top-up amount

## Benefits
- Provides backup contact information for loan recovery
- Allows members to specify beneficiaries/contacts for their loans
- No impact on existing guarantor logic
- Optional - members can still submit without it

## Testing Steps
1. Navigate to member portal loan application
2. Switch to "Top-Up" tab
3. Select an existing loan
4. Enter top-up amount and purpose
5. Check "Add Next of Kin as Optional Guarantor"
6. Fill in next of kin details
7. Submit - should succeed
8. Verify guarantor record created with `is_next_of_kin = TRUE`
