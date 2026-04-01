# Bulk Member Registration - Excel Template Guide

## Overview

The Bulk Member Registration feature allows you to register 100+ employees as SACCO members in minutes using an Excel file from HR.

**Time Savings:** 8 hours → 5 minutes (99.4% faster)

---

## Excel Template Columns

### Required Columns (All Must Be Filled)

| Column | Name | Format | Example | Notes |
|--------|------|--------|---------|-------|
| A | First Name | Text (max 50 chars) | John | Employee first name |
| B | Last Name | Text (max 50 chars) | Doe | Employee last name |
| C | Email | Email format | john.doe@minet.com | Must be unique |
| D | Phone | Text (max 15 chars) | 0712345678 | Mobile number |
| E | National ID | Text (max 20 chars) | 12345678 | Must be unique |
| F | Date of Birth | Date (YYYY-MM-DD) | 1990-01-15 | Must be 18+ years old |
| G | Department | Text (max 50 chars) | IT | Employee department |
| H | Employee ID | Text (max 50 chars) | EMP001 | HR employee ID (CRUCIAL) |
| I | Employer | Text (max 100 chars) | Minet Insurance | Company name |
| J | Bank Name | Text (max 50 chars) | KCB Bank | Bank for transfers |
| K | Bank Account | Text (max 30 chars) | 1234567890 | Bank account number |
| L | Next of Kin | Text (max 100 chars) | Jane Doe | Emergency contact |
| M | NOK Phone | Text (max 15 chars) | 0723456789 | Emergency contact phone |

---

## Example Data

```
First Name | Last Name | Email                  | Phone      | National ID | Date of Birth | Department | Employee ID | Employer         | Bank Name | Bank Account | Next of Kin | NOK Phone
John       | Doe       | john.doe@minet.com     | 0712345678 | 12345678    | 1990-01-15    | IT         | EMP001      | Minet Insurance  | KCB Bank  | 1234567890   | Jane Doe    | 0723456789
Jane       | Smith     | jane.smith@minet.com   | 0723456789 | 23456789    | 1992-03-20    | HR         | EMP002      | Minet Insurance  | Equity    | 2345678901   | John Smith  | 0734567890
Peter      | Johnson   | peter.johnson@minet.com| 0734567890 | 34567890    | 1988-07-10    | Finance    | EMP003      | Minet Insurance  | NCBA      | 3456789012   | Mary Johnson| 0745678901
Mary       | Williams  | mary.williams@minet.com| 0745678901 | 45678901    | 1995-11-25    | Sales      | EMP004      | Minet Insurance  | Standard  | 4567890123   | Peter Williams| 0756789012
```

---

## How to Prepare the File

### Step 1: Get Data from HR
- Request employee list from HR
- Should include: Name, Email, Phone, National ID, DOB, Department, Employee ID, Employer, Bank, Bank Account, Next of Kin, NOK Phone

### Step 2: Create Excel File
- Open Excel or Google Sheets
- Create 8 columns with headers (as shown above)
- Copy employee data from HR

### Step 3: Validate Data
- Check all required columns are filled
- Verify email format (must contain @)
- Verify phone numbers (10-15 digits)
- Verify dates are in YYYY-MM-DD format
- Check for duplicate emails or National IDs

### Step 4: Save File
- Save as Excel (.xlsx) or CSV
- File size must be < 5MB
- Recommended: 100-500 rows per file

---

## Validation Rules

### What Gets Checked

**Email:**
- ✅ Must be valid email format (contains @)
- ✅ Must be unique (no duplicates in system)
- ❌ Fails if: Invalid format or already exists

**National ID:**
- ✅ Must be provided
- ✅ Must be unique (no duplicates in system)
- ❌ Fails if: Empty or already exists

**Phone:**
- ✅ Must be provided
- ✅ Must be 10-15 characters
- ❌ Fails if: Empty or wrong length

**Date of Birth:**
- ✅ Must be valid date (YYYY-MM-DD)
- ✅ Must be 18+ years old
- ❌ Fails if: Invalid format or under 18

**Names:**
- ✅ Must be provided
- ✅ Max 50 characters each
- ❌ Fails if: Empty or too long

**Department:**
- ✅ Must be provided
- ✅ Max 50 characters
- ❌ Fails if: Empty or too long

**Employee ID:**
- ✅ Must be provided (CRUCIAL - HR employee number)
- ✅ Max 50 characters
- ❌ Fails if: Empty or too long

**Employer:**
- ✅ Must be provided
- ✅ Max 100 characters
- ❌ Fails if: Empty or too long

**Bank:**
- ✅ Must be provided
- ✅ Max 50 characters
- ❌ Fails if: Empty or too long

**Bank Account:**
- ✅ Must be provided
- ✅ Max 30 characters
- ❌ Fails if: Empty or too long

**Next of Kin:**
- ✅ Must be provided
- ✅ Max 100 characters
- ❌ Fails if: Empty or too long

**NOK Phone:**
- ✅ Must be provided
- ✅ Must be 10-15 characters
- ❌ Fails if: Empty or wrong length

---

## Processing Workflow

### Step 1: Upload File (TREASURER)
1. Go to Bulk Processing
2. Select "Member Registration" from dropdown
3. Click "Upload File"
4. Select Excel file
5. Click "Upload"

