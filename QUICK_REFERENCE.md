# Financial Data Reset - Quick Reference Card

## ⚡ Quick Command Reference

### 1. Backup (REQUIRED!)
```powershell
.\1_BACKUP_DATABASE.ps1
```

### 2. Document Current State
```powershell
mysql -u root -p sacco_db < 2_VERIFY_CURRENT_DATA.sql > current_snapshot.txt
```

### 3. Delete Financial Data
```powershell
mysql -u root -p
source 3_DELETE_FINANCIAL_DATA.sql
```

### 4. Import from Excel
```powershell
.\4_IMPORT_FROM_EXCEL.ps1
```

### 5. Verify Import
```powershell
mysql -u root -p sacco_db < 5_VERIFY_IMPORTED_DATA.sql > import_verification.txt
```

---

## 🚨 Emergency Rollback
```powershell
# Quick rollback command (update timestamp!)
$BACKUP = ".\database_backups\backup_before_financial_reset_YYYYMMDD_HHMMSS.sql"
mysql -u root -e "DROP DATABASE sacco_db; CREATE DATABASE sacco_db;"
mysql -u root sacco_db < $BACKUP
```

---

## 📋 Execution Order

1. ✅ **BACKUP** - Must succeed before continuing
2. ✅ **VERIFY** - Document current state
3. ⚠️ **DELETE** - Point of no return
4. ✅ **IMPORT** - Load correct data
5. ✅ **VERIFY** - Confirm success

---

## ⚠️ Critical Warnings

- **NEVER skip backup**
- **NEVER proceed if backup fails**
- **STOP users from accessing system**
- **SAVE all output for comparison**
- **TEST before declaring success**

---

## 📁 Files Location

All files are in: `C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\`

**Scripts:**
- `1_BACKUP_DATABASE.ps1`
- `2_VERIFY_CURRENT_DATA.sql`
- `3_DELETE_FINANCIAL_DATA.sql`
- `4_IMPORT_FROM_EXCEL.ps1`
- `5_VERIFY_IMPORTED_DATA.sql`

**Docs:**
- `FINANCIAL_DATA_RESET_EXECUTION_GUIDE.md` (Full guide)
- `FINANCIAL_DATA_RESET_ANALYSIS.md` (Technical details)
- `ROLLBACK_IF_NEEDED.sql` (Emergency recovery)
- `QUICK_REFERENCE.md` (This file)

---

## ✅ Success Checklist

- [ ] Backup completed
- [ ] Current data documented
- [ ] Excel file ready
- [ ] Users logged out
- [ ] Deletion completed
- [ ] Import completed
- [ ] Verification passed
- [ ] Application tested
- [ ] Reports accurate

---

## 🔢 What Gets Deleted

✅ **DELETED:**
- All loan repayments
- All transactions
- All loan top-up history
- All loan financial values (reset to 0)
- All account balances (reset to 0)

✅ **PRESERVED:**
- Member records
- User accounts
- Loan product definitions
- Guarantor relationships (structure)
- Loan application records (status)

---

## 📊 Excel Requirements

**Minimum columns needed for LOANS:**
- `id` - Loan ID
- `member_id` - Member ID
- `amount` - Principal
- `interest_rate` - Rate %
- `term_months` - Term
- `outstanding_balance` - Balance

**All amounts must be numbers (no currency symbols)**

---

## 🎯 Time Estimates

- Backup: **2-5 minutes**
- Verification: **5-10 minutes**
- Deletion: **1-2 minutes**
- Import: **10-20 minutes** (depends on data volume)
- Testing: **15-30 minutes**

**Total: 30-60 minutes**

---

## 💡 Tips

1. **Run during off-hours** (evening/weekend)
2. **Have Excel file ready** before starting
3. **Save all command output**
4. **Compare before/after numbers**
5. **Test thoroughly** before announcing success
6. **Keep backup for 30 days**

---

## 🆘 If Something Goes Wrong

1. **Stay calm**
2. **Don't run more scripts**
3. **Use the rollback procedure**
4. **You have a backup!**
5. **Contact support if needed**

---

## 📞 Emergency Contacts

- Database Admin: [Add contact]
- System Admin: [Add contact]
- Developer: [Add contact]

---

**Remember: The backup is your safety net. Never skip it!** 🛡️
