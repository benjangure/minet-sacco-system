# Financial Data Reset - Complete Execution Guide

## 📋 Overview

This guide provides step-by-step instructions for safely deleting incorrect financial data and importing correct data from Excel.

## ⚠️ CRITICAL WARNINGS

**BEFORE YOU START:**
- ✅ This process will DELETE ALL financial data
- ✅ You MUST have a database backup
- ✅ You MUST have correct Excel file ready
- ✅ Test on a copy first if possible
- ✅ Inform all users - system will be down during import
- ✅ Choose a low-activity time (evening/weekend)

**ESTIMATED TIME:** 30-60 minutes depending on data volume

---

## 📁 Files Created

All scripts are located in: `c:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\`

### Scripts:
1. `1_BACKUP_DATABASE.ps1` - Creates full database backup
2. `2_VERIFY_CURRENT_DATA.sql` - Documents current state
3. `3_DELETE_FINANCIAL_DATA.sql` - Safely deletes financial data
4. `4_IMPORT_FROM_EXCEL.ps1` - Imports data from Excel
5. `5_VERIFY_IMPORTED_DATA.sql` - Verifies imported data

### Documentation:
- `FINANCIAL_DATA_RESET_ANALYSIS.md` - Detailed analysis
- `FINANCIAL_DATA_RESET_EXECUTION_GUIDE.md` - This guide
- `ROLLBACK_IF_NEEDED.sql` - Emergency rollback instructions

---

## 📝 Excel File Requirements

### Required Worksheets:

#### 1. LOANS Sheet
Columns needed (column names can vary, script will adapt):
- `id` or `loan_id` - Loan ID
- `member_id` - Member ID
- `loan_number` - Loan number (optional)
- `amount` or `principal` - Loan principal amount
- `interest_rate` - Interest rate (percentage, e.g., 12.5)
- `term_months` or `term` - Loan term in months
- `monthly_repayment` - Monthly payment amount
- `total_interest` - Total interest amount
- `total_repayable` - Total to be repaid
- `outstanding_balance` - Current outstanding balance
- `interest_collected` - Interest already collected (optional)
- `principal_repaid` - Principal already repaid (optional)
- `status` - Loan status (DISBURSED, ACTIVE, etc.)

#### 2. REPAYMENTS Sheet (Optional)
If you have repayment history:
- `loan_id` - Loan ID (must match loans sheet)
- `amount` - Total repayment amount
- `principal_amount` - Principal portion
- `interest_amount` - Interest portion
- `repayment_date` - Date of repayment
- `payment_method` - Payment method (CASH, MPESA, etc.)
- `reference_number` - Payment reference (optional)

#### 3. TRANSACTIONS Sheet (Optional)
If you want to restore transaction history:
- `account_id` - Account ID
- `amount` - Transaction amount
- `transaction_type` - Type (deposit, withdrawal, etc.)
- `transaction_date` - Date of transaction
- `description` - Transaction description

### Excel File Preparation Checklist:

- [ ] All amounts are numbers (no currency symbols)
- [ ] Interest rates are numbers (e.g., 12.5 not 12.5%)
- [ ] Dates are in proper format (YYYY-MM-DD or Excel date)
- [ ] No empty rows in the middle of data
- [ ] Column headers are in the first row
- [ ] Member IDs exist in the database
- [ ] Loan IDs match existing loans
- [ ] No duplicate loan IDs
- [ ] All required columns are present

---

## 🚀 EXECUTION PROCEDURE

### PHASE 1: PREPARATION (Do NOT Skip!)

#### Step 1.1: Review Analysis Document
```powershell
# Open and read the analysis
notepad .\FINANCIAL_DATA_RESET_ANALYSIS.md
```

**Action:** Read and understand what will be deleted and preserved.

#### Step 1.2: Prepare Excel File
- Place your Excel file in the project directory
- Rename it to: `financial_data_correct.xlsx`
- Or note the exact filename for later

#### Step 1.3: Inform Users
**CRITICAL:** Stop all users from using the system!
- Announce system downtime
- Ensure no one is processing loans or payments
- Stop the backend application if possible

---

### PHASE 2: BACKUP (MANDATORY!)

#### Step 2.1: Create Database Backup
```powershell
# Run the backup script
.\1_BACKUP_DATABASE.ps1
```

**Expected Output:**
```
================================================
  MINET SACCO - DATABASE BACKUP
================================================
✅ MySQL connection successful
💾 Creating backup...
✅ BACKUP SUCCESSFUL!
================================================
```

**Verification:**
- Check that backup file exists in `.\database_backups\`
- Verify file size is reasonable (not 0 KB)
- Note the backup filename (contains timestamp)

**⛔ STOP HERE IF BACKUP FAILS!**

---

### PHASE 3: DOCUMENT CURRENT STATE

#### Step 3.1: Run Verification Query
```powershell
# Connect to MySQL
mysql -u root -p