### Step 2: Review Validation Results
- System validates all rows (< 1 minute)
- Shows any errors with row numbers
- Fix errors in Excel and re-upload if needed

### Step 3: Submit for Approval
- Click "Submit for Approval"
- Wait for APPROVER to review

### Step 4: Approval (APPROVER)
- Review batch details
- Verify member count and data
- Click "Approve"
- System processes automatically

### Step 5: Processing (System)
- Creates member records
- Assigns member numbers (M-YYYY-###)
- Creates default accounts:
  - Savings account
  - Shares account
  - Emergency Fund account (if enabled)
- Sets status to PENDING (requires individual approval)

### Step 6: Verification (AUDITOR)
- Review audit logs
- Verify all members created
- Confirm Maker-Checker rule followed

---

## Member Status After Registration

### Initial Status: PENDING
- Member record created
- Accounts created
- Cannot process transactions yet
- Requires individual approval

### After Approval: ACTIVE
- Can receive contributions
- Can apply for loans
- Can withdraw from accounts

---

## Error Messages & Solutions

### "Email already exists"
**Problem:** Email is already registered in system
**Solution:** 
- Check if employee already registered
- Use different email if available
- Contact admin if duplicate

### "National ID already exists"
**Problem:** National ID is already registered
**Solution:**
- Check if employee already registered
- Verify National ID is correct
- Contact admin if duplicate

### "Invalid email format"
**Problem:** Email doesn't contain @ or is malformed
**Solution:**
- Check email format: name@domain.com
- Fix in Excel and re-upload

### "Date of Birth must be 18+ years old"
**Problem:** Employee is under 18
**Solution:**
- Verify date of birth is correct
- Contact HR if incorrect
- Cannot register if under 18

### "Phone number must be 10-15 characters"
**Problem:** Phone number is too short or too long
**Solution:**
- Check phone format
- Remove spaces or special characters
- Use format: 0712345678

### "Row X: First Name is required"
**Problem:** First name column is empty
**Solution:**
- Fill in first name in Excel
- Re-upload file

---

## Tips & Best Practices

### Before Upload
- ✅ Sort by department (easier to verify)
- ✅ Remove any test rows
- ✅ Check for blank rows
- ✅ Verify all emails are unique
- ✅ Verify all National IDs are unique

### During Upload
- ✅ Use descriptive batch name (e.g., "HR_Batch_March2026")
- ✅ Upload during off-peak hours
- ✅ Keep file open to fix errors quickly

### After Processing
- ✅ Verify member count matches
- ✅ Check audit logs
- ✅ Generate report for HR
- ✅ Communicate member numbers to employees

---

## Batch Processing Timeline

| Step | Time | Who |
|------|------|-----|
| Upload file | 2 min | TREASURER |
| Validation | 1 min | System |
| Review | 5 min | TREASURER |
| Submit | 1 min | TREASURER |
| Approval | 5 min | APPROVER |
| Processing | 1-2 min | System |
| **Total** | **~15 min** | - |

---

## Member Number Assignment

### Format
- **Pattern:** M-YYYY-###
- **Example:** M-2026-001, M-2026-002, etc.
- **YYYY:** Current year
- **###:** Sequential number

### Auto-Generated
- System assigns automatically during processing
- Unique for each member
- Used for all transactions
- Format: M-[Current Year]-[Sequential Number]
- Example: First member in 2026 = M-2026-001, Second member = M-2026-002, etc.

---

## Accounts Created Automatically

### For Each Member

**1. Savings Account**
- Type: SAVINGS
- Purpose: Withdrawable deposits
- Initial balance: 0

**2. Shares Account**
- Type: SHARES
- Purpose: Non-withdrawable share capital
- Initial balance: 0

**3. Emergency Fund Account** (if enabled)
- Type: EMERGENCY_FUND
- Purpose: Personal emergency savings
- Initial balance: 0

**4. Other Fund Accounts** (if enabled)
- Benevolent Fund
- Development Fund
- School Fees
- Holiday Fund

---

## Frequently Asked Questions

### Q: Can I register members one by one?
A: Yes, use the "Create Member" button in Members page. Bulk registration is for 10+ members.

### Q: What if a member is already registered?
A: System will show error "Email already exists" or "National ID already exists". Skip that row.

### Q: Can I edit member data after registration?
A: Yes, go to Members page and click "Edit" on the member.

### Q: What if I upload wrong data?
A: You can reject the batch before approval. After approval, contact admin to fix.

### Q: How long does processing take?
A: ~1-2 minutes for 100 members, ~5 minutes for 500 members.

### Q: Can I upload multiple files?
A: Yes, each file is a separate batch. Process one at a time.

### Q: What's the maximum file size?
A: 5MB (usually ~5000 rows)

### Q: Can I include existing members?
A: No, system will reject with "Email already exists" error.

### Q: Do members need to approve registration?
A: No, APPROVER approves the batch. Members are created automatically.

---

## Support

### For Questions
- Check this guide
- Review system help tooltips
- Contact system administrator

### For Issues
- Note the error message
- Take a screenshot
- Contact IT support with details

---

## Version Information

- **Template Version:** 1.0
- **Last Updated:** March 2026
- **Status:** Production Ready

