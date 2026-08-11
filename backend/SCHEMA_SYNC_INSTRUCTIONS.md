# Production Database Schema Synchronization Guide

**Date:** August 5, 2026  
**Purpose:** Sync production `minetsacco` database structure with local `tminet` database  
**Risk Level:** LOW (Schema changes only, no data modifications)

---

## ⚠️ CRITICAL: READ THIS FIRST

### What This Script Does:
✅ **ADDS** missing tables (push_subscriptions, user_devices, topup_guarantors)  
✅ **ADDS** missing columns to existing tables  
✅ **ADDS** performance indexes  
✅ **BACKFILLS** safe data (full_name, original_principal)  
✅ **UPDATES** Flyway migration tracking  

### What This Script Does NOT Do:
❌ **Does NOT drop** any tables  
❌ **Does NOT drop** any columns  
❌ **Does NOT delete** any data  
❌ **Does NOT modify** existing data (except safe backfills)  

---

## 📋 Pre-Deployment Checklist

### 1. **Backup Production Database** (MANDATORY)

```bash
# Full backup with structure and data
mysqldump -u minetsacco -p minetsacco > backup_minetsacco_$(date +%Y%m%d_%H%M%S).sql

# Schema-only backup (for quick reference)
mysqldump -u minetsacco -p --no-data minetsacco > backup_minetsacco_schema_$(date +%Y%m%d_%H%M%S).sql
```

### 2. **Test on Staging First** (if available)

If you have a staging server:
```bash
# Restore production backup to staging
mysql -u staging_user -p staging_minetsacco < backup_minetsacco_YYYYMMDD_HHMMSS.sql

# Run sync script on staging
mysql -u staging_user -p staging_minetsacco < SYNC_PRODUCTION_SCHEMA.sql

# Test application on staging
```

### 3. **Schedule Maintenance Window**

- **Recommended:** During off-hours (night/weekend)
- **Duration:** 5-10 minutes
- **Notify users:** System will be briefly unavailable

---

## 🚀 Deployment Steps

### Step 1: Stop Backend Application

```bash
# If using systemd
sudo systemctl stop minet-sacco

# If running manually
kill <backend_process_id>
```

### Step 2: Create Backup

```bash
cd /opt/minet-sacco/backups
mysqldump -u minetsacco -p minetsacco > backup_before_schema_sync_$(date +%Y%m%d_%H%M%S).sql

# Verify backup created
ls -lh backup_before_schema_sync_*
```

### Step 3: Run Schema Sync Script

```bash
mysql -u minetsacco -p minetsacco < SYNC_PRODUCTION_SCHEMA.sql
```

You should see output like:
```
Query OK, 0 rows affected
Table 'push_subscriptions' created
...
✓ push_subscriptions table exists
✓ user_devices table exists
...
SCHEMA SYNCHRONIZATION COMPLETED!
```

### Step 4: Verify Schema Changes

```sql
-- Connect to database
mysql -u minetsacco -p minetsacco

-- Check new tables exist
SHOW TABLES LIKE '%push%';
SHOW TABLES LIKE '%topup_guarantors%';

-- Check new columns exist
DESCRIBE members;
DESCRIBE loans;

-- Count records (should be unchanged)
SELECT COUNT(*) FROM members;
SELECT COUNT(*) FROM loans;
SELECT COUNT(*) FROM transactions;
```

### Step 5: Start Backend Application

```bash
# If using systemd
sudo systemctl start minet-sacco
sudo systemctl status minet-sacco

# Check logs
sudo journalctl -u minet-sacco -f
```

### Step 6: Test Application

1. **Staff Portal:**
   - Login: http://10.39.60.15/login
   - Check dashboard loads
   - View members list
   - View loans list

2. **Member Portal:**
   - Login: http://10.39.60.15/member/login
   - Check dashboard loads
   - View transactions
   - Test push notification settings

3. **Key Features:**
   - Create test loan
   - Process transaction
   - Generate report
   - Send notification

---

## 🔄 Rollback Plan

### If Something Goes Wrong:

#### Option 1: Quick Rollback (Restore Full Backup)

```bash
# Stop application
sudo systemctl stop minet-sacco

# Drop current database
mysql -u minetsacco -p -e "DROP DATABASE minetsacco;"

# Recreate database
mysql -u minetsacco -p -e "CREATE DATABASE minetsacco CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Restore backup
mysql -u minetsacco -p minetsacco < backup_before_schema_sync_YYYYMMDD_HHMMSS.sql

# Start application
sudo systemctl start minet-sacco
```


#### Option 2: Surgical Rollback (Remove Only New Changes)

If you want to keep any new data created after sync:

```sql
-- Connect to database
mysql -u minetsacco -p minetsacco

-- Drop new tables (if they cause issues)
DROP TABLE IF EXISTS push_subscriptions;
DROP TABLE IF EXISTS user_devices;
DROP TABLE IF EXISTS topup_guarantors;

-- Remove new columns (examples)
ALTER TABLE members DROP COLUMN IF EXISTS full_name;
ALTER TABLE loans DROP COLUMN IF EXISTS interest_collected;
ALTER TABLE loans DROP COLUMN IF EXISTS is_topup;

-- Remove Flyway entries for new migrations
DELETE FROM flyway_schema_history WHERE version IN ('149', '999');
```

---

## 🔍 Post-Deployment Monitoring

### Day 1: Intensive Monitoring

