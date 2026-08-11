# Quick Start: Create Test Loan for Treasurer Notifications

## 🚀 Quick Start (Choose One Method)

### Option A: Automated API Script (Easiest)

```powershell
# In PowerShell, from the minet-sacco-system directory:
.\create-test-loan-for-treasurer.ps1
```

**What it does:**
- Logs in as admin
- Finds test members automatically
- Creates a loan application
- Approves all guarantors
- Takes the loan to PENDING_TREASURER status
- Shows you the final result

**Requirements:**
- Backend running on http://localhost:9090
- At least 3 members in database
- Admin credentials (default: admin/admin123)

---

### Option B: Direct SQL Script (Database Access)

```sql
-- 1. First, check if system is ready:
source check-system-readiness.sql

-- 2. Edit create-test-loan-direct.sql and update these IDs:
--    @loan_applicant_id, @guarantor1_id, @guarantor2_id, @loan_product_id

-- 3. Run the script:
source create-test-loan-direct.sql
```

**What it does:**
- Directly inserts test loan into database
- Creates guarantor records
- Auto-approves guarantors
- Sets loan to PENDING_TREASURER
- Creates treasurer notification

**Requirements:**
- MySQL access (phpMyAdmin, MySQL Workbench, or CLI)
- At least 3 members in database
- At least 1 loan product

---

## 📋 Pre-Flight Check

Run this first to see if your system is ready:

### SQL Check
```sql
-- In MySQL:
USE sacco_db;
source check-system-readiness.sql
```

### API Check
```powershell
# Test if backend is running:
curl http://localhost:9090/api/health

# Or test login:
curl -X POST http://localhost:9090/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"admin123"}'
```

---

## ✅ Expected Result

After running either script, you should see:

```
========================================
  FINAL LOAN STATUS
========================================
  Loan ID: 123
  Loan Number: LN20261234
  Status: PENDING_TREASURER ✓
  Applicant: John Doe
  Amount: KES 50,000
  Monthly Repayment: KES 4,583.33
========================================

✓✓✓ SUCCESS! ✓✓✓
The loan is now PENDING_TREASURER status.
The treasurer should receive a notification!
```

---

## 🔍 Verify Treasurer Notification

### Method 1: Check Database
```sql
SELECT 
    n.title,
    n.message,
    u.username as treasurer,
    n.created_at
FROM notifications n
JOIN users u ON n.user_id = u.id
WHERE u.role = 'TREASURER'
  AND n.reference_type = 'LOAN'
ORDER BY n.created_at DESC
LIMIT 5;
```

### Method 2: Login as Treasurer
1. Open frontend application
2. Login with treasurer credentials
3. Check notifications bell icon
4. Should see: "New Loan Awaiting Approval"

### Method 3: Check API
```powershell
# Get treasurer notifications (need treasurer token):
curl http://localhost:9090/api/notifications `
  -H "Authorization: Bearer TREASURER_TOKEN"
```

---

## 🧹 Cleanup Test Data

### Via SQL
```sql
-- Replace 123 with your test loan ID
SET @test_loan_id = 123;

DELETE FROM notifications 
WHERE reference_id = @test_loan_id AND reference_type = 'LOAN';

DELETE FROM guarantors WHERE loan_id = @test_loan_id;
DELETE FROM loans WHERE id = @test_loan_id;
```

### Via API
```powershell
# Delete loan (as admin):
Invoke-RestMethod -Uri "http://localhost:9090/api/loans/123" `
  -Method Delete `
  -Headers @{"Authorization" = "Bearer $ADMIN_TOKEN"}
```

---

## 🐛 Troubleshooting

| Issue | Solution |
|-------|----------|
| "Login failed" | Check backend is running on port 9090 |
| "Not enough members" | Create test members via admin panel or SQL |
| "No loan products" | Create a loan product via admin panel |
| "Status not PENDING_TREASURER" | Check if additional approval steps are required |
| "No notifications" | Verify treasurer user exists in database |

---

## 📚 More Information

For detailed instructions and troubleshooting, see:
- **TEST_LOAN_CREATION_GUIDE.md** - Complete guide with all details
- **check-system-readiness.sql** - Verify your system is ready
- **create-test-loan-for-treasurer.ps1** - Automated API script
- **create-test-loan-direct.sql** - Direct database insertion script

---

## 🎯 Loan Workflow Overview

```
Application
    ↓
PENDING_GUARANTOR_APPROVAL
    ↓
Guarantors Accept
    ↓
PENDING_TREASURER ← You are here!
    ↓
Treasurer Approves
    ↓
APPROVED
    ↓
Treasurer Disburses
    ↓
DISBURSED
```

---

## 📞 Need Help?

1. Check backend logs: `backend/logs/`
2. Review database schema: `backend/src/main/resources/db/migration/`
3. Test API endpoints: `http://localhost:9090/swagger-ui/index.html`
4. Contact development team

---

**Last Updated:** 2026-08-03
**Version:** 1.0
