# Shares Account Dormant Implementation

## Overview
Minet SACCO is an in-house SACCO for Minet employees that does not accept share deposits. The shares account has been made dormant while keeping the infrastructure intact for future use.

## Changes Made

### 1. Backend - Account Service (`backend/src/main/java/com/minet/sacco/service/AccountService.java`)
**Change**: Added validation to prevent deposits to SHARES account
```java
// Prevent deposits to SHARES account (Minet SACCO does not accept share deposits)
if (accountType == Account.AccountType.SHARES) {
    throw new RuntimeException("Deposits to SHARES account are not allowed. This SACCO does not accept share contributions.");
}
```
**Impact**: Any attempt to deposit to a SHARES account will be rejected at the backend level, even if the UI is bypassed.

### 2. Member Portal - Deposit Request Form (`minetsacco-main/src/components/DepositRequestForm.tsx`)
**Change**: Filter out SHARES account from the account dropdown
```typescript
// Filter out SHARES account - this SACCO does not accept share deposits
const filteredAccounts = (response.data || []).filter((a: Account) => a.accountType !== 'SHARES');
```
**Impact**: Members cannot see or select the SHARES account when submitting deposit requests.

### 3. Staff Portal - Savings Page (`minetsacco-main/src/pages/Savings.tsx`)
**Changes**:
- Filter SHARES account from deposit dropdown (only for DEPOSIT transactions)
- Added informational alert explaining why SHARES is not available for deposits
- Kept SHARES visible for withdrawal attempts (to show the restriction message)

**Impact**: 
- Tellers and Treasurers cannot deposit to SHARES accounts
- Clear messaging about why SHARES deposits are not allowed
- Withdrawal restrictions remain in place

### 4. Bulk Processing - Monthly Contributions Template (`minetsacco-main/src/pages/BulkProcessing.tsx`)
**Change**: Removed "Shares" column from the monthly contributions Excel template
```typescript
// Before: "Shares": 2000,
// After: Removed entirely
```
**Impact**: The monthly contributions template no longer includes a shares column, preventing bulk share deposits.

## User Experience

### For Members
- ✓ Can see their SHARES account balance in the dashboard
- ✗ Cannot select SHARES account in deposit requests
- ✗ Cannot make deposits to SHARES account

### For Staff (Teller/Treasurer)
- ✓ Can see SHARES account in the system
- ✗ Cannot deposit to SHARES account via direct transaction
- ✗ Cannot include SHARES in monthly contributions bulk upload
- ✗ Cannot withdraw from SHARES account (existing restriction)

### For Administrators
- ✓ Full visibility of all accounts including SHARES
- ✓ Can view SHARES balances in reports
- ✓ Can manage the system configuration

## Future Enablement

If Minet SACCO decides to enable share deposits in the future:

1. **Remove backend validation** - Comment out or remove the SHARES deposit check in `AccountService.java`
2. **Update UI filters** - Remove the `.filter(a => a.accountType !== 'SHARES')` lines
3. **Update template** - Add "Shares" column back to the monthly contributions template
4. **No database changes needed** - All SHARES accounts already exist with zero balance

## Data Integrity

- ✓ SHARES accounts are preserved in the database
- ✓ All existing SHARES account data is intact
- ✓ No data loss or deletion
- ✓ Audit trail remains complete
- ✓ Easy to reverse if needed

## Testing Checklist

- [ ] Member cannot select SHARES in deposit request form
- [ ] Teller cannot deposit to SHARES via direct transaction
- [ ] Treasurer cannot deposit to SHARES via direct transaction
- [ ] Monthly contributions template does not include SHARES column
- [ ] Backend rejects any SHARES deposit attempts
- [ ] SHARES account balance still visible in dashboards
- [ ] Withdrawal restrictions from SHARES still work
- [ ] Reports still show SHARES account data
