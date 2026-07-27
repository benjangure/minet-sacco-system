# Guarantor Rejection Handling - Database Fix

## Issue Found

When testing the guarantor rejection feature, the backend threw an error:
```
Data truncated for column 'status' at row 1
```

This occurred because the `loans` table's `status` column was defined as an `ENUM` with a limited set of values that didn't include the new `PENDING_GUARANTOR_REPLACEMENT` status.

## Root Cause

The initial database schema (V1__Initial_schema.sql) defined the loans status as:
```sql
status ENUM('PENDING', 'APPROVED', 'REJECTED', 'DISBURSED', 'REPAID', 'DEFAULTED') DEFAULT 'PENDING'
```

This ENUM didn't include:
- `PENDING_GUARANTOR_APPROVAL`
- `PENDING_GUARANTOR_REPLACEMENT` (NEW - needed for rejection handling)
- `PENDING_LOAN_OFFICER_REVIEW`
- `PENDING_CREDIT_COMMITTEE`
- `PENDING_TREASURER`
- `ACTIVE`

## Solution

Created a new database migration: **V86__Extend_loan_status_enum.sql**

This migration:
1. Extends the `loans.status` ENUM to include all required statuses
2. Extends the `guarantors.status` ENUM to include the `REPLACED` status

### New Loans Status ENUM
```sql
ENUM(
    'PENDING',
    'PENDING_GUARANTOR_APPROVAL',
    'PENDING_GUARANTOR_REPLACEMENT',
    'PENDING_LOAN_OFFICER_REVIEW',
    'PENDING_CREDIT_COMMITTEE',
    'PENDING_TREASURER',
    'APPROVED',
    'REJECTED',
    'DISBURSED',
    'REPAID',
    'DEFAULTED',
    'ACTIVE'
)
```

### New Guarantors Status ENUM
```sql
ENUM(
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'REPLACED',
    'ACTIVE',
    'DECLINED',
    'RELEASED'
)
```

## How to Apply the Fix

### Step 1: Rebuild the Backend
In IntelliJ, rebuild the backend project:
1. Click **Build** → **Rebuild Project**
2. Wait for the build to complete

### Step 2: Restart the Backend
1. Stop the running backend server
2. Run the backend again
3. The migration V86 will automatically apply when the application starts

### Step 3: Test the Fix
1. Try rejecting a guarantor again
2. The loan status should now correctly change to `PENDING_GUARANTOR_REPLACEMENT`
3. The member should receive a notification with 3 action options
4. The dialog should appear in the frontend

## Files Modified

1. **backend/src/main/resources/db/migration/V86__Extend_loan_status_enum.sql** (NEW)
   - Extends ENUM values for loans.status
   - Extends ENUM values for guarantors.status

## Verification

After applying the migration, verify:
- ✅ Guarantor can reject without database errors
- ✅ Loan status changes to `PENDING_GUARANTOR_REPLACEMENT`
- ✅ Member receives notification
- ✅ Frontend dialog appears with 3 options
- ✅ All 3 options (Replace, Reduce, Withdraw) work correctly

## Notes

- This is a non-destructive migration (only extends ENUM, doesn't delete data)
- All existing loan records will continue to work
- The migration will run automatically on backend startup
- No manual database changes needed
