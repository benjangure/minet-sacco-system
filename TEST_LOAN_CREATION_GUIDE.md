# Test Loan Creation Guide - Treasurer Notification

This guide provides two methods to create a test loan that goes through the complete workflow to the treasurer, allowing you to verify that treasurer notifications are working correctly.

## Overview

The loan workflow in the SACCO system follows these steps:

1. **Member applies for loan** → Status: `PENDING_GUARANTOR_APPROVAL`
2. **Guarantors accept/reject** → Status: `PENDING_LOAN_OFFICER_REVIEW` or `PENDING_TREASURER`
3. **Loan Officer reviews** (optional) → Status: `PENDING_CREDIT_COMMITTEE` or `PENDING_TREASURER`
4. **Credit Committee approves** (optional) → Status: `PENDING_TREASURER`
5. **Treasurer approves** → Status: `APPROVED`
6. **Treasurer disburses** → Status: `DISBURSED`

This guide helps you create a test loan that reaches **PENDING_TREASURER** status so you can verify treasurer notifications.

---

## Method 1: Using the API (Recommended)

### Prerequisites

1. Backend server running on `http://localhost:9090`
2. At least 3 members in the database (1 applicant + 2 guarantors)
3. At least 1 loan product configured
4. Admin credentials (default: `admin` / `admin123`)

### Steps

1. **Open PowerShell** in the project directory

2. **Run the test script**:
   ```powershell
   .\create-test-loan-for-treasurer.ps1
   ```

3. **Watch the output** - the script will:
   - Login as admin
   - Find test members
   - Get loan products
   - Apply for a loan
   - Auto-approve all guarantors
   - Verify the loan reaches PENDING_TREASURER status

4. **Expected Output**:
   ```
   ========================================
     FINAL LOAN STATUS
   ========================================
     Loan ID: 123
     Loan Number: LN20261234
     Status: PENDING_TREASURER
     Applicant: John Doe
     Amount: KES 50000
     Monthly Repayment: KES 4583.33
   ========================================
   
   ✓✓✓ SUCCESS! ✓✓✓
   The loan is now PENDING_TREASURER status.
   The treasurer should receive a notification!
   ```

### Troubleshooting Method 1

**Issue: "Login failed"**
- Check that the backend is running on port 9090
- Verify admin credentials in the script
- Check `application.properties` for any auth configuration

**Issue: "Not enough members in database"**
- Use the admin panel to create test members
- Or use Method 2 (SQL) to insert test members first

**Issue: "Loan application failed"**
- Check that members have sufficient savings balance
- Verify loan product configuration (min/max amounts)
- Check backend logs for detailed error messages

**Issue: "Loan status is not PENDING_TREASURER"**
- The workflow might include additional approval steps (Loan Officer, Credit Committee)
- Check the loan status to see where it's stuck
- You may need to manually approve intermediate steps

---

## Method 2: Direct Database Insertion (SQL)

### Prerequisites

1. Access to MySQL database (via phpMyAdmin, MySQL Workbench, or command line)
2. Database name: `sacco_db`
3. At least 3 members and 1 loan product in the database

### Steps

1. **Open MySQL client** (phpMyAdmin, MySQL Workbench, or command line)

2. **Select the database**:
   ```sql
   USE sacco_db;
   ```

3. **Find test members**:
   ```sql
   SELECT id, member_number, first_name, last_name, email
   FROM members 
   LIMIT 5;
   ```
   
   Note down at least 3 member IDs

4. **Find a loan product**:
   ```sql
   SELECT id, name, interest_rate, max_amount
   FROM loan_products
   LIMIT 1;
   ```
   
   Note down the loan product ID

5. **Edit the SQL script**:
   - Open `create-test-loan-direct.sql`
   - Update these variables with your actual IDs:
     ```sql
     SET @loan_applicant_id = 1;    -- Replace with actual member ID
     SET @guarantor1_id = 2;         -- Replace with actual member ID
     SET @guarantor2_id = 3;         -- Replace with actual member ID
     SET @loan_product_id = 1;       -- Replace with actual loan product ID
     ```

6. **Run the entire script** in your MySQL client

7. **Verify the results** - the script will show:
   - Loan creation confirmation
   - Guarantor records
   - Loan status update
   - Treasurer notification

### Troubleshooting Method 2

**Issue: "Cannot insert into loans table"**
- Check that all referenced IDs exist (member IDs, loan product ID)
- Verify that the loans table schema is correct
- Check for any foreign key constraints

**Issue: "No notifications created"**
- Check if the notifications table exists
- Verify that a treasurer user exists in the users table
- The script will skip this step if the table doesn't exist

