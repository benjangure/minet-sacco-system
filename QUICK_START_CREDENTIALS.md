# Quick Start - Member Credentials Dashboard

## 5-Minute Setup & Verification

### Step 1: Verify Backend is Running
```
Backend URL: http://localhost:8080
Expected: API responds with 200 status

Quick check:
curl http://localhost:8080/api/member-credentials -H "Authorization: Bearer YOUR_TOKEN"
```

### Step 2: Login to Frontend
1. Open http://localhost:3000
2. Login as: **admin** / **password** (or your ADMIN account)
3. Verify you see the sidebar

### Step 3: Navigate to Member Credentials
1. Look for "Member Credentials" in sidebar (under Admin section)
2. Or direct URL: http://localhost:3000/member-credentials
3. Click to open dashboard

### Step 4: Create a Test Member (If Needed)
1. Go to Members page
2. Click "Add Member"
3. Fill in details:
   - First Name: John
   - Last Name: Doe
   - Email: john@example.com
   - **IMPORTANT**: Leave "National ID" field EMPTY
4. Click Save
5. Credentials modal appears with temporary password

### Step 5: View Credentials in Dashboard
1. Go back to Member Credentials page
2. Refresh page (F5)
3. You should see "John Doe" in the table
4. Click eye icon on the row
5. Modal opens showing:
   - Username: (member number)
   - Password: (generated password)

### Step 6: Test Copy to Clipboard
1. In the credentials modal
2. Click the copy icon next to password
3. Icon should change to ✓ checkmark
4. Open any text editor and paste (Ctrl+V)
5. Should see the password

### Step 7: Test Search
1. Go back to table view
2. In search box, type "John"
3. Click Search button
4. Should only show John Doe's credentials

## Troubleshooting

### Problem: Dashboard page doesn't load
**Solution**: 
1. Check browser console (F12) for errors
2. Verify backend is running: `curl http://localhost:8080/health`
3. Check token is valid by logging out and back in

### Problem: 401 Unauthorized error
**Solution**:
1. Verify you're logged in as ADMIN/TREASURER/CUSTOMER_SUPPORT
2. Check Authorization header has Bearer token
3. Token might be expired - logout and login again

### Problem: No members appear in dashboard
**Solution**:
1. Create a member first via Members page
2. Check that credentials were created (look for console output)
3. Refresh the dashboard page

### Problem: Can't see password in modal
**Solution**:
1. Check if member already changed password
2. Try with a newly created member
3. Check database: `SELECT password FROM member_credentials LIMIT 1;`

### Problem: Migration didn't run (V125)
**Solution**:
1. V126 safeguard migration will handle it
2. Restart backend
3. Check logs for migration status
4. Manually verify password column:
   ```sql
   DESCRIBE member_credentials;
   ```

## Testing Member Login After Getting Credentials

### Step 1: Get credentials from dashboard
1. View member credentials in modal
2. Copy username and password

### Step 2: Test login as member
1. Logout from admin account
2. On login page, enter:
   - Username: (copied from dashboard)
   - Password: (copied from dashboard)
3. Should login successfully
4. Should see "Set New Password" prompt on first login
5. Follow password setup flow

### Step 3: Change password
1. Complete password setup
2. Verify new password works
3. Go back to admin credentials dashboard
4. Search for member
5. Try to view credentials
6. Should see: "Password has been changed by the member..."

## API Testing with Curl

### Get all credentials
```bash
curl -X GET http://localhost:8080/api/member-credentials \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Search credentials
```bash
curl -X GET "http://localhost:8080/api/member-credentials/search?query=John" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Get password for credential
```bash
curl -X GET http://localhost:8080/api/member-credentials/1/password \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### Get by member ID
```bash
curl -X GET http://localhost:8080/api/member-credentials/member/5 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

## File Locations Reference

### Backend Files
- Controller: `backend/src/main/java/com/minet/sacco/controller/MemberCredentialsController.java`
- Entity: `backend/src/main/java/com/minet/sacco/entity/MemberCredential.java`
- Migration: `backend/src/main/resources/db/migration/V125__Add_password_to_member_credentials.sql`

