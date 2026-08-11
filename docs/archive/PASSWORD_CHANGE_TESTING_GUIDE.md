# Password Change Feature - Testing Guide

## Pre-Testing Checklist

- [ ] Backend server is running on `http://localhost:9090`
- [ ] Frontend is running on `http://localhost:5173` (or configured URL)
- [ ] Database is accessible and migrations are applied
- [ ] Email service is configured (for confirmation emails)
- [ ] Test user accounts are created (staff and member)

---

## Test Environment Setup

### Create Test Users

**Staff User**:
```sql
INSERT INTO users (username, password, role, created_at, updated_at) 
VALUES ('staff_test', '$2a$10$...', 'ADMIN', NOW(), NOW());
```

**Member User**:
```sql
INSERT INTO members (member_number, first_name, last_name, employee_id, created_at) 
VALUES ('MEM001', 'Test', 'Member', 'EMP001', NOW());

INSERT INTO users (username, password, member_id, role, created_at, updated_at) 
VALUES ('member_test', '$2a$10$...', 1, 'MEMBER', NOW(), NOW());
```

**Default Test Credentials**:
- Username: `member_test`
- Password: `TestPass123` (or your configured initial password)

---

## Test Suite 1: Staff Password Change

### Test 1.1: Successful Password Change

**Steps**:
1. Log in as staff user (e.g., `staff_test`)
2. Navigate to **Settings** → **Security**
3. Enter:
   - Current Password: `TestPass123`
   - New Password: `NewStaffPass456`
   - Confirm Password: `NewStaffPass456`
4. Click **Change Password**

**Expected Result**:
- ✅ Success toast: "Password changed successfully"
- ✅ Form clears
- ✅ Eye icons reset to hidden state
- ✅ Can log in with new password on next login

**Verification**:
```sql
-- Check password was updated
SELECT username, updated_at FROM users WHERE username = 'staff_test';
```

---

### Test 1.2: Invalid Current Password

**Steps**:
1. Log in as staff user
2. Navigate to **Settings** → **Security**
3. Enter:
   - Current Password: `WrongPassword`
   - New Password: `NewStaffPass456`
   - Confirm Password: `NewStaffPass456`
4. Click **Change Password**

**Expected Result**:
- ✅ Error toast: "Current password is incorrect"
- ✅ Form remains filled
- ✅ No password change in database

---

### Test 1.3: Passwords Don't Match

**Steps**:
1. Log in as staff user
2. Navigate to **Settings** → **Security**
3. Enter:
   - Current Password: `TestPass123`
   - New Password: `NewStaffPass456`
   - Confirm Password: `DifferentPass789`
4. Click **Change Password**

**Expected Result**:
- ✅ Error toast: "New passwords do not match"
- ✅ Form remains filled
- ✅ No password change in database

---

### Test 1.4: Password Too Short

**Steps**:
1. Log in as staff user
2. Navigate to **Settings** → **Security**
3. Enter:
   - Current Password: `TestPass123`
   - New Password: `Short1`
   - Confirm Password: `Short1`
4. Click **Change Password**

**Expected Result**:
- ✅ Error toast: "Password must be at least 8 characters"
- ✅ Form remains filled
- ✅ No password change in database

---

### Test 1.5: New Password Same as Current

**Steps**:
1. Log in as staff user
2. Navigate to **Settings** → **Security**
3. Enter:
   - Current Password: `TestPass123`
   - New Password: `TestPass123`
   - Confirm Password: `TestPass123`
4. Click **Change Password**

**Expected Result**:
- ✅ Error toast: "New password must be different from current password"
- ✅ Form remains filled
- ✅ No password change in database

---

### Test 1.6: Eye Icon Toggle

**Steps**:
1. Log in as staff user
2. Navigate to **Settings** → **Security**
3. Click eye icon for Current Password field
4. Verify password is visible (text input)
5. Click eye icon again
6. Verify password is hidden (password input)
7. Repeat for New Password and Confirm Password fields

**Expected Result**:
- ✅ Eye icon toggles between Eye and EyeOff icons
- ✅ Input type changes between "text" and "password"
- ✅ Password visibility toggles correctly

---

## Test Suite 2: Member Password Change

### Test 2.1: Successful Password Change (Fresh Login)

**Steps**:
1. Log in as member user (e.g., `member_test`)
2. Navigate to **Settings** → **Security**
3. Enter:
   - Current Password: `TestPass123`
   - New Password: `NewMemberPass456`
   - Confirm Password: `NewMemberPass456`
4. Click **Change Password**

**Expected Result**:
- ✅ Success toast: "Password changed successfully"
- ✅ Form clears
- ✅ Eye icons reset to hidden state
- ✅ Confirmation email sent to member's email
- ✅ Can log in with new password on next login

**Verification**:
```sql
-- Check password was updated
SELECT username, updated_at FROM users WHERE username = 'member_test';

-- Check audit log
SELECT * FROM audit_log WHERE action = 'PASSWORD_CHANGE' 
ORDER BY created_at DESC LIMIT 1;
```

---