**Issue: "Cannot find members"**
- Create test members first using the admin panel or SQL:
  ```sql
  INSERT INTO members (member_number, first_name, last_name, email, phone, created_at)
  VALUES 
  ('M001', 'John', 'Doe', 'john@test.com', '0712345678', NOW()),
  ('M002', 'Jane', 'Smith', 'jane@test.com', '0723456789', NOW()),
  ('M003', 'Bob', 'Wilson', 'bob@test.com', '0734567890', NOW());
  ```

---

## Verifying Treasurer Notifications

### Method 1: Check the Database

```sql
-- View all treasurer notifications
SELECT 
    n.id,
    n.title,
    n.message,
    n.is_read,
    n.created_at,
    u.username as treasurer
FROM notifications n
JOIN users u ON n.user_id = u.id
WHERE u.role = 'TREASURER'
  AND n.reference_type = 'LOAN'
ORDER BY n.created_at DESC
LIMIT 10;
```

### Method 2: Login as Treasurer

1. Open the frontend application
2. Login with treasurer credentials
3. Check the notifications bell icon
4. Navigate to the loan approval queue
5. You should see the test loan in PENDING_TREASURER status

### Method 3: Check Backend Logs

Look for log entries like:
```
Notifying users with role TREASURER about loan approval
Loan LN20261234 status updated to PENDING_TREASURER
```

---

## Next Steps After Creating Test Loan

Once the test loan is created and in PENDING_TREASURER status:

1. **Login as Treasurer**
   - Username: (check your users table or use admin to create treasurer user)
   - Navigate to loan approvals

2. **Review the Notification**
   - Should appear in notifications panel
   - Click to view loan details

3. **Approve the Loan** (Optional)
   ```http
   POST /api/loans/approve
   Content-Type: application/json
   
   {
     "loanId": 123,
     "approved": true,
     "comments": "Approved for testing"
   }
   ```

4. **Disburse the Loan** (Optional)
   ```http
   POST /api/loans/disburse/123
   ```

---

## Cleanup Test Data

### Remove via API
```powershell
# Delete the test loan (as admin)
$loanId = 123
Invoke-RestMethod -Uri "http://localhost:9090/api/loans/$loanId" `
    -Method Delete `
    -Headers @{"Authorization" = "Bearer $JWT_TOKEN"}
```

### Remove via SQL
```sql
-- Replace @new_loan_id with your test loan ID
SET @test_loan_id = 123;

DELETE FROM notifications 
WHERE reference_id = @test_loan_id AND reference_type = 'LOAN';

DELETE FROM guarantors 
WHERE loan_id = @test_loan_id;

DELETE FROM loans 
WHERE id = @test_loan_id;

SELECT '✓ Test loan cleaned up' as status;
```

---

## Common Loan Statuses

| Status | Description |
|--------|-------------|
| `PENDING_GUARANTOR_APPROVAL` | Waiting for guarantors to accept/reject |
| `PENDING_LOAN_OFFICER_REVIEW` | Loan officer needs to review |
| `PENDING_CREDIT_COMMITTEE` | Credit committee needs to approve |
| `PENDING_TREASURER` | **Treasurer needs to approve** ← Target status |
| `APPROVED` | Approved but not yet disbursed |
| `DISBURSED` | Money has been released to member |
| `REJECTED` | Loan was rejected at any stage |

---

## API Endpoints Reference

### Apply for Loan
```http
POST /api/loans/apply
Content-Type: application/json
Authorization: Bearer {token}

{
  "memberId": 1,
  "loanProductId": 1,
  "amount": 50000,
  "termMonths": 12,
  "purpose": "Test loan",
  "guarantors": [
    {
      "guarantorMemberId": 2,
      "pledgedAmount": 25000
    },
    {
      "guarantorMemberId": 3,
      "pledgedAmount": 25000
    }
  ]
}
```

### Guarantor Response
```http
POST /api/loans/guarantors/respond
Content-Type: application/json
Authorization: Bearer {token}

{
  "guarantorId": 1,
  "accepted": true,
  "comments": "I guarantee this loan"
}
```

### Approve Loan (Treasurer)
```http
POST /api/loans/approve
Content-Type: application/json
Authorization: Bearer {token}

{
  "loanId": 123,
  "approved": true,
  "comments": "Approved"
}
```

### Disburse Loan (Treasurer)
```http
POST /api/loans/disburse/{loanId}
Authorization: Bearer {token}
```

### Get Loan Details
```http
GET /api/loans/{loanId}
Authorization: Bearer {token}
```

---

## Support

If you encounter issues:

1. Check backend logs at `backend/logs/`
2. Verify database schema with Flyway migrations
3. Ensure all services are running (backend on port 9090)
4. Check that users have correct roles assigned

For further assistance, contact the development team.
