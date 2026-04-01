# Minet SACCO - Usage Guide

Step-by-step guide on how to use the system correctly.

## Table of Contents

1. [Staff Portal Setup](#staff-portal-setup)
2. [Member Portal Setup](#member-portal-setup)
3. [Member Registration](#member-registration)
4. [Loan Management](#loan-management)
5. [Savings Management](#savings-management)
6. [Bulk Processing](#bulk-processing)
7. [Reports & Analytics](#reports--analytics)
8. [Audit Trail](#audit-trail)

---

## Staff Portal Setup

### Initial Setup

1. **Start the Backend**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
   Backend runs on `http://localhost:8080`

2. **Start the Frontend**
   ```bash
   cd minetsacco-main
   npm run dev
   ```
   Frontend runs on `http://localhost:3000`

3. **Access Staff Portal**
   - Visit `http://localhost:3000/login` or `http://localhost:3000`
   - You'll see the staff login page

### Staff Login

1. **Enter Credentials**
   - Username: (created by admin)
   - Password: (set during user creation)

2. **Select Role**
   - ADMIN - Full system access
   - TREASURER - Accounting & transactions
   - LOAN_OFFICER - Loan processing
   - CREDIT_COMMITTEE - Loan approvals
   - TELLER - Member transactions
   - AUDITOR - Audit trail access

3. **Click Login**
   - You'll be redirected to the dashboard
   - Token is stored in browser session

### Dashboard Overview

The staff dashboard shows:
- **Quick Stats** - Total members, active loans, savings balance
- **Recent Transactions** - Latest deposits, withdrawals, loan disbursements
- **Pending Approvals** - Members and loans awaiting approval
- **Navigation Menu** - Links to all system features

---

## Member Portal Setup

### Member Access

**Web Portal:**
1. Visit `http://localhost:3000/member`
2. You'll see the member login page

**Mobile App (APK):**
1. Install APK on Android device
2. App opens directly to member login

### Member Login

1. **Enter Credentials**
   - Phone Number: (registered during member registration)
   - Password: (set during registration)

2. **Click Login**
   - Token is stored in device storage (web) or secure storage (mobile)
   - You'll be redirected to member dashboard

### Member Dashboard

The member dashboard shows:
- **Account Summary** - Savings balance, shares balance, contributions
- **Recent Transactions** - Latest deposits, withdrawals, loan repayments
- **Active Loans** - Current loans with balance and repayment schedule
- **Quick Actions** - Deposit, withdraw, apply for loan, view statements

---

## Member Registration

### Manual Registration (Staff Portal)

1. **Navigate to Members**
   - Click "Members" in sidebar
   - Click "Add Member" button

2. **Fill Member Form**
   - Full Name
   - Phone Number (unique)
   - Email
   - Date of Birth (YYYY-MM-DD format)
   - ID Number
   - Address
   - Occupation

3. **Submit**
   - Member created with status "PENDING"
   - Member appears in pending approvals list

4. **Approve Member**
   - Admin/Treasurer approves member
   - Member status changes to "ACTIVE"
   - Member can now log in

### Bulk Registration (Excel Upload)

1. **Download Template**
   - Go to Bulk Processing
   - Click "Download Member Registration Template"
   - Opens Excel file with columns:
     - Full Name
     - Phone Number
     - Email
     - Date of Birth
     - ID Number
     - Addres
s
     - Occupation

2. **Upload File**
   - Click "Upload Members" button
   - Select Excel file
   - System validates all rows
   - Shows validation report (errors/warnings)

3. **Review & Confirm**
   - Check validation results
   - Click "Process Batch" to create members
   - Members created with status "PENDING"

4. **Bulk Approval**
   - Go to Members list
   - Filter by "PENDING" status
   - Select multiple members
   - Click "Approve Selected"
   - All selected members activated

---

## Loan Management

### Loan Products Setup

1. **Navigate to Loan Products**
   - Click "Loan Products" in sidebar (Admin only)

2. **Create Loan Product**
   - Click "Add Loan Product"
   - Fill details:
     - Product Name (e.g., "Personal Loan")
     - Interest Rate (annual %)
     - Loan Term (months)
     - Minimum Amount
     - Maximum Amount
     - Processing Fee (%)

3. **Save**
   - Product available for loan applications

### Loan Eligibility Rules

1. **Navigate to Eligibility Rules**
   - Click "Loan Eligibility Rules" (Admin only)

2. **Set Rules**
   - Minimum Savings Balance
   - Minimum Contribution Period (months)
   - Maximum Loan Multiple (of savings)
   - Guarantor Requirements

3. **Save**
   - Rules applied to all new loan applications

### Member Loan Application

1. **Member Applies for Loan**
   - Member logs into portal
   - Click "Apply for Loan"
   - Select Loan Product
   - Enter Loan Amount
   - System checks eligibility:
     - Sufficient savings balance
     - Contribution period met
     - Loan amount within limits

2. **Add Guarantors** (if required)
   - Click "Add Guarantor"
   - Search member by phone/name
   - Select guarantor
   - Guarantor must have sufficient savings

3. **Submit Application**
   - Application created with status "PENDING"
   - Notification sent to LOAN_OFFICER

### Loan Officer Review

1. **Navigate to Loans**
   - Click "Loans" in sidebar
   - Filter by "PENDING" status

2. **Review Application**
   - Click on loan to view details
   - Check member info, guarantors, amount
   - Review eligibility calculations

3. **Recommend**
   - Click "Recommend for Approval" or "Reject"
   - Add comments
   - Application moves to CREDIT_COMMITTEE

### Credit Committee Approval

1. **Navigate to Loans**
   - Click "Loans" in sidebar
   - Filter by "RECOMMENDED" status

2. **Approve or Reject**
   - Review loan officer recommendation
   - Click "Approve" or "Reject"
   - Add approval comments

3. **Approved Loans**
   - Status changes to "APPROVED"
   - Notification sent to member
   - Loan ready for disbursement

### Loan Disbursement

1. **Navigate to Loans**
   - Click "Loans" in sidebar
   - Filter by "APPROVED" status

2. **Disburse Loan**
   - Click on loan
   - Click "Disburse Loan"
   - Confirm amount and account
   - System creates transaction
   - Loan status changes to "ACTIVE"
   - Member receives funds

### Loan Repayment

**Member Repayment:**
1. Member logs into portal
2. Click "Repay Loan"
3. Select loan to repay
4. Enter amount (minimum = monthly installment)
5. Choose payment method (M-Pesa or Bank)
6. Confirm payment
7. Transaction recorded, loan balance updated

**Bulk Repayment:**
1. Go to Bulk Processing
2. Download Loan Repayment Template
3. Fill with member phone, loan ID, amount
4. Upload file
5. System processes all repayments
6. Generates report

---

## Savings Management

### Deposit (Member)

1. **Member Initiates Deposit**
   - Click "Deposit" on dashboard
   - Enter amount
   - Choose payment method:
     - M-Pesa (automatic)
     - Bank Transfer (manual approval)

2. **M-Pesa Deposit**
   - System generates M-Pesa prompt
   - Member enters M-Pesa PIN
   - Payment confirmed
   - Deposit recorded immediately

3. **Bank Transfer Deposit**
   - System shows bank details
   - Member transfers funds
   - Teller receives notification
   - Teller verifies and approves
   - Deposit recorded

### Withdrawal (Member)

1. **Member Initiates Withdrawal**
   - Click "Withdraw" on dashboard
   - Enter amount
   - System checks:
     - Sufficient balance
     - Minimum balance maintained
     - Withdrawal limits

2. **Withdrawal Approval**
   - Teller receives notification
   - Teller verifies member identity
   - Teller approves withdrawal
   - Funds transferred to member

### Deposit Approval (Teller)

1. **Navigate to Deposits**
   - Click "Deposits" in sidebar (Teller only)
   - Filter by "PENDING" status

2. **Review Deposit**
   - Click on deposit
   - Verify member and amount
   - Check bank transfer confirmation

3. **Approve**
   - Click "Approve Deposit"
   - Deposit recorded to member account
   - Member receives notification

---

## Bulk Processing

### Bulk Member Registration

1. **Download Template**
   - Go to Bulk Processing
   - Click "Download Member Registration Template"

2. **Fill Template**
   - One member per row
   - Required columns: Name, Phone, Email, DOB, ID, Address, Occupation
   - Phone must be unique

3. **Upload & Process**
   - Click "Upload Members"
   - Select file
   - Review validation report
   - Click "Process Batch"

### Bulk Loan Applications

1. **Download Template**
   - Go to Bulk Processing
   - Click "Download Loan Application Template"

2. **Fill Template**
   - One application per row
   - Columns: Member Phone, Loan Product, Amount, Guarantor Phone

3. **Upload & Process**
   - Click "Upload Loan Applications"
   - Select file
   - Review validation report
   - Click "Process Batch"

### Bulk Loan Repayments

1. **Download Template**
   - Go to Bulk Processing
   - Click "Download Loan Repayment Template"

2. **Fill Template**
   - One repayment per row
   - Columns: Member Phone, Loan ID, Amount

3. **Upload & Process**
   - Click "Upload Repayments"
   - Select file
   - Review validation report
   - Click "Process Batch"

---

## Reports & Analytics

### Profit & Loss Report

1. **Navigate to Reports**
   - Click "Reports" in sidebar

2. **Generate P&L Report**
   - Select Period (Month/Year)
   - Click "Generate Report"
   - View:
     - Total Revenue (interest, fees)
     - Total Expenses (salaries, operations)
     - Net Profit/Loss
     - Breakdown by category

3. **Export Report**
   - Click "Export to Excel"
   - Report downloaded as Excel file

### Member Reports

1. **Member List Report**
   - Click "Reports" → "Members"
   - Filter by status, registration date
   - View member details
   - Export to Excel

2. **Member Contribution Report**
   - Click "Reports" → "Contributions"
   - View contribution history
   - Filter by period
   - Export to Excel

### Loan Reports

1. **Loan Portfolio Report**
   - Click "Reports" → "Loans"
   - View all loans by status
   - See total disbursed, outstanding
   - Export to Excel

2. **Loan Performance Report**
   - Click "Reports" → "Loan Performance"
   - View repayment rates
   - See overdue loans
   - Export to Excel

### Transaction Reports

1. **Transaction History**
   - Click "Reports" → "Transactions"
   - Filter by type, date range, member
   - View all deposits, withdrawals, repayments
   - Export to Excel

---

## Audit Trail

### View Audit Trail

1. **Navigate to Audit Trail**
   - Click "Audit Trail" in sidebar (Auditor only)

2. **Filter Audit Logs**
   - Filter by:
     - Action Type (CREATE, UPDATE, DELETE, APPROVE, REJECT)
     - Entity Type (Member, Loan, Deposit, etc.)
     - User
     - Date Range

3. **View Details**
   - Click on log entry
   - See:
     - What changed
     - Old value vs new value
     - Who made change
     - When change was made
     - IP address and device info

### Audit Report

1. **Generate Audit Report**
   - Click "Generate Report"
   - Select date range
   - Select action types to include
   - Click "Generate"

2. **Export Report**
   - Click "Export to Excel"
   - Report includes all audit entries
   - Useful for compliance and investigations

---

## Common Workflows

### Complete Loan Approval Workflow

1. Member applies for loan
2. Loan Officer reviews and recommends
3. Credit Committee approves
4. Treasurer disburses funds
5. Member receives loan
6. Member makes monthly repayments
7. Loan marked as PAID when complete

### Member Onboarding Workflow

1. Staff creates member (manual or bulk)
2. Admin approves member
3. Member receives login credentials
4. Member logs in and sets password
5. Member completes KYC documents
6. Member can now use all features

### Deposit Approval Workflow

1. Member initiates deposit (M-Pesa or Bank)
2. M-Pesa: Automatic approval
3. Bank: Teller receives notification
4. Teller verifies and approves
5. Deposit recorded to account
6. Member receives confirmation

---

## Troubleshooting

### Member Can't Login

1. Check member status is "ACTIVE"
2. Verify phone number is correct
3. Reset password if forgotten
4. Check browser cookies/cache

### Loan Application Rejected

1. Check eligibility requirements:
   - Minimum savings balance
   - Contribution period
   - Loan amount limits
2. Review guarantor requirements
3. Contact Loan Officer for details

### Deposit Not Appearing

1. Check deposit status in Deposits list
2. If pending, wait for Teller approval
3. If M-Pesa, check M-Pesa confirmation
4. Contact support if issue persists

### Reports Not Generating

1. Check date range is valid
2. Ensure data exists for period
3. Try refreshing page
4. Check browser console for errors

---

## Best Practices

1. **Regular Backups** - Backup database regularly
2. **Audit Reviews** - Review audit trail monthly
3. **Member Verification** - Verify member details during registration
4. **Loan Limits** - Set appropriate loan limits based on savings
5. **Repayment Monitoring** - Monitor overdue loans weekly
6. **Report Reviews** - Review P&L reports monthly
7. **User Access** - Regularly review user roles and permissions
8. **Data Validation** - Validate bulk uploads before processing

---

## Support & Contact

For issues or questions:
- Check the troubleshooting section above
- Review system logs in backend
- Contact system administrator
- Check audit trail for transaction details
