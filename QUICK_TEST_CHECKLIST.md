# Quick Test Checklist - Password Setup & Login

## Pre-Test Setup
- [ ] Backend built and running in IntelliJ
- [ ] Frontend built (`npm run build` - already done ✓)
- [ ] Browser DevTools open (F12)
- [ ] Switch to Console tab in DevTools
- [ ] Have test user credentials ready

## Test Execution

### Test 1: Initial Login (First Time)
**Action:** Login with initial credentials (National ID)
```
Username: [employee_id]
Password: [national_id]
```

**Expected Result:**
- [ ] Redirected to password setup page
- [ ] Shows message: "Welcome! Please create a secure password for your account."
- [ ] Backend logs show:
  ```
  DEBUG: Member login attempt for: [username]
  DEBUG: Member authentication successful
  DEBUG: firstLogin=true
  ```
- [ ] Browser logs show:
  ```
  DEBUG: First login detected - redirecting to password setup
  ```

**If Failed:**
- [ ] Check backend console for ERROR lines
- [ ] Check browser console for red errors
- [ ] Verify username/password are correct
- [ ] Verify user has `first_login = 1` in database

---

### Test 2: Password Setup
**Action:** Create new password
```
New Password: Test123!
Confirm: Test123!
(Must meet: 6+ chars, uppercase, lowercase, number, special char)
```

**Expected Result:**
- [ ] All password requirements show green checkmarks
- [ ] "Passwords match" shows green
- [ ] "Set Password" button is enabled
- [ ] After clicking, shows: "Setting up..."
- [ ] Backend logs show:
  ```
  DEBUG: setup-password endpoint called for user: [username]
  DEBUG: User found, id=[id], firstLogin=true
  DEBUG: Current password verified, encoding new password
  DEBUG: User password updated and firstLogin set to false. New firstLogin value: false
  DEBUG: Password setup completed successfully
  ```
- [ ] Redirected to login page
- [ ] Shows message: "Password setup successful! Please login with your new password."

**If Failed:**
- [ ] Check backend logs for ERROR or "Password setup failed"
- [ ] Check if password meets all requirements
- [ ] Check if current password (National ID) is correct
- [ ] Check browser console for errors

---

### Test 3: Login with New Password (CRITICAL)
**Action:** Login with new password you just created
```
Username: [same_employee_id]
Password: Test123! (the new password, NOT national ID)
```

**Before Clicking Login:**
- [ ] Open DevTools Network tab
- [ ] Open DevTools Console tab
- [ ] Clear previous logs to see fresh output

**Expected Result:**
- [ ] Form fields should NOT be cleared
- [ ] Login button shows: "Logging in..."
- [ ] Backend logs show:
  ```
  DEBUG: Member login attempt for: [username]
  DEBUG: Member authentication successful
  DEBUG: User found - id=[id], role=MEMBER, firstLogin=false
  DEBUG: Generating JWT token with memberId=[id], firstLogin=false
  DEBUG: JWT token generated successfully, first login flag: false
  ```
- [ ] Browser console logs show:
  ```
  DEBUG: memberSignIn - fetching from http://[backend]/api/auth/member/login
  DEBUG: memberSignIn - response status: 200
  DEBUG: memberSignIn - success response received, firstLogin: false
  DEBUG: memberSignIn - setting session for user: [username]
  DEBUG: memberSignIn - session saved to localStorage
  DEBUG: Login successful, redirecting to member dashboard
  ```
- [ ] Redirected to `/member/dashboard`
- [ ] Member dashboard loads with member info
- [ ] Can see member portal navigation

**Network Tab Check:**
- [ ] Request to `/api/auth/member/login` shows Status 200
- [ ] Response contains `token`, `memberId`, `firstLogin: false`

**Local Storage Check:**
- [ ] DevTools → Application → Local Storage
- [ ] Find key `session`
- [ ] Value contains: `token`, `user` with `role: "MEMBER"`

**If Failed:**
- [ ] Check backend logs for ERROR
- [ ] Check Network response for error message
- [ ] Check if redirected back to login (infinite loop)
- [ ] Check if password was stored correctly in Test 2
- [ ] **Database Check:**
  ```sql
  SELECT id, username, first_login, password FROM users 
  WHERE username='[username]' 
  ORDER BY updated_at DESC;
  ```
  - Should show: `first_login = 0`, `password = [long_hash]`

---

## Common Issues & Quick Fixes

### Issue: Redirected back to password setup
**Cause:** `firstLogin` flag still = true in database
**Fix:** 
1. Check Test 2 logs for "Password setup completed"
2. If not showing, password setup didn't complete
3. Database may need manual update:
   ```sql
   UPDATE users SET first_login = 0, updated_at = NOW() 
   WHERE username='[username]';
   ```

### Issue: "Invalid username or password" error
**Cause:** New password not being saved or hashed incorrectly
**Fix:**
1. Check Test 2 logs for "Current password verified"
2. If that shows, but password won't work later, issue is hashing
3. Try password setup again
4. Make sure National ID was entered correctly in Test 1

### Issue: Login fields clear after clicking Login
**Cause:** Form reset or state issue (less likely with our fixes)
**Fix:**
1. Check browser console for JavaScript errors
2. Check Network tab - did request succeed?
3. If request failed, check backend logs
4. Try different password (simpler, no special chars)

### Issue: No logs appearing in console
**Cause:** DevTools closed, wrong tab, or old session cached
**Fix:**
1. Press F12 to open DevTools
2. Click Console tab
3. Ctrl+Shift+Delete to clear browser cache
4. Close all tabs and reopen
5. Logout from any existing session

---

## Success Criteria

✅ **You've fixed the issue when:**
1. Test 1: Initial login with National ID redirects to password setup
2. Test 2: Password setup completes and redirects to login
3. **Test 3: Login with new password succeeds and loads dashboard**

---

## Debug Command Cheat Sheet

### Backend Console Filtering
- Look for: `DEBUG: Member login attempt`
- Look for: `firstLogin=false` (in Test 3)
- Red lines: `ERROR:`

### Browser Console Filtering
- Look for: `DEBUG: memberSignIn`
- Look for: `session saved to localStorage`
- Red lines: JavaScript errors

### Database Query
```sql
SELECT username, first_login, email, role FROM users 
WHERE username='[your_test_username]';
```

Expected after Test 2: `first_login = 0 (or false)`
Expected after Test 3: Same, user successfully logged in

---

## Timeline

**Expected Duration:** 5-10 minutes per test cycle

1. Test 1 (Initial Login): 1-2 min
2. Test 2 (Password Setup): 2-3 min
3. Test 3 (Login with New Password): 1-2 min
4. Verification & Debugging: Variable

---

## Notes

- Keep both backend console and browser console visible during testing
- Write down exact error messages if any occur
- Note which DEBUG log is the last one before error/redirect
- This tells us exactly where in the flow the issue occurs