### Frontend Files
- Dashboard: `minetsacco-main/src/pages/MemberCredentials.tsx`
- Auth Context: `minetsacco-main/src/contexts/AuthContext.tsx`

### Documentation Files
- This file: `QUICK_START_CREDENTIALS.md`
- Full summary: `MEMBER_CREDENTIALS_IMPLEMENTATION_SUMMARY.md`
- Testing guide: `TESTING_CHECKLIST.md`
- Status: `FINAL_IMPLEMENTATION_STATUS.md`

## Database Queries for Verification

### Check password column exists
```sql
SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'member_credentials' AND COLUMN_NAME = 'password';
```

### View all credentials
```sql
SELECT id, username, member_name, email, 
       IF(password IS NULL, 'NULL', 'HAS_VALUE') as has_password,
       password_changed, created_at 
FROM member_credentials 
ORDER BY created_at DESC 
LIMIT 10;
```

### Find password for specific member
```sql
SELECT username, email, password, password_changed 
FROM member_credentials 
WHERE member_name LIKE '%John%';
```

### Check which members haven't changed password
```sql
SELECT username, member_name, email, created_at 
FROM member_credentials 
WHERE password_changed = FALSE;
```

### Check which passwords have been changed
```sql
SELECT username, member_name, password_changed_at 
FROM member_credentials 
WHERE password_changed = TRUE;
```

## Expected Behavior

### New Member Without National ID
- ✅ Temporary password generated
- ✅ Password visible in dashboard
- ✅ Can copy password
- ✅ Can login with password
- ✅ After login, can set new password
- ✅ After change, password no longer visible in dashboard

### New Member With National ID
- ✅ National ID used as password
- ✅ Password visible in dashboard
- ✅ Can copy National ID value
- ✅ Can login with National ID
- ✅ After first login, must change password
- ✅ After change, password no longer visible

### Bulk Upload Members
- ✅ All members appear in dashboard
- ✅ Same password behavior as individual
- ✅ Can search all members
- ✅ Can copy passwords before distribution

### Role-Based Access
- ✅ ADMIN can access: YES
- ✅ TREASURER can access: YES
- ✅ CUSTOMER_SUPPORT can access: YES
- ✅ LOAN_OFFICER can access: NO (403)
- ✅ MEMBER can access: NO (403)
- ✅ AUDITOR can access: NO (403)

## Performance Checklist

- [ ] Dashboard loads within 2 seconds
- [ ] Search works instantly
- [ ] Copy button is immediate
- [ ] Modal opens quickly
- [ ] No console errors or warnings
- [ ] Network tab shows efficient API calls

## Security Checklist

- [ ] Password only visible to authorized roles
- [ ] Token required for all API calls
- [ ] 401 error on unauthorized access
- [ ] Password hidden after member change
- [ ] No password logs in backend
- [ ] CORS properly configured
- [ ] No sensitive data in localStorage except token

## Success Indicators

If you can:
1. ✅ Create a member without National ID
2. ✅ View credentials in dashboard
3. ✅ Search for member
4. ✅ Copy password to clipboard
5. ✅ Login as member with credentials
6. ✅ Set new password as member
7. ✅ See "password changed" message in dashboard

**Then the implementation is working correctly!** 🎉

## Next Steps

1. **Run full testing** following TESTING_CHECKLIST.md
2. **Test with multiple members** to ensure scalability
3. **Test bulk upload** with sample Excel file
4. **Test on different browsers** (Chrome, Firefox, Safari)
5. **Review security settings** with your team
6. **Plan deployment** to production

## Contact / Issues

If you encounter any issues:
1. Check TROUBLESHOOTING section above
2. Review logs in backend console
3. Check browser console (F12)
4. Compare implementation with FINAL_IMPLEMENTATION_STATUS.md
5. Verify all files were created/modified correctly

---

**Ready to go!** Start with Step 1 above and you'll have a working credentials dashboard in minutes.
