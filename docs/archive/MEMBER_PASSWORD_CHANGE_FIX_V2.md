# Member Password Change Fix - Version 2

## Problem
Members were getting a 400 Bad Request error when trying to change their password. The issue was that the member endpoint implementation didn't match the staff endpoint implementation.

## Root Cause
The member password change endpoint was missing two critical components that the staff endpoint had:

1. **Missing `@PreAuthorize` annotation** - The endpoint wasn't checking if the user was authenticated
2. **Missing `Authentication` parameter** - The endpoint wasn't receiving the authentication object as a method parameter

The staff endpoint in `UserController` uses:
```java
@PutMapping("/change-password")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', ...)")
public ResponseEntity<ApiResponse<String>> changeOwnPassword(
        @Valid @RequestBody PasswordChangeRequestDTO request,
        Authentication authentication) {
```

But the member endpoint was missing both the `@PreAuthorize` and `Authentication` parameter.

## Solution
Updated the member endpoint to match the staff endpoint pattern:

1. Added `@PreAuthorize("isAuthenticated()")` annotation
2. Added `Authentication authentication` parameter to the method signature
3. Use the `authentication` parameter directly instead of getting it from `SecurityContextHolder`

## Changes Made

**File**: `backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java`

**Before**:
```java
@PutMapping("/change-password")
public ResponseEntity<ApiResponse<String>> changeMemberPassword(
        @Valid @RequestBody PasswordChangeRequestDTO request) {
    
    try {
        // Get authenticated user from security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        // ...
    }
}
```

**After**:
```java
@PutMapping("/change-password")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<ApiResponse<String>> changeMemberPassword(
        @Valid @RequestBody PasswordChangeRequestDTO request,
        Authentication authentication) {
    
    try {
        // Get current user from authentication
        String username = authentication.getName();
        // ...
    }
}
```

## Key Differences from Staff Endpoint

| Aspect | Staff | Member |
|--------|-------|--------|
| Endpoint | `/api/users/change-password` | `/api/member/change-password` |
| Authorization | `@PreAuthorize("hasAnyRole(...)")` | `@PreAuthorize("isAuthenticated()")` |
| Authentication | Passed as parameter | Passed as parameter |
| Password Validation | Same | Same |
| Password Update | Via `userService.changePassword()` | Direct `userRepository.save()` |

## How It Works Now

1. Member submits password change form with:
   - Current password
   - New password
   - Confirm new password

2. Frontend sends PUT request to `/api/member/change-password` with:
   - Authorization header with member token
   - JSON body with password fields

3. Backend endpoint:
   - Checks if user is authenticated via `@PreAuthorize`
   - Receives `Authentication` object from Spring Security
   - Extracts username from authentication
   - Looks up user by username
   - Validates current password
   - Validates new password requirements
   - Updates password in database
   - Sends confirmation email

4. Response:
   - Success: "Password changed successfully. You can now log in with your new password."
   - Error: Specific error message

## Testing
- [ ] Member logs in successfully
- [ ] Member navigates to Settings > Security
- [ ] Member enters current password (National ID for first login)
- [ ] Member enters new password (min 8 characters)
- [ ] Member confirms new password
- [ ] Click "Change Password"
- [ ] Success message appears
- [ ] Member can log out and log back in with new password

## Why This Fixes the Issue

The 400 Bad Request error was likely caused by:
1. Missing `@PreAuthorize` annotation causing authorization issues
2. Improper authentication handling without the `Authentication` parameter
3. Spring Security not properly validating the request

By matching the staff endpoint pattern, we ensure:
- Proper Spring Security integration
- Correct authentication validation
- Consistent error handling
- Reliable password change functionality

## Files Modified
- `backend/src/main/java/com/minet/sacco/controller/MemberPortalController.java` - Updated `changeMemberPassword()` method