# Run the verification script
source 2_VERIFY_CURRENT_DATA.sql
```

**Action:** Save the output to a text file for comparison later.

```powershell
# Alternative: Save output directly
mysql -u root -p sacco_db < 2_VERIFY_CURRENT_DATA.sql > current_data_snapshot.txt
```

**Important Numbers to Note:**
- Total number of loans
- Total principal amount
- Total outstanding balance
- Number of repayments
- Number of transactions

---

### PHASE 4: DELETE FINANCIAL DATA

#### Step 4.1: Final Safety Check
**PAUSE and confirm:**
- [ ] Backup completed successfully?
- [ ] Current data documented?
- [ ] All users logged out?
- [ ] Excel file ready?
- [ ] You understand this will DELETE data?

#### Step 4.2: Execute Deletion
```powershell
# Connect to MySQL
mysql -u root -p

# Run the deletion script
source 3_DELETE_FINANCIAL_DATA.sql
```

**Expected Output:**
```
================================================
STEP 1: Creating deletion snapshot
STEP 2: Disabling foreign key checks
   ✅ Foreign key checks disabled
STEP 3: Deleting loan repayments
   ✅ Loan repayments deleted
STEP 4: Deleting loan top-up history
   ✅ Loan top-up history deleted
STEP 5: Deleting transactions
   ✅ Transactions deleted
STEP 6: Resetting loan financial fields
   ✅ Loan financial fields reset
STEP 7: Resetting account balances
   ✅ Account balances reset
STEP 8: Re-enabling foreign key checks
   ✅ Foreign key checks re-enabled
================================================
✅ FINANCIAL DATA DELETION COMPLETE
================================================
```

#### Step 4.3: Verify Deletion
```sql
-- Check that data is deleted
SELECT COUNT(*) FROM loan_repayments;  -- Should be 0
SELECT COUNT(*) FROM transactions;     -- Should be 0
SELECT SUM(amount) FROM loans;         -- Should be 0
SELECT SUM(balance) FROM accounts;     -- Should be 0
```

---

### PHASE 5: IMPORT CORRECT DATA

#### Step 5.1: Prepare Import Script
```powershell
# Edit the import script to set your Excel filename
notepad .\4_IMPORT_FROM_EXCEL.ps1

# Update this line:
# $EXCEL_FILE = ".\financial_data_correct.xlsx"
```

#### Step 5.2: Run Import Script
```powershell
# Run the import script
.\4_IMPORT_FROM_EXCEL.ps1
```

**Follow the prompts:**
1. Script will detect worksheets in your Excel file
2. Specify which worksheet contains LOANS data
3. Specify which worksheet contains REPAYMENTS data (or skip)
4. Script will generate SQL file
5. Review the generated SQL
6. Confirm to execute import

**Expected Output:**
```
================================================
  MINET SACCO - IMPORT FROM EXCEL
================================================
✅ Excel file found
📄 Worksheets found:
   • Loans
   • Repayments
📋 Processing LOANS worksheet
   Found 150 loan records
   ✅ Generated SQL for 150 loans
================================================
  IMPORT SUMMARY
================================================
📊 Records to be imported:
   • Loans: 150
   • Repayments: 320
✅ SQL IMPORT SUCCESSFUL!
================================================
```

#### Step 5.3: Review Generated SQL (Optional)
```powershell
# Check the generated SQL file
notepad .\import_data_from_excel.sql
```

---

### PHASE 6: VERIFICATION

#### Step 6.1: Run Verification Script
```powershell
# Run verification
mysql -u root -p sacco_db < 5_VERIFY_IMPORTED_DATA.sql > imported_data_verification.txt
```

#### Step 6.2: Compare Results
Open both files and compare:
```powershell
# Open original snapshot
notepad current_data_snapshot.txt

# Open new verification
notepad imported_data_verification.txt
```

**What to Check:**
- [ ] Number of loans matches Excel
- [ ] Total principal amount matches Excel
- [ ] Total outstanding balance correct
- [ ] Sample loans match Excel records
- [ ] No negative amounts
- [ ] No data quality issues reported

#### Step 6.3: Manual Spot Checks
```sql
-- Check specific loans
SELECT * FROM loans WHERE id = 1;
SELECT * FROM loans WHERE id = 50;
SELECT * FROM loans WHERE id = 100;

