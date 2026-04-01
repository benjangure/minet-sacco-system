# Audit Trail - Debug Steps

## What We Know

1. ✅ Backend started successfully
2. ✅ Debug logging is now in place in AuditService and DepositRequestService
3. ❌ Audit logs are still not appearing in the database
4. ⚠️ Audit trail endpoint returning 503 error

## Next Steps to Diagnose

### Step 1: Check Backend Console Output

When you perform a deposit rejection, look for these debug messages in the backend console:

```
DEBUG: postRejectionNotificationsAndAudit called for request ID=X
DEBUG: About to call auditService.logAction for REJECT
DEBUG: logAction called - action=REJECT, entityType=DEPOSIT_REQUEST, user=treasurer
DEBUG: Audit log saved successfully - ID=Y
```

**If you see these messages**: The audit logging code IS being called and IS saving to the database.

**If you DON'T see these messages**: The post-rejection method is not being called at all.

**If you see an ERROR message**: There's an exception preventing the audit log from being saved.

### Step 2: Perform Test Action and Capture Logs

1. **Open the backend console** (where you see the Spring Boot startup logs)
2. **Perform a deposit rejection** in the web UI
3. **Look for the DEBUG messages** in the console
4. **Copy and paste the relevant log lines** here

### Step 3: Check Database Directly

Run this SQL query:

```sql
SELECT * FROM audit_logs ORDER BY id DESC LIMIT 10;
```

**Expected**: Should show new entries after you perform actions

**If still empty**: The audit logs are not being saved to the database

### Step 4: Check for Exceptions

Look for any ERROR or EXCEPTION messages in the backend console:

```
ERROR: Failed to save audit log: ...
ERROR: Exception type: ...
```

These will tell us what's going wrong.

---

## Possible Issues

### Issue 1: Post-Rejection Method Not Being Called
**Symptom**: No DEBUG messages appear in console
**Cause**: The TellerController might not be calling `postRejectionNotificationsAndAudit()`
**Solution**: Check if the controller code was saved correctly

### Issue 2: User Object is Null
**Symptom**: `ERROR: User is null, cannot save audit log`
**Cause**: The user object passed to audit logging is null
**Solution**: Verify the user is being loaded correctly from the database

### Issue 3: Database Connection Issue
**Symptom**: `ERROR: Failed to save audit log: Connection refused`
**Cause**: Cannot connect to the database
**Solution**: Verify MySQL is running and accessible

### Issue 4: Foreign Key Constraint
**Symptom**: `ERROR: Failed to save audit log: Foreign key constraint failed`
**Cause**: The user_id doesn't exist in the users table
**Solution**: Verify the user exists in the database

---

## What to Do Now

1. **Perform a deposit rejection** in the web UI
2. **Check the backend console** for DEBUG messages
3. **Copy the relevant log output** and share it
4. **Run the SQL query** to check the database
5. **Share the results** so we can diagnose the issue

The debug logging will help us pinpoint exactly where the problem is!
