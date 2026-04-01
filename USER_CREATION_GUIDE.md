# User Creation Guide - Minet Sacco System

## Overview
User creation is the **FIRST STEP** in system setup. The system uses role-based access control with a hierarchy where Admin can create other roles.

## Role Hierarchy

### Available Roles
1. **ADMIN** - System Administrator (Full access)
2. **TREASURER** - Finance/Accounting (Can create Teller, Customer Support)
3. **LOAN_OFFICER** - Loan Processing
4. **CREDIT_COMMITTEE** - Loan Approval
5. **AUDITOR** - Compliance/Audit
6. **TELLER** - Data Entry (Created by Treasurer)
7. **CUSTOMER_SUPPORT** - Support (Created by Treasurer)

### Role Hierarchy
```
ADMIN (can create all roles)
├── TREASURER (can create Teller, Customer Support)
├── LOAN_OFFICER
├── CREDIT_COMMITTEE
└── AUDITOR
```

---

## Step-by-Step User Creation

### Step 1: Login as Admin
- **URL**: http://localhost:5173/login
- **Username**: `admin`
- **Password**: `password`
- **Expected**: Dashboard loads, you see "User Management" in sidebar

### Step 2: Navigate to User Management
- Click **Admin** menu in sidebar
- Select **User Management**
- You should see "Staff Users" tab and "Create Staff User" button

### Step 3: Create First User - Treasurer

**Click "Create Staff User" button**

Fill in the form:
- **Username**: `treasurer`
- **Email**: `treasurer@minetsacco.com`
- **Password**: `password` (or secure password)
- **Role**: Select **TREASURER** from dropdown
- **Click**: Create button

**Expected Result**: 
- Toast message: "User treasurer created successfully"
- Treasurer appears in users list

### Step 4: Create Second User - Credit Committee

**Click "Create Staff User" button again**

Fill in the form:
- **Username**: `credit_committee`
- **Email**: `credit_committee@minetsacco.com`
- **Password**: `password`
- **Role**: Select **CREDIT_COMMITTEE** from dropdown
- **Click**: Create button

**Expected Result**: 
- Toast message: "User credit_committee created successfully"
- Credit Committee appears in users list

### Step 5: Create Third User - Loan Officer

**Click "Create Staff User" button**

Fill in the form:
- **Username**: `loan_officer`
- **Email**: `loan_officer@minetsacco.com`
- **Password**: `password`
- **Role**: Select **LOAN_OFFICER** from dropdown
- **Click**: Create button

### Step 6: Create Fourth User - Auditor

**Click "Create Staff User" button**

Fill in the form:
- **Username**: `auditor`
- **Email**: `auditor@minetsacco.com`
- **Password**: `password`
- **Role**: Select **AUDITOR** from dropdown
- **Click**: Create button

### Step 7: Create Fifth User - Teller (via Treasurer)

**Logout as Admin**
- Click profile icon (top right)
- Select **Logout**

**Login as Treasurer**
- **Username**: `treasurer`
- **Password**: `password`

**Navigate to User Management**
- Click **Admin** menu
- Select **User Management**
- Note: You can only create Teller and Customer Support roles

**Click "Create Staff User" button**

Fill in the form:
- **Username**: `teller`
- **Email**: `teller@minetsacco.com`
- **Password**: `password`
- **Role**: Select **TELLER** from dropdown
- **Click**: Create button

### Step 8: Create Sixth User - Customer Support (via Treasurer)

**Click "Create Staff User" button**

Fill in the form:
- **Username**: `customer_support`
- **Email**: `support@minetsacco.com`
- **Password**: `password`
- **Role**: Select **CUSTOMER_SUPPORT** from dropdown
- **Click**: Create button

---

## User Creation Summary Table