```bash
# Watch backend logs continuously
sudo journalctl -u minet-sacco -f

# Check database connections
mysql -u minetsacco -p minetsacco -e "SHOW PROCESSLIST;"

# Monitor database size
mysql -u minetsacco -p -e "SELECT table_schema AS 'Database', 
  ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) AS 'Size (MB)'
  FROM information_schema.tables 
  WHERE table_schema = 'minetsacco'
  GROUP BY table_schema;"
```

### Week 1: Regular Checks

- Check error logs daily
- Monitor application performance
- Verify no data anomalies
- Check user feedback

### After 1 Week: Safe to Delete Backup

If everything works perfectly for 7 days:
```bash
# Optional: Delete old backup to save space
rm backup_before_schema_sync_YYYYMMDD_HHMMSS.sql
```

---

## 🐛 Troubleshooting

### Issue: Backend won't start after sync

**Check:**
```bash
sudo journalctl -u minet-sacco -n 100
```

**Common causes:**
- Flyway migration mismatch
- Missing column application expects
- Foreign key constraint issues

**Solution:**
- Check application.properties for profile=prod
- Verify Flyway migrations are up to date
- Check for typos in column names

### Issue: Application works but push notifications don't

**Check:**
```sql
-- Verify table structure
DESCRIBE push_subscriptions;

-- Check if VAPID keys configured
SELECT * FROM system_settings WHERE setting_key LIKE '%vapid%';
```

**Solution:**
- Verify VAPID keys in application.properties
- Check browser console for errors
- Ensure HTTPS is enabled

### Issue: Loan top-up feature not working

**Check:**
```sql
-- Verify topup columns exist
SHOW COLUMNS FROM loans LIKE '%topup%';

-- Verify topup_guarantors table exists
SHOW CREATE TABLE topup_guarantors;
```

**Solution:**
- Re-run specific ALTER TABLE statements
- Check foreign key constraints

### Issue: Performance degradation

**Check:**
```sql
-- Verify indexes were created
SHOW INDEX FROM loans;
SHOW INDEX FROM members;
SHOW INDEX FROM transactions;
```

**Solution:**
- Run ANALYZE TABLE on affected tables
- Check slow query log
- Consider adding more indexes if needed

---

## 📊 Schema Changes Summary

### New Tables Created:
1. **push_subscriptions** - PWA push notification subscriptions
2. **user_devices** - Device tracking for users
3. **topup_guarantors** - Loan top-up guarantor management

### Tables Modified:

#### members table:
- `full_name` varchar(255) - Combined first and last name
- `next_of_kin_name` varchar(255) - Next of kin details
- `next_of_kin_phone` varchar(20)
- `next_of_kin_relationship` varchar(100)
- `is_exited` tinyint(1) - Exit tracking
- `exit_date` date
- `exit_reason` text

#### loans table:
- `interest_remaining` decimal(15,2) - Remaining interest
- `interest_collected` decimal(15,2) - Collected interest
- `original_principal` decimal(15,2) - Original loan amount
- `is_topup` tinyint(1) - Top-up indicator
- `parent_loan_id` bigint - Parent loan for top-ups
- `topup_additional_amount` decimal(15,2)
- `topup_request_id` bigint

#### users table:
- `first_name` varchar(100)
- `last_name` varchar(100)
- `phone` varchar(20)
- `is_first_login` tinyint(1)

#### loan_topup_requests table:
- `hr_approved` tinyint(1) - HR approval workflow
- `hr_approved_by` bigint
- `hr_approved_at` datetime
- `hr_rejection_reason` text
- `treasurer_approved` tinyint(1) - Treasurer approval workflow
- `treasurer_approved_by` bigint
- `treasurer_approved_at` datetime
- `treasurer_rejection_reason` text

#### guarantors table:
- `is_next_of_kin` tinyint(1) - Next of kin guarantor support
- `next_of_kin_name` varchar(255)
- `next_of_kin_phone` varchar(20)
- `next_of_kin_relationship` varchar(100)

### Performance Indexes Added:
- loans: parent_loan, topup_request, status_date, member_status
- members: status, employee_id, is_exited, full_name
- transactions: transaction_date, account_type_date, transaction_type
- guarantors: status, member_loan, next_of_kin
- notifications: user_read, created_at, target_role
- audit_logs: user_action, timestamp, entity_type_id

---

## ✅ Success Criteria

The migration is successful when:

1. ✅ Backend starts without errors
2. ✅ All verification queries pass
3. ✅ Staff portal loads and functions
4. ✅ Member portal loads and functions
5. ✅ Loans can be created/viewed
6. ✅ Transactions can be processed
7. ✅ Reports generate successfully
8. ✅ Push notifications can be enabled
9. ✅ No errors in logs for 24 hours
10. ✅ Users report no issues

---

## 📞 Support

If you encounter issues during deployment:

1. **First:** Check troubleshooting section above
2. **Second:** Review backend logs
3. **Third:** Check database error log
4. **Last Resort:** Rollback to backup

**Contact:** admin@minetsacco.co.ke

---

## 📝 Deployment Log Template

Keep a log of your deployment:

```
DEPLOYMENT LOG
=============
Date: _____________
Time Started: _____________
Performed By: _____________

Pre-Checks:
[ ] Backup created: backup_minetsacco_YYYYMMDD_HHMMSS.sql
[ ] Backend stopped
[ ] Users notified

Deployment:
[ ] Schema sync script executed
[ ] No errors in execution
[ ] Verification queries passed

Post-Checks:
[ ] Backend started successfully  
[ ] Staff portal accessible
[ ] Member portal accessible
[ ] Push notifications working
[ ] No errors in logs

Time Completed: _____________
Status: SUCCESS / ROLLBACK
Notes: _______________________________________
```

---

**Remember:** Always backup before making schema changes! 💾
