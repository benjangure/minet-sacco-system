# Minet SACCO System - Operating Guide

## 📋 Table of Contents
1. [System Overview](#system-overview)
2. [User Roles](#user-roles)
3. [Getting Started](#getting-started)
4. [Daily Operations](#daily-operations)
5. [Troubleshooting](#troubleshooting)

---

## System Overview

### What This System Does

The Minet SACCO System automates bulk financial processing for employee savings and loans.

**Main Capability: Bulk Monthly Contributions**
- Process 500+ employee contributions in 3 minutes
- Automatic validation and error detection
- Secure approval workflow (Maker-Checker)
- Complete audit trail for compliance

**Secondary Capabilities:**
- Individual member account management
- Individual loan application and tracking
- Configurable savings funds
- Role-based access control
- Comprehensive audit logs

### Key Features

✅ **Fast Processing** - 500 employees in 3 minutes (vs 16 hours manual)
✅ **Secure** - Maker-Checker workflow prevents fraud
✅ **Flexible** - Admin can enable/disable optional funds
✅ **Auditable** - Complete history of all transactions
✅ **Reliable** - Automatic validation catches errors

---

## User Roles

### 1. ADMIN (System Administrator)

**Who:** 1-2 people (Finance Manager, IT Manager)

**Responsibilities:**
- Configure which funds are available
- Create and manage user accounts
- View system audit logs
- Monitor system health

**Access:**
- Settings page (fund configuration)
- User management
- Audit logs
- System reports

**Daily Tasks:**
- Check system status
- Create new user accounts as needed
- Configure funds (rarely - only when policy changes)

**Cannot Do:**
- Process bulk uploads
- Approve submissions
- View member data (unless also AUDITOR)

---

### 2. TREASURER (Bulk Processor)

**Who:** 2-3 people (Finance team members)

**Responsibilities:**
- Collect data from HR
- Prepare Excel files
- Upload bulk files
- Validate data
- Submit for approval

**Access:**
- Bulk processing page
- Excel template download
- View own submissions
- Member management (individual)
- Loan management (individual)

**Daily Tasks:**
1. Receive data from HR
2. Prepare Excel file using template
3. Upload file to system
4. Review validation results
5. Submit for approval
6. Wait for APPROVER to review

**Cannot Do:**
- Approve own submissions (security feature)
- Access other TREASURER's submissions
- Configure system settings
- View audit logs

**Example Workflow:**
```
Monday 9:00 AM
├─ Receive employee contribution data from HR
├─ Open Excel template
├─ Fill in employee data
├─ Upload to system
├─ System validates (< 1 minute)
├─ Review any errors
├─ Submit for approval
└─ Wait for APPROVER

Monday 10:00 AM
└─ APPROVER reviews and approves
   └─ Processing completes (< 30 seconds)
   └─ All accounts updated
   └─ Audit trail recorded
```

---

### 3. APPROVER (Bulk Authorizer)

**Who:** 2-3 people (Senior Finance staff, Credit Committee)

**Responsibilities:**
- Review submitted bulk files
- Validate data accuracy
- Approve or reject submissions
- Ensure compliance

**Access:**
- Bulk processing page (approval section)
- View pending submissions
- View submission details
- View audit logs
- Member management (individual)
- Loan management (individual)

**Daily Tasks:**
1. Check for pending submissions
2. Review submission details
3. Verify data accuracy
4. Approve or reject
5. Document decision

**Cannot Do:**
- Approve own submissions (Maker-Checker rule)
- Upload files
- Configure system settings
- Modify submitted data

**Maker-Checker Rule:**
- If TREASURER A uploads a file, only APPROVER B/C can approve it
- TREASURER A cannot approve their own file
- This prevents fraud and ensures oversight

**Example Workflow:**
```
Monday 10:00 AM
├─ APPROVER logs in
├─ Sees 1 pending submission from TREASURER
├─ Clicks to view details
├─ Reviews:
│  ├─ Number of records: 500
│  ├─ Total amount: KES 2,500,000
│  ├─ Validation status: All passed
│  └─ Submitted by: John (TREASURER)
├─ Approves submission
└─ Processing starts automatically

Monday 10:01 AM
└─ Processing completes
   ├─ 500 accounts updated
   ├─ Audit trail recorded
   └─ Confirmation email sent
```

---

### 4. AUDITOR (Compliance Monitor)

**Who:** 1-2 people (Internal Audit, Compliance Officer)

**Responsibilities:**
- Monitor all transactions
- Review audit logs
- Ensure compliance
- Generate reports
- Investigate discrepancies

**Access:**
- View all transactions (read-only)
- View all audit logs
- View all member data (read-only)
- View all loan data (read-only)
- Generate reports

**Daily Tasks:**
1. Review audit logs
2. Check for unusual activity
3. Verify compliance
4. Generate reports as needed

**Cannot Do:**
- Modify any data
- Approve submissions
- Upload files
- Configure system

**Example Workflow:**
```
Weekly Audit
├─ AUDITOR logs in
├─ Views audit logs for the week
├─ Checks:
│  ├─ All submissions approved by different users
│  ├─ No rejected submissions
│  ├─ All amounts within policy
│  └─ No data modifications
├─ Generates compliance report
└─ Files report for records
```

---

## Getting Started

### First-Time Setup

#### Step 1: Admin Creates User Accounts

**As ADMIN:**
1. Login to system
2. Go to User Management
3. Click "Create User"
4. Fill in:
   - Username (e.g., john_treasurer)
   - Email (e.g., john@minet.com)
   - Role (TREASURER, APPROVER, or AUDITOR)
   - Password (temporary)
5. Click "Create"
6. Share credentials with user

#### Step 2: Configure Funds

**As ADMIN:**
1. Go to Settings
2. Click "Fund Configuration"
3. See available funds:
   - Emergency Fund (always enabled)
   - Benevolent Fund (optional)
   - Development Fund (optional)
   - School Fees (optional)
   - Holiday Fund (optional)
4. Enable/disable as needed
5. Click "Save"

**Note:** Only enabled funds appear in Excel template

#### Step 3: Test System

**As TREASURER:**
1. Login with temporary password
2. Change password
3. Go to Bulk Processing
4. Download Excel template
5. Fill with test data (5-10 rows)
6. Upload file
7. Review validation results
8. Submit for approval

**As APPROVER:**
1. Login
2. Go to Bulk Processing
3. View pending submission
4. Review details
5. Click "Approve"
6. Verify processing completed

---

## Daily Operations

### Scenario 1: Monthly Contribution Processing

**Timeline:** Every month (e.g., last Friday)

**Step 1: TREASURER Prepares Data (9:00 AM)**
```
1. Receive employee contribution data from HR
2. Open Excel template (download from system)
3. Fill in columns:
   - Member Number (from HR)
   - Savings amount
   - Shares amount
   - Loan Repayment amount
   - Loan Number (if applicable)
   - Emergency Fund amount (if enabled)
   - Other enabled funds
4. Save file
5. Upload to system
6. System validates (< 1 minute)
7. Review any errors
8. Fix errors if needed
9. Submit for approval
```

**Step 2: APPROVER Reviews (10:00 AM)**
```
1. Login to system
2. Go to Bulk Processing
3. Click pending submission
4. Review:
   - Number of records
   - Total amounts
   - Validation status
   - Submitted by (verify it's not you)
5. Click "Approve"
6. System processes (< 30 seconds)
7. Confirmation shows success
```

**Step 3: AUDITOR Verifies (Next Day)**
```
1. Login to system
2. Go to Audit Logs
3. Filter by date (yesterday)
4. Verify:
   - Submission uploaded by TREASURER
   - Approved by different APPROVER
   - All 500 records processed
   - No errors
5. Generate report
6. File for records
```

### Scenario 2: Individual Member Account

**When:** Anytime (new member, account adjustment)

**As TREASURER:**
```
1. Go to Members
2. Click "Create Member" or "Edit Member"
3. Fill in details:
   - Name
   - Email
   - Phone
   - National ID
   - Department
4. Click "Save"
5. System auto-creates accounts:
   - Savings account
   - Shares account
   - Emergency Fund account (if enabled)
6. Member can now receive contributions
```

### Scenario 3: Individual Loan Application

**When:** Member applies for loan

**As TREASURER:**
```
1. Go to Loans
2. Click "New Loan Application"
3. Fill in:
   - Member
   - Loan product
   - Amount
   - Purpose
   - Guarantors
4. Click "Submit"
5. System calculates repayment schedule
6. Loan status: PENDING_APPROVAL
```

**As APPROVER:**
```
1. Go to Loans
2. Click pending loan
3. Review:
   - Member details
   - Loan amount
   - Repayment schedule
   - Guarantors
4. Click "Approve" or "Reject"
5. If approved:
   - Loan status: APPROVED
   - Funds disbursed to member account
   - Repayment schedule active
```

---

## Troubleshooting

### Problem: "Cannot Approve - You Submitted This"

**Cause:** Maker-Checker rule - you cannot approve your own submission

**Solution:** Ask another APPROVER to approve it

**Why:** Security feature to prevent fraud

---

### Problem: "Excel Template Missing Columns"

**Cause:** Admin disabled some funds

**Solution:** This is correct behavior
- Only enabled funds appear in template
- Fill in only the columns shown
- System will process only enabled funds

**Example:**
- If only Emergency Fund is enabled
- Template shows: Member Number, Savings, Shares, Loan Repayment, Emergency Fund
- Other fund columns are hidden

---

### Problem: "Validation Failed - 50 Errors"

**Cause:** Data format issues

**Common Errors:**
- Member number not found
- Amount is not a number
- Loan number doesn't exist
- Duplicate member in file

**Solution:**
1. Download error report
2. Fix errors in Excel
3. Re-upload file
4. Repeat until all errors fixed

---

### Problem: "System Won't Start"

**Cause:** Database connection issue

**Solution:**
1. Verify MySQL is running
2. Check database credentials in application.properties
3. Verify database exists: `sacco_db`
4. Check database migrations ran successfully
5. Restart application

---

### Problem: "Forgot Password"

**Solution:**
1. Click "Forgot Password" on login page
2. Enter email
3. Check email for reset link
4. Click link and set new password
5. Login with new password

---

## Quick Reference

### User Roles at a Glance

| Task | ADMIN | TREASURER | APPROVER | AUDITOR |
|------|-------|-----------|----------|---------|
| Upload bulk file | ❌ | ✅ | ❌ | ❌ |
| Approve bulk file | ❌ | ❌ | ✅ | ❌ |
| Configure funds | ✅ | ❌ | ❌ | ❌ |
| Create users | ✅ | ❌ | ❌ | ❌ |
| View audit logs | ✅ | ❌ | ✅ | ✅ |
| View all data | ✅ | ❌ | ✅ | ✅ |
| Modify data | ✅ | ✅ | ❌ | ❌ |

### Excel Template Columns

**Always Included:**
- Member Number
- Savings
- Shares
- Loan Repayment
- Loan Number

**Conditionally Included (if enabled):**
- Emergency Fund
- Benevolent Fund
- Development Fund
- School Fees
- Holiday Fund

### Processing Timeline

| Step | Time | Who |
|------|------|-----|
| Upload file | 2 min | TREASURER |
| Validation | 1 min | System |
| Review | 5 min | TREASURER |
| Submit | 1 min | TREASURER |
| Approval | 5 min | APPROVER |
| Processing | 30 sec | System |
| **Total** | **~15 min** | - |

---

## Support

### For Questions:
1. Check this guide
2. Review system help tooltips
3. Contact system administrator

### For Issues:
1. Note the error message
2. Take a screenshot
3. Contact IT support with details

---

## System Capabilities Summary

### ✅ What the System CAN Do

**Bulk Processing:**
- Process 500+ contributions in 3 minutes
- Automatic validation
- Maker-Checker approval
- Complete audit trail

**Individual Management:**
- Create/edit members
- Create/edit loans
- View transaction history
- Generate reports

**Configuration:**
- Enable/disable funds
- Manage users
- Configure system settings

**Compliance:**
- Role-based access control
- Audit logging
- Maker-Checker workflow
- Data encryption

### ❌ What the System CANNOT Do

- Bulk member registration (not implemented)
- Bulk loan applications (not implemented)
- Mobile app (web-only)
- SMS notifications (email only)
- Integration with HR system (manual import)

---

## Version Information

- **System Version:** 1.0
- **Last Updated:** March 2026
- **Status:** Production Ready for Bulk Contributions