### Test 2.2: Session Expired (401 Error)

**Steps**:
1. Log in as member user
2. Wait 24 hours (or manually expire token in browser dev tools)
3. Navigate to **Settings** → **Security**
4. Enter password change details
5. Click **Change Password**

**Expected Result**:
- ✅ Error toast: "Session Expired - Your session has expired. Please log out and log back in to continue."
- ✅ Form remains filled
- ✅ No password change in database

**Recovery**:
1. Click **Logout** button
2. Log back in
3. Try password change again - should succeed

---

### Test 2.3: Empty Current Password

**Steps**:
1. Log in as member user
2. Navigate to **Settings** → **Security**
3. Leave Current Password empty
4. Enter:
   - New Password: `NewMemberPass456`
   - Confirm Password: `NewMemberPass456`
5. Click **Change Password**

**Expected Result**:
- ✅ Error toast: "Please enter your current password"
- ✅ Form remains filled
- ✅ No password change in database

---

### Test 2.4: Empty New Password

**Steps**:
1. Log in as member user
2. Navigate to **Settings** → **Security**
3. Enter:
   - Current Password: `TestPass123`
   - New Password: (empty)
   - Confirm Password: `NewMemberPass456`
4. Click **Change Password**

**Expected Result**:
- ✅ Error toast: "Please enter a new password"
- ✅ Form remains filled
- ✅ No password change in database

---

### Test 2.5: Network Error Handling

**Steps**:
1. Log in as member user
2. Disconnect internet or stop backend server
3. Navigate to **Settings** → **Security**
4. Enter valid password change details
5. Click **Change Password**

**Expected Result**:
- ✅ Error toast: "Failed to change password. Please check your connection and try again."
- ✅ Form remains filled
- ✅ No password change in database

---

### Test 2.6: Member Settings Navigation

**Steps**:
1. Log in as member user
2. Verify Settings link appears in **MemberSidebar**
3. Click Settings link
4. Verify page loads correctly
5. Verify Settings tab appears in mobile top navbar
6. Click Settings tab
7. Verify page loads correctly

**Expected Result**:
- ✅ Settings accessible from sidebar
- ✅ Settings accessible from mobile navbar
- ✅ Page loads without errors
- ✅ All form elements visible and functional

---

## Test Suite 3: Email Confirmation

### Test 3.1: Confirmation Email Sent

**Steps**:
1. Log in as member user
2. Navigate to **Settings** → **Security**
3. Change password successfully
4. Check email inbox for confirmation

**Expected Result**:
- ✅ Email received within 1 minute
- ✅ Email subject: "Password Change Confirmation"
- ✅ Email contains:
  - Confirmation that password was changed
  - Timestamp of change
  - Instructions to contact support if unauthorized
  - Link to reset password if needed

**Email Template Check**:
```
Subject: Password Change Confirmation

Dear [Member Name],

Your password has been successfully changed on [Date/Time].

If you did not make this change, please contact support immediately.

Best regards,
Minet Sacco System
```

---

## Test Suite 4: Audit Logging

### Test 4.1: Password Change Logged

**Steps**:
1. Log in as member user
2. Change password successfully
3. Check audit log in database

**Expected Result**:
```sql
SELECT * FROM audit_log 
WHERE action = 'PASSWORD_CHANGE' 
AND user_id = (SELECT id FROM users WHERE username = 'member_test')
ORDER BY created_at DESC LIMIT 1;
```

Should show:
- ✅ Action: `PASSWORD_CHANGE`
- ✅ User ID: Correct member user ID
- ✅ Timestamp: Current time
- ✅ Details: "Self-service password change by user"

---

## Test Suite 5: Security Validation

### Test 5.1: Password Hashing

**Steps**:
1. Change password to `TestPassword123`
2. Check database password field

**Expected Result**:
- ✅ Password is hashed (starts with `$2a$10$`)
- ✅ Password is not stored in plain text
- ✅ Hash is different each time (BCrypt salt)

**Verification**:
```sql
SELECT username, password FROM users WHERE username = 'member_test';
-- Should show: $2a$10$... (BCrypt hash)
```

---

### Test 5.2: JWT Token Validation

**Steps**:
1. Log in as member user
2. Open browser DevTools → Network tab
3. Change password
4. Check Authorization header

**Expected Result**:
- ✅ Authorization header: `Bearer [JWT_TOKEN]`
- ✅ Token is valid and not expired
- ✅ Token contains correct user claims

---

## Test Suite 6: Cross-Browser Testing

### Test 6.1: Chrome

**Steps**:
1. Open Chrome
2. Navigate to member portal
3. Log in and change password
4. Verify all features work

**Expected Result**:
- ✅ All features work correctly
- ✅ Eye icons toggle properly
- ✅ Form validation works
- ✅ Success/error messages display

---

### Test 6.2: Firefox

**Steps**:
1. Open Firefox
2. Navigate to member portal
3. Log in and change password
4. Verify all features work

**Expected Result**:
- ✅ All features work correctly
- ✅ Eye icons toggle properly
- ✅ Form validation works
- ✅ Success/error messages display

