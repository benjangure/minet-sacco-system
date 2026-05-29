# Database Migration Guide - Direct SQL Approach

This guide explains how to migrate real SACCO data directly from your legacy system using SQL scripts, bypassing the Excel-based UI migration.

## Overview

Instead of using the Data Migration UI (which requires manual Excel preparation), you can migrate data directly at the database level using SQL scripts. This is:
- **More reliable** - Direct database-to-database transfer
- **Faster** - No manual Excel preparation needed
- **More accurate** - Fewer data entry errors
- **Auditable** - SQL scripts can be version controlled and reviewed

## Prerequisites

1. **Access to both databases**:
   - Legacy SACCO database (source)
   - New Minet SACCO database (target)

2. **Database credentials** for both systems

3. **Network connectivity** between the two databases (or ability to export/import)

4. **Backup** of both databases before migration

## Step-by-Step Migration Process

### Step 1: Prepare Your Legacy Database Schema

First, identify the table and column names in your legacy system:

```sql
-- Example: Check your legacy members table structure
DESCRIBE legacy_database.members;

-- Check what columns exist
SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'legacy_database' AND TABLE_NAME = 'members';
```

Common legacy column names to look for:
- Member ID / Employee ID
- First Name, Last Name
- National ID / ID Number
- Phone Number
- Email
- Employment Status / Employment Type
- Employer Name
- Department
- Member Status (Active/Inactive/Suspended)
- Months Contributed / Contribution Period

### Step 2: Customize the Migration Script

Edit `V97__Data_Migration_Template.sql` to match your legacy schema:

**Example: If your legacy table is named `tbl_members` instead of `members`:**

```sql
-- BEFORE (template):
FROM legacy_system.members as legacy_members

-- AFTER (your system):
FROM legacy_database.tbl_members as legacy_members
```

**Example: If your column names are different:**

```sql
-- BEFORE (template):
legacy_members.first_name,
legacy_members.last_name,

-- AFTER (your system):
legacy_members.fname,
legacy_members.lname,
```

### Step 3: Handle Password Hashing

The script includes a placeholder for member passwords. You have two options:

**Option A: Use a temporary password for all members**
```sql
-- All members get the same temporary password (they'll change it on first login)
'$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi' -- password: admin123
```

**Option B: Hash each member's national ID as their password**

You'll need to:
1. Export member data with national IDs
2. Hash them using bcrypt (strength 10)
3. Import the hashed passwords

Using Java to generate bcrypt hashes:
```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);
String hashedPassword = encoder.encode("nationalId123456");
System.out.println(hashedPassword);
```

Or using online tools (for testing only):
- https://bcrypt-generator.com/ (set strength to 10)

### Step 4: Test on a Backup Database

**CRITICAL: Never run on production first!**

```bash
# Create a backup of your target database
mysqldump -u root -p minet_sacco > minet_sacco_backup.sql

# Create a test database
mysql -u root -p -e "CREATE DATABASE minet_sacco_test;"

# Restore backup to test database
mysql -u root -p minet_sacco_test < minet_sacco_backup.sql

# Run migration on test database
mysql -u root -p minet_sacco_test < V97__Data_Migration_Template.sql
```

### Step 5: Verify Migration Results

After running the script, verify the data:

```sql
-- Check member count
SELECT COUNT(*) as total_members FROM members WHERE is_legacy_member = TRUE;

-- Check account balances
SELECT 
    account_type,
    COUNT(*) as account_count,
    SUM(balance) as total_balance
FROM accounts
WHERE member_id IN (SELECT id FROM members WHERE is_legacy_member = TRUE)
GROUP BY account_type;

-- Check loans
SELECT 
    status,
    COUNT(*) as loan_count,
    SUM(principal_amount) as total_principal,
    SUM(outstanding_balance) as total_outstanding
FROM loans
WHERE member_id IN (SELECT id FROM members WHERE is_legacy_member = TRUE)
GROUP BY status;

-- Check for data issues
SELECT 'Members without accounts' as issue, COUNT(*) as count
FROM members m
WHERE is_legacy_member = TRUE
  AND NOT EXISTS (SELECT 1 FROM accounts WHERE member_id = m.id);

SELECT 'Loans with missing members' as issue, COUNT(*) as count
FROM loans l
WHERE NOT EXISTS (SELECT 1 FROM members WHERE id = l.member_id);
```

