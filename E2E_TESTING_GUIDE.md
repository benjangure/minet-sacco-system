# End-to-End Testing Guide - Minet Sacco System

## Pre-Test Setup

### 1. Clean Database
Run the database cleanup script to start fresh:
```bash
# Execute in your database client (MySQL Workbench, DBeaver, etc.)
# File: CLEAN_DATABASE.sql
```

### 2. Rebuild and Restart Backend
```bash
cd backend
mvn clean package -DskipTests
# Stop any running backend instance
java -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar
```

### 3. Start Frontend
```bash
cd minetsacco-main
npm run dev
```

---

## Test Scenarios

### Scenario 1: Member Registration (Treasurer)

**Objective**: Register members in bulk

**Steps**:
1. Login as **Treasurer** (username: `treasurer`, password: `password`)
2. Navigate to **Bulk Processing**
3. Click **Download Template** → Select **Member Registration**
4. Fill in member data (at least 5 members):
   - First Name, Last Name, Email, Phone
   - National ID (8 digits), Date of Birth (YYYY-MM-DD)
   - Department, Employer, Bank details
5. Upload the file
6. Verify all members show as **SUCCESS** in batch details
7. Navigate to **Members** page and confirm members appear with **APPROVED** status

**Expected Result**: All members registered and visible in Members list

---

### Scenario 2: Loan Product Setup (Admin)

**Objective**: Ensure loan products are enabled

**Steps**:
1. Login as **Admin** (username: `admin`, password: `password`)
2. Navigate to **Fund Configuration**
3. Verify at least 2 loan products are **ENABLED**:
   - Personal Loan (Interest Rate: 10%)
   - Business Loan (Interest Rate: 12%)
4. Check min/max amounts and term months are reasonable

**Expected Result**: Loan products configured and active

---

### Scenario 3: Bulk Loan Application (Treasurer)

**Objective**: Submit bulk loan applications

**Steps**:
1. Login as **Treasurer**
2. Navigate to **Bulk Processing**
3. Click **Download Template** → Select **Loan Applications**
4. Fill in loan data (at least 3 loans):
   - Member Number: Use registered members (M-YYYY-XXXXX)
   - Loan Product: "Personal Loan" or "Business Loan"
   - Amount: 50,000 - 200,000
   - Term Months: 12-24
   - Purpose: Business, Medical, Education, etc.
   - Guarantor 1 & 2: Use different member numbers
5. Upload the file
6. Verify batch shows **PENDING** status

**Expected Result**: Loan batch created with PENDING status, calculations visible (Interest, Total Repayable, Monthly)

---

### Scenario 4: Loan Approval (Credit Committee)

**Objective**: Review and approve individual loans

**Steps**:
1. Login as **Credit Committee** (username: `credit_committee`, password: `password`)
2. Navigate to **Bulk Processing**
3. Find the loan batch from Scenario 3
4. Click **View** to open batch details
5. For each loan item:
   - Click **Eye icon** to view member & guarantor eligibility
   - Review validation results (Member eligible? Guarantors eligible?)
   - Click **Checkmark icon** to approve (if all criteria met)
   - Or click **X icon** to reject with reason
6. Verify loan status changes to **APPROVED** or **REJECTED**

**Expected Result**: 
- Loans approved show green checkmark
- Rejected loans show red X with reason
- Eligibility validation shows all checks passed

---

### Scenario 5: Loan Disbursement (Treasurer)

**Objective**: Disburse approved loans

**Steps**:
1. Login as **Treasurer**
2. Navigate to **Bulk Processing**
3. Scroll to **Loan Disbursement** section
4. Verify approved loans from Scenario 4 appear in the table
5. Select loans to disburse (checkboxes)
6. Click **Bulk Disburse** button
7. Verify disbursement results show success/failure

**Expected Result**: 
- Approved loans disbursed successfully
- Loan status changes to **DISBURSED**
- Member account updated with loan amount

---

### Scenario 6: Individual Loan Application (Member)

**Objective**: Apply for loan as individual member

**Steps**:
1. Login as **Member** (use registered member credentials)
2. Navigate to **Loans**
3. Click **New Loan Application**
4. Fill in:
   - Loan Product: Select from dropdown
   - Amount: Within product limits
   - Term Months: Within product range
   - Purpose: Describe loan purpose
   - Guarantors: Select 2 guarantors from dropdown
5. Submit application
6. Verify loan appears in **Loans** list with **PENDING** status

**Expected Result**: Individual loan application created and visible

---

### Scenario 7: Loan Approval Workflow (Credit Committee)