---

### Test 6.3: Safari

**Steps**:
1. Open Safari
2. Navigate to member portal
3. Log in and change password
4. Verify all features work

**Expected Result**:
- ✅ All features work correctly
- ✅ Eye icons toggle properly
- ✅ Form validation works
- ✅ Success/error messages display

---

## Test Suite 7: Mobile Testing

### Test 7.1: Mobile Responsiveness

**Steps**:
1. Open member portal on mobile device
2. Navigate to Settings
3. Verify layout is responsive
4. Verify all form elements are accessible
5. Change password successfully

**Expected Result**:
- ✅ Layout adapts to mobile screen
- ✅ Form fields are properly sized
- ✅ Eye icons are clickable
- ✅ Buttons are easily tappable
- ✅ Password change works on mobile

---

### Test 7.2: Mobile Navigation

**Steps**:
1. Open member portal on mobile
2. Verify Settings tab appears in top navbar
3. Click Settings tab
4. Verify page loads correctly

**Expected Result**:
- ✅ Settings tab visible in mobile navbar
- ✅ Tab is clickable
- ✅ Page loads without errors

---

## Test Suite 8: Performance Testing

### Test 8.1: Response Time

**Steps**:
1. Open browser DevTools → Network tab
2. Log in as member user
3. Navigate to Settings
4. Change password
5. Measure response time

**Expected Result**:
- ✅ Password change request completes in < 2 seconds
- ✅ No timeout errors
- ✅ Response size is reasonable (< 1 KB)

---

## Test Results Template

```markdown
## Test Results - [Date]

### Test Suite 1: Staff Password Change
- [ ] Test 1.1: Successful Password Change - PASS/FAIL
- [ ] Test 1.2: Invalid Current Password - PASS/FAIL
- [ ] Test 1.3: Passwords Don't Match - PASS/FAIL
- [ ] Test 1.4: Password Too Short - PASS/FAIL
- [ ] Test 1.5: New Password Same as Current - PASS/FAIL
- [ ] Test 1.6: Eye Icon Toggle - PASS/FAIL

### Test Suite 2: Member Password Change
- [ ] Test 2.1: Successful Password Change - PASS/FAIL
- [ ] Test 2.2: Session Expired (401 Error) - PASS/FAIL
- [ ] Test 2.3: Empty Current Password - PASS/FAIL
- [ ] Test 2.4: Empty New Password - PASS/FAIL
- [ ] Test 2.5: Network Error Handling - PASS/FAIL
- [ ] Test 2.6: Member Settings Navigation - PASS/FAIL

### Test Suite 3: Email Confirmation
- [ ] Test 3.1: Confirmation Email Sent - PASS/FAIL

### Test Suite 4: Audit Logging
- [ ] Test 4.1: Password Change Logged - PASS/FAIL

### Test Suite 5: Security Validation
- [ ] Test 5.1: Password Hashing - PASS/FAIL
- [ ] Test 5.2: JWT Token Validation - PASS/FAIL

### Test Suite 6: Cross-Browser Testing
- [ ] Test 6.1: Chrome - PASS/FAIL
- [ ] Test 6.2: Firefox - PASS/FAIL
- [ ] Test 6.3: Safari - PASS/FAIL

### Test Suite 7: Mobile Testing
- [ ] Test 7.1: Mobile Responsiveness - PASS/FAIL
- [ ] Test 7.2: Mobile Navigation - PASS/FAIL

### Test Suite 8: Performance Testing
- [ ] Test 8.1: Response Time - PASS/FAIL

### Summary
- Total Tests: 24
- Passed: __
- Failed: __
- Pass Rate: __%

### Notes
[Add any observations or issues found]
```

---

## Continuous Testing

### Automated Tests (Recommended)

```bash
# Run unit tests
npm run test

# Run integration tests
npm run test:integration

# Run e2e tests
npm run test:e2e

# Run all tests
npm run test:all
```

### Manual Testing Schedule

- **Daily**: Test successful password change
- **Weekly**: Test all error scenarios
- **Monthly**: Test cross-browser compatibility
- **Quarterly**: Test mobile responsiveness

---

## Known Issues and Workarounds

### Issue: 401 Error After 24 Hours
- **Workaround**: Log out and log back in to refresh token
- **Status**: Expected behavior, not a bug

### Issue: Email Not Received
- **Workaround**: Check spam folder, verify email service is running
- **Status**: Check email service configuration

### Issue: Eye Icon Not Toggling
- **Workaround**: Refresh page, clear browser cache
- **Status**: Rare browser compatibility issue

---

## Support and Escalation

If tests fail:
1. Check backend logs: `tail -f /var/log/minetsacco/backend.log`
2. Check frontend console: DevTools → Console tab
3. Verify database connectivity
4. Contact development team with:
   - Test case that failed
   - Error message
   - Browser and OS information
   - Steps to reproduce

---

## Sign-Off

- **Tested By**: _______________
- **Date**: _______________
- **Status**: ✅ PASS / ❌ FAIL
- **Notes**: _______________
