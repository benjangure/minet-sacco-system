# Password Setup & Login Issue - Debug Guide

## Issue Summary
When a member first logs in and sets a new password, upon attempting to login with the new credentials:
- Fields are cleared
- Page seems to refresh
- Login fails silently
- User cannot authenticate

## Changes Made

### 1. Backend - AuthController.java
Added comprehensive debug logging to both endpoints:

#### `/auth/member/setup-password` - Debug logs show:
- User found status
- First login flag verification
- Password verification progress
- Password update completion
- firstLogin flag change confirmation

#### `/auth/member/login` - Debug logs show:
- Authentication attempt start
- Authentication success confirmation
- User role and firstLogin status
- JWT token generation
- Token generation success

### 2. Frontend - MemberLogin.tsx
Enhanced error handling with detailed logging:
- Login attempt start with username
- Response data from backend
- Error detection and handling
- Success state transitions

### 3. Frontend - AuthContext.tsx
Added detailed logs in `memberSignIn()`:
- Fetch endpoint being called
- Response status
- Error response details
- Session creation steps
- Session storage confirmation

## Testing Steps

### Prerequisites
1. Backend is running on IntelliJ
2. Frontend is built (`npm run build` completed successfully ✓)
3. Open browser Developer Tools (F12) to see console logs

### Test Flow

#### Step 1: Initial Login (First Time)
1. Navigate to `http://localhost:3000/member/login` (or your frontend URL)
2. Enter credentials that trigger first login:
   - Username: (Phone number or Employee ID)
   - Password: National ID (initial password)
3. **Check Backend Console** for logs starting with:
   ```
   DEBUG: Member login attempt for: [username]
   DEBUG: Member authentication successful
   DEBUG: User found - id=[id], role=MEMBER, firstLogin=true
   DEBUG: Generating JWT token with memberId=[id], firstLogin=true
   ```
4. You should be redirected to `/member/password-setup`
5. **Check Browser Console** for log:
   ```
   DEBUG: First login detected - redirecting to password setup
   ```

#### Step 2: Password Setup
1. Enter new password meeting requirements:
   - At least 6 characters
   - 1 uppercase letter
   - 1 lowercase letter
   - 1 number
   - 1 special character
2. Confirm password
3. Click "Set Password"
4. **Check Backend Console** for logs:
   ```
   DEBUG: setup-password endpoint called for user: [username]
   DEBUG: User found, id=[id], firstLogin=true
   DEBUG: Verifying current password
   DEBUG: Current password verified, encoding new password
   DEBUG: User password updated and firstLogin set to false. New firstLogin value: false
   DEBUG: Updating member credential record
   DEBUG: Member credential record updated
   DEBUG: Password setup completed successfully for user: [username]
   ```
5. Should redirect to `/member/login` with success message
6. **Check Browser Console** for log:
   ```
   DEBUG: Password setup successful - redirecting to login
   ```

#### Step 3: Login with New Password (The Critical Test)
1. On login page, fields should show success message: "Password setup successful! Please login with your new password."
2. Enter the same username
3. Enter the NEW password you just created (NOT the national ID)
4. Click Login
5. **Check Backend Console** - Critical logs to watch:
   ```
   DEBUG: Member login attempt for: [username]
   DEBUG: Member authentication successful
   DEBUG: User found - id=[id], role=MEMBER, firstLogin=false
   DEBUG: Generating JWT token with memberId=[id], firstLogin=false
   DEBUG: JWT token generated successfully, first login flag: false
   ```
6. **Check Browser Console** for logs:
   ```
   DEBUG: memberSignIn - fetching from http://[backend]/api/auth/member/login
   DEBUG: memberSignIn - response status: 200
   DEBUG: memberSignIn - success response received, firstLogin: false
   DEBUG: memberSignIn - setting session for user: [username]
   DEBUG: memberSignIn - session saved to localStorage
   DEBUG: Login successful, redirecting to member dashboard
   ```
7. Should redirect to `/member/dashboard` with member portal visible

## Expected vs Actual Behavior

### Expected (After Fix)
- ✅ Password setup updates `firstLogin` to `false` in database
- ✅ Login with new password succeeds
- ✅ JWT token is generated
- ✅ Session is saved to localStorage
- ✅ User redirected to member dashboard
- ✅ All fields remain intact during login

### Actual (Before Fix)
- ❌ Fields cleared after login attempt
- ❌ Page refreshes/resets
- ❌ Login fails silently
- ❌ No session created
- ❌ Still redirected to password setup (stuck in loop)

## Root Cause Suspects

Based on code analysis, the issue is likely ONE of:

1. **firstLogin flag not being updated to false** after password setup
   - Verify in database: `SELECT username, first_login FROM users WHERE username='[test_user]'`
   - Should show `first_login = 0 or false` after password setup

2. **Password not being properly encoded/hashed** during setup
   - Backend may be failing password validation during login
   - Check BCrypt encoding consistency

3. **Session not persisting to localStorage** on login
   - Check browser DevTools → Application → Local Storage
   - Should show `session` key with JWT token after login

4. **Frontend clearing fields on form submission** (less likely)
   - Check if form reset is being triggered
   - Check if React state is being reset unexpectedly

## Debug Database Query

Run this SQL to check user state:
```sql
SELECT 
  id, 
  username, 
  password, 
  first_login, 
  created_at, 
  updated_at 
FROM users 
WHERE username = '[test_username]' 
ORDER BY updated_at DESC;
```

Expected after password setup:
- `first_login` should be `0` or `false`
- `updated_at` should be recent
- `password` should be a long hash (BCrypt format)

## Browser DevTools Inspection

### Console Tab
- Look for all logs starting with "DEBUG:"
- Check for any JavaScript errors (red)
- Check for failed fetch requests

### Network Tab
- Monitor requests to `/api/auth/member/login`
- Check response status (should be 200)
- Check response body contains `token` field
- Check response contains `firstLogin: false`

### Application → Local Storage
- After successful login, should have `session` key
- Value should be JSON with `token` and `user` properties
- Verify token is being stored

## Next Steps If Issue Persists

1. **Check database state** after each step (password setup, login attempt)
2. **Compare password hashes** in database before/after setup
3. **Test with direct SQL** to manually change `firstLogin` flag
4. **Check Spring Security logs** for authentication failures
5. **Monitor network requests** in DevTools to see actual responses

---

## Quick Test User

If you have test data, use:
- **Username:** EMP001
- **Initial Password:** National ID (check database or test data)
- **New Password:** Test123!
