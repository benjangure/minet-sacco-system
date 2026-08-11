# January 2026 Data Import - Quick Instructions

## 📋 Overview

This script will:
1. **Backup** your database automatically
2. **Clear** all financial data (loans, repayments, transactions, balances)
3. **Import** January 2026 data from your CSV file
4. **Match** members by name and update their:
   - Shares balance (B/F minus 3000 as you mentioned)
   - Normal loan outstanding balance
   - Emergency loan 1 balance
   - Emergency loan 2 balance (if applicable)
   - Principal paid and interest collected

## 🚀 Quick Start

### Single Command:
```powershell
.\IMPORT_JANUARY_2026_DATA.ps1
```

That's it! The script handles everything.

## 📊 What the Script Does

### Data Mapping from CSV:
- **B/F (Brought Forward)** = Opening balance for January 2026
- **I (Interest)** = Interest paid in January
- **P (Principal)** = Principal paid in January  
- **C/D (Carried Down)** = Closing balance (what you want as outstanding)

### Shares Calculation:
- Takes **Balance C/F** from CSV
- Subtracts **3000** (as you mentioned they were added incorrectly)
- Sets as opening balance for January 2026

### Loans Updated:
For each member with loans, updates:
- `outstanding_balance` = C/D from CSV
- `principal_repaid` = P from CSV
- `interest_collected` = I from CSV
- `amount` = B/F from CSV (opening balance)

## ⚠️ Important Notes

1. **Member Matching**: Members are matched by NAME (case-insensitive)
2. **Loan Types**:
   - Normal Loan: Matches loans with loan product name containing "Normal"
   - Emergency Loan: Matches loans with loan product name containing "Emergency"
3. **Multiple Loans**: If a member has multiple loans of same type, updates the most recent one

## 📝 CSV File Location

Script expects CSV at:
```
C:\Users\Lenovo\Downloads\01 2026 - Listing Final(Listing).csv
```

If your file is elsewhere, edit the script and change `$CSV_FILE` variable.

## 🛡️ Safety Features

### Automatic Backup
- Backup created before any changes
- Saved to: `.\database_backups\backup_before_january_2026_import_YYYYMMDD_HHMMSS.sql`

### Rollback if Needed
If something goes wrong:
```powershell
# Find your backup file
$BACKUP = ".\database_backups\backup_before_january_2026_import_YYYYMMDD_HHMMSS.sql"

# Restore it
mysql -u root $DATABASE_NAME < $BACKUP
```

## 📋 Step-by-Step Execution

### 1. Open PowerShell as Administrator
```powershell
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system
```

### 2. Run the script
```powershell
.\IMPORT_JANUARY_2026_DATA.ps1
```

### 3. Script will:
- ✅ Create backup
- ✅ Read CSV file
- ✅ Parse 140+ member records
- ✅ Generate SQL import script
- ✅ Show summary

### 4. Confirm import
When prompted, type: `IMPORT`

### 5. Script executes:
- Clears financial data
- Imports January 2026 balances
- Shows success message

## ✅ Verification After Import

### Check in MySQL:
```sql
-- Total shares
SELECT SUM(balance) FROM accounts WHERE account_type = 'SHARES';

-- Total outstanding loans
SELECT SUM(outstanding_balance) FROM loans WHERE status IN ('DISBURSED', 'ACTIVE');

-- Sample member
SELECT m.name, a.balance, l.outstanding_balance
FROM members m
LEFT JOIN accounts a ON m.id = a.member_id
LEFT JOIN loans l ON m.id = l.member_id
WHERE m.name LIKE '%MBURU FREDRICK%';
```

### Check in Application:
1. Login to system
2. View member list
3. Check a few member details
4. Verify loan balances match CSV
5. Verify shares balances match (minus 3000)

## 🔄 Next Steps - Import February to May

After successful January import:

1. Get CSV files for Feb, Mar, Apr, May 2026
2. Run similar import for each month
3. Each month will update from previous month's closing balance

## 📊 Example: How Data is Processed

### From CSV:
```
MBURU FREDRICK MAINA
Shares: B/F=1,699,042, I=10,000, C/F=1,709,042
Normal Loan: B/F=2,803,550, P=174,450, I=28,036, C/D=2,629,100
```

### What Gets Updated:
```sql
-- Shares account
balance = 1,709,042 - 3000 = 1,706,042

-- Normal loan
outstanding_balance = 2,629,100
principal_repaid = 174,450
interest_collected = 28,036
amount = 2,803,550
```

## ⚠️ Troubleshooting

### Issue: Member not found
**Cause**: Name in CSV doesn't match database exactly
**Solution**: Check spelling, spaces, special characters

### Issue: Multiple loans matched
**Cause**: Member has multiple loans of same type
**Solution**: Script updates most recent one (by disbursement_date)

### Issue: Loan product not found
**Cause**: Loan product name doesn't contain "Normal" or "Emergency"
**Solution**: Check loan_products table names

### Issue: CSV parsing error
**Cause**: CSV format different than expected
**Solution**: Check CSV has comma separators and proper structure

## 📁 Generated Files

### SQL File: `JANUARY_2026_IMPORT.sql`
- Contains all SQL commands
- Can be reviewed before execution
- Can be re-run manually if needed

### Backup File: `database_backups\backup_before_january_2026_import_YYYYMMDD_HHMMSS.sql`
- Complete database backup
- Use for rollback if needed
- Keep for at least 30 days

## 🎯 Success Criteria

Import is successful if:
- [ ] Script completes without errors
- [ ] Backup file created
- [ ] Members' shares balances updated
- [ ] Members' loan balances match CSV
- [ ] Application shows correct data
- [ ] No negative balances
- [ ] Total amounts reasonable

## 📞 Support

If you encounter issues:
1. Check the error message
2. Review the generated SQL file
3. Verify CSV file format
4. Check member names match database
5. Use rollback if needed

## 💡 Tips

1. **Run during off-hours** - No users should be logged in
2. **Stop backend first** - Prevent concurrent access
3. **Review SQL file** - Before confirming import
4. **Test one member** - Verify data looks correct
5. **Keep backup safe** - For at least 30 days

---

## Quick Command Reference

```powershell
# Run import
.\IMPORT_JANUARY_2026_DATA.ps1

# Verify after import
mysql -u root -p sacco_db -e "SELECT SUM(balance) FROM accounts WHERE account_type='SHARES';"

# Rollback if needed
mysql -u root -p sacco_db < .\database_backups\backup_before_january_2026_import_YYYYMMDD_HHMMSS.sql
```

---

**Ready to go! Run the script when you're ready.** 🚀