-- Compare with Excel file manually
```

---

### PHASE 7: APPLICATION TESTING

#### Step 7.1: Start Backend
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

#### Step 7.2: Test Application Features
1. **Login** - Can you log in?
2. **View Loans** - Do loans display correctly?
3. **Loan Details** - Check a few loan details
4. **Outstanding Balances** - Are they correct?
5. **Reports** - Generate a financial report
6. **Member Accounts** - Check member balances

#### Step 7.3: Test Calculations
- Open a loan with repayments
- Verify outstanding = principal - repaid
- Check interest calculations
- Verify monthly repayment amounts

---

## ✅ SUCCESS CRITERIA

Your import is successful if:
- [ ] All verification checks pass
- [ ] Total amounts match Excel file
- [ ] Sample loans match Excel records
- [ ] Application loads without errors
- [ ] Loan details display correctly
- [ ] Outstanding balances are accurate
- [ ] Reports generate successfully
- [ ] No database errors in logs

---

## 🔴 ROLLBACK PROCEDURE

If anything goes wrong:

### Option 1: Quick Rollback (PowerShell)
```powershell
# Find your backup file
$BACKUP = ".\database_backups\backup_before_financial_reset_20260807_143000.sql"

# Drop and recreate database
mysql -u root -e "DROP DATABASE sacco_db;"
mysql -u root -e "CREATE DATABASE sacco_db;"

# Restore from backup
mysql -u root sacco_db < $BACKUP
```

### Option 2: Manual Rollback
```powershell
# Connect to MySQL
mysql -u root -p

# Run these commands
DROP DATABASE IF EXISTS sacco_db;
CREATE DATABASE sacco_db;
exit;

# Restore backup
mysql -u root -p sacco_db < .\database_backups\backup_before_financial_reset_YYYYMMDD_HHMMSS.sql
```

### Verify Rollback
```sql
-- Check data is restored
SELECT COUNT(*) FROM loans;
SELECT SUM(amount) FROM loans;
SELECT COUNT(*) FROM loan_repayments;
```

**See also:** `ROLLBACK_IF_NEEDED.sql` for detailed rollback instructions

---

## 🐛 TROUBLESHOOTING

### Issue 1: Backup Script Fails
**Error:** "Cannot connect to MySQL"
**Solution:**
- Ensure MySQL is running (check XAMPP)
- Verify username/password in script
- Check if port 3306 is accessible

### Issue 2: Excel File Not Found
**Error:** "Excel file not found"
**Solution:**
- Verify file path is correct
- Place Excel file in project directory
- Update `$EXCEL_FILE` variable in script

### Issue 3: ImportExcel Module Error
**Error:** "ImportExcel module not found"
**Solution:**
```powershell
Install-Module ImportExcel -Scope CurrentUser -Force
```

### Issue 4: Foreign Key Constraint Error
**Error:** "Cannot delete or update a parent row"
**Solution:**
- The deletion script handles this automatically
- If manual deletion, run: `SET FOREIGN_KEY_CHECKS = 0;`

### Issue 5: Column Name Mismatch
**Error:** Script doesn't recognize Excel columns
**Solution:**
- Check Excel column headers match expected names
- Update column mapping in `4_IMPORT_FROM_EXCEL.ps1`
- See Excel Requirements section above

### Issue 6: Duplicate Loan Numbers
**Error:** "Duplicate entry for loan_number"
**Solution:**
- Check Excel for duplicate loan numbers
- Ensure loan_number column has unique values
- Or set loan_number to NULL if duplicates exist

---

## 📊 POST-IMPORT CHECKLIST

After successful import:

### Immediate Actions:
- [ ] Verify all data imported correctly
- [ ] Test critical application features
- [ ] Generate and review financial reports
- [ ] Check a sample of member accounts
- [ ] Verify loan calculations

### Within 24 Hours:
- [ ] Monitor for any data issues
- [ ] Review user feedback
- [ ] Check application logs for errors
- [ ] Keep backup file safe for 30 days
- [ ] Document any issues found

### Communication:
- [ ] Notify users system is back online
- [ ] Inform users to report any discrepancies
- [ ] Send summary to management
- [ ] Document lessons learned

---

## 📞 SUPPORT

If you encounter issues:

1. **Check the logs:**
   - MySQL error log
   - Application logs
   - PowerShell script output

2. **Review documentation:**
   - This guide
   - Analysis document
   - Rollback instructions

3. **Don't panic:**
   - You have a backup
   - Data can be restored
   - Take your time

4. **Contact support:**
   - Provide error messages
   - Share script output
   - Describe what you were doing

---

## 📚 Additional Resources

- **MySQL Documentation:** https://dev.mysql.com/doc/
- **PowerShell ImportExcel:** https://github.com/dfinke/ImportExcel
- **Spring Boot Database:** Backend README.md

---

## ✅ FINAL CHECKLIST

Before you start:
- [ ] Read entire guide
- [ ] Understand what will happen
- [ ] Have Excel file ready
- [ ] Users are logged out
- [ ] You have time to complete (1 hour)
- [ ] You can rollback if needed

During execution:
- [ ] Follow steps in order
- [ ] Don't skip backup
- [ ] Save all output
- [ ] Verify each phase
- [ ] Test before going live

After completion:
- [ ] Data verified
- [ ] Application tested
- [ ] Users notified
- [ ] Backup archived
- [ ] Documentation updated

---

**Good luck! Be careful and methodical. The backup is your safety net.** 🛡️