### Step 6: Run on Production

Once verified on test database:

```bash
# Backup production database
mysqldump -u root -p minet_sacco > minet_sacco_production_backup.sql

# Run migration on production
mysql -u root -p minet_sacco < V97__Data_Migration_Template.sql

# Verify production migration
mysql -u root -p minet_sacco < verification_queries.sql
```

## Common Issues and Solutions

### Issue 1: "Unknown column" errors

**Cause**: Column names don't match your legacy schema

**Solution**: 
```sql
-- Check actual column names in your legacy table
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'legacy_database' AND TABLE_NAME = 'members';

-- Update the script with correct names
```

### Issue 2: "Duplicate entry" errors

**Cause**: Data already exists in target database

**Solution**: The script uses `ON DUPLICATE KEY UPDATE` to handle this. If you want to skip duplicates:
```sql
-- Add WHERE clause to skip existing records
WHERE legacy_members.member_id NOT IN (
    SELECT CAST(SUBSTRING(member_number, 2) AS UNSIGNED) 
    FROM members 
    WHERE member_number LIKE 'M%'
)
```

### Issue 3: Data type mismatches

**Cause**: Legacy system uses different data types

**Solution**: Use CAST or CONVERT functions:
```sql
-- Convert text to date
CAST(legacy_members.dob AS DATE)

-- Convert decimal to string
CAST(legacy_members.balance AS CHAR)

-- Convert string to decimal
CAST(legacy_members.balance AS DECIMAL(15,2))
```

### Issue 4: Missing loan products

**Cause**: Loan product names don't match

**Solution**: 
```sql
-- Check available loan products
SELECT id, name FROM loan_products;

-- Map legacy product names to new system
CASE 
    WHEN legacy_loans.product_name = 'Emergency' THEN 'Emergency Loan'
    WHEN legacy_loans.product_name = 'Development' THEN 'Development Loan'
    ELSE 'Emergency Loan'  -- default
END
```

## Data Validation Checklist

After migration, verify:

- [ ] All members migrated with correct names and IDs
- [ ] All accounts created (savings + shares for each member)
- [ ] Account balances match legacy system
- [ ] All loans migrated with correct amounts
- [ ] Loan outstanding balances match legacy system
- [ ] All guarantors linked to correct loans
- [ ] Member user accounts created with correct usernames
- [ ] No duplicate records
- [ ] No orphaned records (loans without members, etc.)
- [ ] Audit trail shows migration date

## Rollback Procedure

If something goes wrong:

```bash
# Restore from backup
mysql -u root -p minet_sacco < minet_sacco_backup.sql

# Or manually delete migrated data
DELETE FROM members WHERE is_legacy_member = TRUE;
DELETE FROM accounts WHERE member_id IN (
    SELECT id FROM members WHERE is_legacy_member = TRUE
);
DELETE FROM loans WHERE member_id IN (
    SELECT id FROM members WHERE is_legacy_member = TRUE
);
```

## Performance Considerations

For large datasets (10,000+ members):

1. **Disable indexes during migration** (faster inserts):
```sql
ALTER TABLE members DISABLE KEYS;
-- Run migration
ALTER TABLE members ENABLE KEYS;
```

2. **Increase batch size** if using Flyway:
```properties
# In application.properties
spring.flyway.batch=true
```

3. **Monitor database resources** during migration:
```bash
# Watch MySQL performance
SHOW PROCESSLIST;
SHOW STATUS LIKE 'Threads%';
```

## Next Steps After Migration

1. **Test the application** with migrated data
2. **Run reports** to verify data integrity
3. **Train staff** on the new system
4. **Set up automated backups** for production
5. **Monitor system performance** with real data
6. **Plan cutover date** from legacy system

## Support

If you encounter issues:

1. Check the error message in the migration log
2. Verify your legacy database schema matches the script
3. Test on a backup database first
4. Review the "Common Issues" section above
5. Adjust the script for your specific schema

---

**Important**: Always backup before running migrations. Test on a copy first. Never run directly on production without verification.