**Objective**: Approve individual loan application

**Steps**:
1. Login as **Credit Committee**
2. Navigate to **Loans**
3. Find the loan from Scenario 6
4. Click **Eye icon** to view details
5. Review:
   - Member eligibility status
   - Guarantor eligibility status
   - Loan calculations
6. Click **Approve** button
7. Verify loan status changes to **APPROVED**

**Expected Result**: Loan approved, status updated, approval date recorded

---

### Scenario 8: Monthly Contributions (Treasurer)

**Objective**: Process monthly member contributions

**Steps**:
1. Login as **Treasurer**
2. Navigate to **Bulk Processing**
3. Click **Download Template** → Select **Monthly Contributions**
4. Fill in contributions for registered members:
   - Member Number
   - Savings: 5,000-10,000
   - Shares: 2,000-5,000
   - Optional funds: Benevolent, Development, etc.
5. Upload file
6. Verify batch processes successfully

**Expected Result**: Contributions recorded, member balances updated

---

### Scenario 9: Loan Repayment (Member)

**Objective**: Make loan repayment

**Steps**:
1. Login as **Member** with active loan
2. Navigate to **Loans**
3. Find active loan
4. Click **Make Payment** button
5. Enter repayment amount (suggest monthly amount)
6. Submit payment
7. Verify outstanding balance decreases

**Expected Result**: Payment recorded, balance updated, transaction logged

---

### Scenario 10: Reports (Admin/Treasurer)

**Objective**: Generate and view reports

**Steps**:
1. Login as **Admin** or **Treasurer**
2. Navigate to **Reports**
3. Generate **Profit & Loss Report**:
   - Select date range
   - View revenue, expenses, net profit
4. Generate **Aging Analysis Report**:
   - View overdue loans by age bracket
5. Export reports to Excel

**Expected Result**: Reports generate correctly with accurate calculations

---

## Verification Checklist

### Frontend
- [ ] All pages load without errors
- [ ] Dialogs fit screen without scrolling (1024x768)
- [ ] Calculations display correctly (Interest, Total Repayable, Monthly)
- [ ] Guarantor information visible in loan details
- [ ] Rejection reasons visible when loan rejected
- [ ] Individual approval only (no bulk approve/reject buttons)
- [ ] Role-based access working (Treasurer vs Credit Committee)

### Backend
- [ ] No 400 errors on loan approval
- [ ] Loan entities created automatically from bulk items
- [ ] Eligibility validation working correctly
- [ ] Guarantor validation passing
- [ ] Calculations accurate
- [ ] Database transactions consistent

### Data Integrity
- [ ] Member balances correct after contributions
- [ ] Loan balances correct after disbursement
- [ ] Repayment amounts deducted correctly
- [ ] Audit logs recording all actions
- [ ] No orphaned records

---

## Troubleshooting

### Issue: 400 Error on Loan Approval
**Solution**: 
1. Ensure backend was rebuilt: `mvn clean package`
2. Restart backend with new JAR
3. Check BulkProcessingService.approveLoanItem() has Loan entity creation logic

### Issue: Dialogs Scrolling
**Solution**: 
1. Check max-h-[85vh] or max-h-[80vh] classes applied
2. Reduce padding and font sizes if needed
3. Test on 1024x768 resolution

### Issue: Calculations Not Showing
**Solution**:
1. Verify BulkProcessingService.parseLoanApplications() calculates fields
2. Check totalInterest, totalRepayable, monthlyRepayment are set
3. Refresh page to reload data

### Issue: Guarantor Validation Failing
**Solution**:
1. Ensure guarantors are APPROVED members
2. Check guarantor has sufficient savings/shares
3. Verify no active loan defaults
4. Check guarantor not already guarantor for too many loans

---

## Performance Notes

- Bulk uploads: Max 5MB file size
- Batch processing: Handles 1000+ records
- Report generation: May take 5-10 seconds for large date ranges
- Database queries: Optimized with proper indexing

---

## Post-Test Cleanup

After testing, run cleanup script to reset for next session:
```bash
# Execute CLEAN_DATABASE.sql to remove all test data
```

---

## Presentation Demo Flow

1. **Start**: Show clean system with no data
2. **Register Members**: Upload member batch (5 members)
3. **Submit Loans**: Upload loan batch (3 loans)
4. **Approve Loans**: Credit Committee approves each loan individually
5. **Disburse**: Treasurer disburses approved loans
6. **Verify**: Show updated member accounts and loan status
7. **Reports**: Generate P&L and Aging Analysis reports

**Total Time**: ~15-20 minutes for full demo
