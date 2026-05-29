# Member Password Change Fix

## Problem
Members were getting a 400 Bad Request error when trying to change their password with the error message:
```
"User account is not linked to a member. Please contact support. (User ID: 1)"
```

## Root Cause
The `changeMemberPassword()` endpoint in `MemberPortalController` was calling `getCurrentMember()` which requires the user to have a `memberId` field set in the database. However, member users don't always have this field populated, causing the endpoint to fail.

The issue was that the endpoint was trying to:
1. Call `getCurrentMember()` - which throws an error if `user.getMemberId()` is null
2. Then look up the user by `memberId` - which is redundant since we already have the authenticated user

## Solution
Changed the `changeMemberPassword()` endpoint to:
1. Get the authenticated user directly from the security context using `SecurityContextHolder`
2. Extract the username from the authentication
3. Look up the user by username instead of memberId
4. Proceed with password validation and update

This approach:
- Doesn't depend on the `memberId` field being set
- Works for any authenticated member user
- Is more direct and efficient
- Avoids the unnecessary `getCurrentMember()` call

## Changes Made

**File**: `backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java`

**Before**:
```java
try {
    Member member = getCurrentMember();
    User memberUser = userRepository.findByMemberId(member.getId())
            .orElseThrow(() -> new RuntimeException("User account not found for this member"));
    // ... rest of password change logic
}
```

**After**:
```java
try {
    // Get authenticated user from security context
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    
    User memberUser = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User account not found"));
    // ... rest of password change logic
}
```

## How It Works Now

1. Member submits password change form with:
   - Current password
   - New password
   - Confirm new password

2. Backend endpoint:
   - Gets the authenticated user from Spring Security context
   - Extracts the username
   - Looks up the user by username
   - Validates current password
   - Validates new password requirements
   - Updates password in database
   - Sends confirmation email

3. Response:
   - Success: "Password changed successfully. You can now log in with your new password."
   - Error: Specific error message (incorrect password, passwords don't match, etc.)

## Testing
- [ ] Member logs in successfully
- [ ] Member navigates to Settings > Security
- [ ] Member enters current password (National ID for first login)
- [ ] Member enters new password (min 8 characters)
- [ ] Member confirms new password
- [ ] Click "Change Password"
- [ ] Success message appears
- [ ] Member can log out and log back in with new password

## Impact
- Fixes password change functionality for all member users
- No breaking changes to other endpoints
- Improves reliability by not depending on `memberId` field
- More efficient authentication lookup

## Files Modified
- `backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java` - Updated `changeMemberPassword()` method
