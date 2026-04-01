# Deposit Approval Fix - Migration Cleanup & Restart

## Problem
Deposit approval was failing with error: `Unknown column 'status' in 'field list'`

The migrations V59, V60, V61 were created to add missing columns to the `audit_logs` table. The first attempt failed because:
1. They used `IF NOT EXISTS` syntax which MySQL 5.5 doesn't support
2. Some columns already existed from partial previous attempts, causing "Duplicate column" errors

## Solution Applied
Updated all three migration files to use MySQL 5.5 compatible syntax with INFORMATION_SCHEMA checks:
- `V59__Add_error_message_to_audit_logs.sql` - Uses INFORMATION_SCHEMA to check if column exists
- `V60__Add_status_to_audit_logs.sql` - Uses INFORMATION_SCHEMA to check if column exists
- `V61__Add_user_agent_and_ip_address_to_audit_logs.sql` - Uses INFORMATION_SCHEMA to check if column exists

This approach safely handles both scenarios:
- If column doesn't exist: adds it
- If column already exists: skips (no error)

## Next Steps

### Step 1: Clean Failed Migrations from Database
Execute this SQL in your MySQL client:

```sql
DELETE FROM flyway_schema_history WHERE version IN (59, 60, 61);
```

Or use the provided script:
```
backend/CLEANUP_MIGRATIONS.sql
```

### Step 2: Rebuild Backend
```bash
cd backend
mvn clean package
```

### Step 3: Restart Backend
The backend will automatically:
1. Detect the missing migrations
2. Execute the corrected V59, V60, V61 migrations
3. Add any missing columns to `audit_logs` table (skipping those that already exist)

### Step 4: Test Deposit Approval
1. Log in as a member and submit a deposit request
2. Log in as a teller
3. Go to Deposit Approval panel
4. Try to approve a deposit request
5. Should now work without errors

## What Was Fixed

### Backend Changes
- **AuditLog.java**: Already has proper `@Column` annotations for all fields
- **AuditService.java**: Already captures IP address and user agent
- **DepositRequestService.java**: Already wrapped audit logging in try-catch to prevent failures

### Database Migrations (Updated)
- **V59**: Adds `error_message` LONGTEXT column (with INFORMATION_SCHEMA check)
- **V60**: Adds `status` VARCHAR(50) column with DEFAULT 'SUCCESS' (with INFORMATION_SCHEMA check)
- **V61**: Adds `user_agent` VARCHAR(100) and `ip_address` VARCHAR(45) columns (with INFORMATION_SCHEMA checks)

### Frontend
- **DepositApprovalPanel.tsx**: Already properly formatted with URLSearchParams for query parameters

## Verification
After restart, verify the columns exist:
```sql
DESCRIBE audit_logs;
```

You should see these columns:
- `error_message` (LONGTEXT)
- `status` (VARCHAR(50))
- `user_agent` (VARCHAR(100))
- `ip_address` (VARCHAR(45))
