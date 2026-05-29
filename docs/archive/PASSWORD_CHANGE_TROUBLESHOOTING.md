# Password Change Feature - Troubleshooting Guide

## Common Issues and Solutions

### Issue 1: "Session Expired" Error

**Error Message**: 
```
Session Expired
Your session has expired. Please log out and log back in to continue.
```

**Cause**: JWT token has expired (default: 24 hours after login)

**Solution**:
1. Click the **Logout** button in the member portal
2. Log back in with your credentials
3. Try changing your password again

**Prevention**: Log in again before attempting password change if you've been logged in for more than 24 hours

---

### Issue 2: "Current Password is Incorrect"

**Error Message**:
```
Error
Current password is incorrect. For first login, use your National ID.
```

**Cause**: The password you entered doesn't match your current password

**Solution**:
1. Double-check that you're entering your current password correctly
2. Remember that passwords are case-sensitive
3. If you forgot your current password, contact your administrator

**Note**: For first-time login, members use their National ID as the password

---

### Issue 3: "New Passwords Do Not Match"

**Error Message**:
```
Error
New passwords do not match
```

**Cause**: The "New Password" and "Confirm New Password" fields have different values

**Solution**:
1. Make sure both fields contain exactly the same password
2. Use the eye icon to verify what you're typing
3. Try again with matching passwords

---

### Issue 4: "New Password Must Be Different from Current Password"

**Error Message**:
```
Error
New password must be different from current password
```

**Cause**: You entered the same password as your current password

**Solution**:
1. Choose a different password
2. Make sure it's at least 8 characters long
3. Include uppercase, lowercase, numbers, and special characters for security

---

### Issue 5: "Password Must Be at Least 8 Characters"

**Error Message**:
```
Error
Password must be at least 8 characters
```

**Cause**: Your new password is shorter than 8 characters

**Solution**:
1. Enter a password with at least 8 characters
2. Example: `MyNewPass123!` (13 characters)
3. Longer passwords are more secure

---

### Issue 6: "Please Enter Your Current Password"

**Error Message**:
```
Error
Please enter your current password
```

**Cause**: The "Current Password" field is empty

**Solution**:
1. Click on the "Current Password" field
2. Enter your current password
3. Try again

---

### Issue 7: "Please Enter a New Password"

**Error Message**:
```
Error
Please enter a new password
```

**Cause**: The "New Password" field is empty

**Solution**:
1. Click on the "New Password" field
2. Enter your new password (8+ characters)
3. Try again

---

### Issue 8: "Failed to Change Password - Check Your Connection"

**Error Message**:
```
Error
Failed to change password. Please check your connection and try again.
```

**Cause**: Network connection issue or backend server is unreachable

**Solution**:
1. Check your internet connection
2. Verify the backend server is running
3. Try again in a few moments
4. If problem persists, contact your administrator

---

### Issue 9: "Server Error (500)"

**Error Message**:
```
Error
Server error (500). Please try again.
```

**Cause**: Backend server encountered an unexpected error

**Solution**:
1. Wait a few moments and try again
2. Check backend server logs for details
3. Contact your system administrator if the problem persists

---

### Issue 10: Eye Icon Not Working

**Problem**: The eye icon doesn't toggle password visibility

**Solution**:
1. Refresh the page (Ctrl+R or Cmd+R)
2. Clear browser cache and cookies
3. Try a different browser
4. Contact support if problem persists

---

## Password Requirements

✅ **Minimum Length**: 8 characters
✅ **Recommended**: Mix of uppercase, lowercase, numbers, and special characters
✅ **Examples of Strong Passwords**:
- `MyNewPass123!`
- `SecureP@ssw0rd`
- `Ch@nge2024Now`

❌ **Avoid**:
- Simple patterns: `12345678`, `abcdefgh`
- Personal information: birthdate, name, phone
- Dictionary words: `password`, `welcome`
- Repeating characters: `aaaaaaaa`, `11111111`

---

## Password Change Confirmation

After successfully changing your password, you will:
1. See a success message: "Password changed successfully"
2. Receive a confirmation email at your registered email address
3. Have the form automatically cleared
4. Be able to log in with your new password on next login

---

## Security Tips

🔒 **Do**:
- Change your password regularly (every 3-6 months)
- Use a unique password not used elsewhere
- Keep your password confidential
- Log out when done using the system
- Use the eye icon to verify you're typing correctly

🚫 **Don't**:
- Share your password with anyone
- Write your password down
- Use the same password for multiple accounts
- Click "Remember Password" on shared computers
- Change your password on public WiFi (if possible)

---

## Still Having Issues?

If you've tried all the solutions above and still can't change your password:

1. **Contact Your Administrator**
   - Provide the exact error message
   - Note the time the error occurred
   - Mention any recent system changes

2. **Check System Status**
   - Verify backend server is running
   - Check network connectivity
   - Ensure you have proper permissions

3. **Try Alternative Methods**
   - Use a different browser
   - Try from a different device
   - Clear browser cache and cookies

---

## For Administrators

### Debugging Password Change Issues

**Check Backend Logs**:
```bash
# View recent errors
tail -f /var/log/minetsacco/backend.log | grep -i password

# Search for specific user
grep "username" /var/log/minetsacco/backend.log | grep password
```

**Verify JWT Configuration**:
- Check token expiration time in `application.properties`
- Verify JWT secret key is configured
- Ensure token is being sent in Authorization header

**Database Checks**:
```sql
-- Verify user exists
SELECT id, username, password FROM users WHERE username = 'member_username';

-- Check password change history
SELECT * FROM audit_log WHERE action = 'PASSWORD_CHANGE' ORDER BY created_at DESC;
```

**Common Backend Issues**:
- JWT token expired: User needs to log out and log back in
- Password encoder misconfigured: Check BCrypt strength setting
- Email service down: Password changes succeed but confirmation email fails
- Database connection issue: Check database connectivity

---

## Version Information

- **Frontend**: React with TypeScript
- **Backend**: Spring Boot with Spring Security
- **Authentication**: JWT (JSON Web Tokens)
- **Password Hashing**: BCrypt (strength 10)
- **Token Expiration**: 24 hours (configurable)

---

## Related Documentation

- `PASSWORD_CHANGE_FINAL_STATUS.md` - Complete implementation status
- `STAFF_VS_MEMBER_PASSWORD_CHANGE_COMPARISON.md` - Implementation comparison
- `PASSWORD_CHANGE_QUICK_REFERENCE.md` - Quick reference guide
- `PASSWORD_CHANGE_ARCHITECTURE.md` - Architecture and design details