| # | Username | Email | Password | Role | Created By |
|---|----------|-------|----------|------|-----------|
| 1 | admin | admin@minetsacco.com | password | ADMIN | System |
| 2 | treasurer | treasurer@minetsacco.com | password | TREASURER | admin |
| 3 | credit_committee | credit_committee@minetsacco.com | password | CREDIT_COMMITTEE | admin |
| 4 | loan_officer | loan_officer@minetsacco.com | password | LOAN_OFFICER | admin |
| 5 | auditor | auditor@minetsacco.com | password | AUDITOR | admin |
| 6 | teller | teller@minetsacco.com | password | TELLER | treasurer |
| 7 | customer_support | support@minetsacco.com | password | CUSTOMER_SUPPORT | treasurer |

---

## Verification Checklist

After creating all users, verify:

- [ ] All 7 users appear in User Management list
- [ ] Each user has correct role assigned
- [ ] Each user shows correct "Created By" username
- [ ] Can login as each user with their credentials
- [ ] Each user sees appropriate menu items for their role

### Login Test for Each User

**Admin**
- Login: admin / password
- Should see: All admin options

**Treasurer**
- Login: treasurer / password
- Should see: Bulk Processing, Reports, User Management (limited)

**Credit Committee**
- Login: credit_committee / password
- Should see: Loans, Bulk Processing (loan approval only)

**Loan Officer**
- Login: loan_officer / password
- Should see: Loans, Members

**Auditor**
- Login: auditor / password
- Should see: Reports, Audit logs

**Teller**
- Login: teller / password
- Should see: Members, Savings, limited options

**Customer Support**
- Login: customer_support / password
- Should see: Members, limited support options

---

## User Deletion (Maker-Checker)

### How to Delete a User

1. **Login as Admin**
2. **Go to User Management**
3. **Find user to delete**
4. **Click trash icon** next to user
5. **Enter deletion reason** (required)
6. **Click "Request Deletion"**
7. **Another Admin must approve** (Maker-Checker principle)

### Approve Deletion

1. **Login as different Admin**
2. **Go to User Management**
3. **Click "Pending Deletions" tab**
4. **Review deletion request**
5. **Click "Approve" or "Reject"**
6. **If approved**: User is deleted from system

---

## Important Notes

### Security
- Passwords should be strong in production
- Each user should have unique email
- Usernames are case-sensitive
- Deletion requires approval from another admin (Maker-Checker)

### Role Permissions
- **Admin**: Can create all roles, approve deletions
- **Treasurer**: Can create Teller and Customer Support only
- **Other roles**: Cannot create users

### Access Control
- Users only see menu items for their role
- Users can only access endpoints for their role
- Unauthorized access returns 403 Forbidden

---

## Testing Flow After User Creation

### 1. Test Member Registration (Treasurer)
- Login as Treasurer
- Go to Bulk Processing
- Upload member batch

### 2. Test Loan Application (Treasurer)
- Login as Treasurer
- Go to Bulk Processing
- Upload loan batch

### 3. Test Loan Approval (Credit Committee)
- Login as Credit Committee
- Go to Bulk Processing
- Approve loans individually

### 4. Test Loan Disbursement (Treasurer)
- Login as Treasurer
- Go to Bulk Processing
- Disburse approved loans

### 5. Test Reports (Treasurer/Admin)
- Login as Treasurer or Admin
- Go to Reports
- Generate Profit & Loss report

---

## Troubleshooting

### Issue: "Create Staff User" button not visible
**Solution**: 
- Verify you're logged in as Admin or Treasurer
- Check your role has permission to create users
- Refresh page

### Issue: Cannot create user with that role
**Solution**:
- Your role doesn't have permission to create that role
- Only Admin can create all roles
- Treasurer can only create Teller and Customer Support

### Issue: User creation fails with error
**Solution**:
- Check username is unique (not already taken)
- Check email is valid format
- Check password is not empty
- Check role is selected

### Issue: Cannot login with new user
**Solution**:
- Verify username and password are correct
- Check user is enabled (should be by default)
- Try logging out and back in
- Clear browser cache

---

## Next Steps After User Creation

1. ✅ Create all 7 users (this guide)
2. ⬜ Clean database (CLEAN_DATABASE_COMPLETE.sql)
3. ⬜ Run E2E tests (E2E_TESTING_GUIDE.md)
4. ⬜ Prepare presentation (PRESENTATION_QUICK_REFERENCE.md)
