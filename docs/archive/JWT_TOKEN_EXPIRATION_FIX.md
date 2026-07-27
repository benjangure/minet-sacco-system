# JWT Token Expiration Issue - 401 Unauthorized Error

## Problem
When trying to change password as a member, you get a **401 Unauthorized** error:
```
PUT http://localhost:8080/api/member/change-password 401 (Unauthorized)
```

Backend logs show:
```
Unable to get JWT Token or JWT Token has expired
```

## Root Cause
The JWT authentication token has expired. This is normal behavior - tokens are designed to expire for security reasons.

## Solution

### Option 1: Quick Fix (Temporary)
1. **Log out** from the member portal
2. **Log back in** - This will generate a fresh JWT token
3. Try changing your password again

### Option 2: Extend Token Expiration (Permanent)
If you want tokens to last longer, modify the backend configuration:

**File**: `backend/src/main/resources/application.properties`

Find or add this line:
```properties
jwt.expiration=86400000
```

Change the value (in milliseconds):
- `86400000` = 24 hours (default)
- `604800000` = 7 days
- `2592000000` = 30 days

Then restart the backend server.

### Option 3: Implement Token Refresh (Best Practice)
For production, implement a token refresh mechanism:

1. **Backend**: Add a refresh token endpoint
2. **Frontend**: Automatically refresh token before expiration
3. **AuthContext**: Handle token refresh transparently

## Why This Happens

JWT tokens are stateless and expire for security:
- **Prevents unauthorized access** if a token is compromised
- **Limits session duration** for security
- **Standard practice** in modern APIs

## Current Token Expiration
Your system is currently set to expire tokens after **24 hours** of inactivity.

## Testing Password Change
After logging back in:
1. Navigate to **Settings** (top navbar or sidebar)
2. Go to **Security** tab
3. Enter current password
4. Enter new password (min 8 characters)
5. Confirm new password
6. Click **Change Password**
7. You should see a success message

## Prevention
To avoid this in the future:
- Don't leave the app idle for more than 24 hours without logging out
- Log out when done using the system
- Log back in if you see 401 errors

## Related Files
- Backend JWT config: `backend/src/main/resources/application.properties`
- JWT filter: `backend/src/main/java/com/minet/sacco/security/JwtRequestFilter.java`
- Auth context: `minetsacco-main/src/contexts/AuthContext.tsx`

---

**Status**: ✅ This is expected behavior. Simply log out and log back in to get a fresh token.
