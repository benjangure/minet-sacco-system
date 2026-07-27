# 4 Missing Reports - Implementation Specification

Based on feedback from the system review, these 4 reports need to be built using existing data.

---

## CRITICAL CORRECTIONS TO SPECIFICATION

**Before implementation, these corrections must be applied:**

### 1. Guarantor Report - Frozen Self Guarantee Calculation
**WRONG:** `SUM(loan.outstandingBalance) WHERE member_id = X AND status = DISBURSED`

**CORRECT:** `SUM(guarantor.pledgeAmount) WHERE guarantor.member_id = X AND guarantor.self_guarantee = true AND guarantor.status = ACTIVE`

**Why:** A member with external guarantors has outstanding loan balance but ZERO frozen savings. Frozen amount comes from self-guarantee pledges only.

### 2. Loan Eligibility Report - Frozen Amount Calculation
**WRONG:** Using outstanding loan balance as frozen amount

**CORRECT:** `SUM(guarantor.pledgeAmount) WHERE guarantor.member_id = X AND guarantor.self_guarantee = true AND guarantor.status = ACTIVE`

**Why:** Same as above - only self-guarantee pledges freeze savings, not external guarantor loans.

### 3. Loan Eligibility Report - Months Contributed Calculation
**WRONG:** `COUNT(DISTINCT MONTH(bulk_transaction_items.date))` - fails across years

**CORRECT:** Use `member.consecutiveMonthsCounter` field

**Why:** The counter field is already maintained by bulk upload process and handles year boundaries correctly.

### 4. Guarantor Report - Add All Members View
**NEW REQUIREMENT:** Add "All Members" view mode in addition to single member view

**Why:** Treasurer's primary need is to quickly check who can guarantee what across all members at once.

---

## IMPLEMENTATION INSTRUCTIONS FOR CASCADE

Implement all 4 reports following the specification with these corrections applied:

1. **Frozen Self Guarantee** in Reports 1 & 2 must come from `guarantor` table where `self_guarantee = true` and `status = ACTIVE`
2. **Months Contributed** in Report 2 must use `member.consecutiveMonthsCounter` field
3. **Guarantor Report** must support both "Single Member" and "All Members" view modes
4. Implement in priority order: Withdrawal Monitoring → Guarantor Report → Loan Eligibility → Monthly Contribution Tracking

---

## REPORT 1: GUARANTOR REPORT

### Purpose
Treasurer's primary concern - show guarantor capacity and obligations per member

### Access Control
- Admin
- Treasurer
- Auditor
- Loan Officer

### Data Source
- `guarantor` table
- `account` table (for savings)
- `loan` table (for loans being guaranteed)

### Report Structure - Per Member

**Member Information:**
- Member Number
- Member Name
- Member Status

**Guarantor Capacity:**
- Total Savings (sum of all account balances)
- Frozen Self Guarantee Amount (self-guarantee pledges only, from guarantor table)
- Available Savings (Total Savings - Frozen Self Guarantee)

**Active Guarantor Pledges:**
- Number of loans they are guaranteeing
- Total Pledge Amount (sum of all active guarantor pledges)
- Available Guarantorship Capacity (Available Savings - Total Pledge Amount)

**Loans They Are Guaranteeing:**
- For each loan:
  - Loan Number
  - Borrower Name
  - Loan Amount
  - Outstanding Balance
  - Repayment Progress (% repaid)
  - Guarantor Pledge Amount
  - Status (ACTIVE, RELEASED, DEFAULTED)

### Calculations
```
Total Savings = SUM(account.balance) WHERE member_id = X

Frozen Self Guarantee = SUM(guarantor.pledgeAmount) 
                       WHERE guarantor.member_id = X 
                       AND guarantor.self_guarantee = true 
                       AND guarantor.status = ACTIVE

Available Savings = Total Savings - Frozen Self Guarantee

Total Pledge Amount = SUM(guarantor.pledgeAmount) 
                     WHERE guarantor_member_id = X 
                     AND guarantor.self_guarantee = false
                     AND guarantor.status = ACTIVE

Available Guarantorship Capacity = Available Savings - Total Pledge Amount

Repayment Progress = (Loan Amount - Outstanding Balance) / Loan Amount * 100
```

### Filters
- View Mode: 
  - "Single Member" (requires Member Number)
  - "All Members" (shows guarantorship capacity for all members - Treasurer's daily view)
- Guarantor Status (ACTIVE, RELEASED, DEFAULTED - optional, only for Single Member view)

### Export Options
- Excel
- PDF

### Frontend Integration
- Add to Reports.tsx
- Report Type: "guarantor"
- Filters: 
  - View Mode (Single Member / All Members) - required
  - Member Number (required if Single Member selected)
  - Guarantor Status (optional)

### Backend Implementation
**Controller:** ReportsController.java
- Endpoint (Single Member): `GET /api/reports/guarantor/{memberId}`
- Endpoint (All Members): `GET /api/reports/guarantor/all`
- Export Excel (Single): `GET /api/reports/guarantor/{memberId}/export/excel`
- Export PDF (Single): `GET /api/reports/guarantor/{memberId}/export/pdf`
- Export Excel (All): `GET /api/reports/guarantor/all/export/excel`
- Export PDF (All): `GET /api/reports/guarantor/all/export/pdf`

**Service:** Create GuarantorReportService.java
- Method: `generateGuarantorReport(Long memberId, String guarantorStatus)` - for single member
- Method: `generateGuarantorReportAll()` - for all members summary

---

## REPORT 2: LOAN ELIGIBILITY REPORT

### Purpose
Staff quick reference - show member's loan eligibility status

### Access Control
- Admin
- Treasurer
- Auditor
- Loan Officer
- Customer Support

### Data Source
- `account` table (for savings)
- `loan` table (for outstanding balance)
- `member` table (for contribution history)
- `bulk_transaction_items` table (for contribution tracking)

### Report Structure - Per Member

**Member Information:**
- Member Number
- Member Name
- Member Status
- Date Joined
- Months as Member

**Savings Status:**
- Savings Balance (current SAVINGS account balance)
- Frozen Amount (self-guarantee pledges only, from guarantor table)
- Available Savings (Savings Balance - Frozen Amount)

**Eligibility Calculation:**
- Gross Eligibility (Available Savings * 3)
- Outstanding Loan Balance (sum of all DISBURSED loans)
- Remaining Eligibility (Gross Eligibility - Outstanding Loan Balance)
- Months Contributed (from member.consecutiveMonthsCounter field)

**Eligibility Status:**
- Status: ELIGIBLE or NOT ELIGIBLE
- Reason (if not eligible):
  - "Less than 6 months membership"
  - "Insufficient savings"
  - "Outstanding loan balance exceeds eligibility"
  - "Member status is INACTIVE/SUSPENDED/EXITED"

### Calculations
```
Savings Balance = account.balance WHERE account_type = SAVINGS AND member_id = X

Frozen Amount = SUM(guarantor.pledgeAmount) 
               WHERE guarantor.member_id = X 
               AND guarantor.self_guarantee = true 
               AND guarantor.status = ACTIVE

Available Savings = Savings Balance - Frozen Amount

Gross Eligibility = Available Savings * 3

Outstanding Loan Balance = SUM(loan.outstandingBalance) 
                          WHERE member_id = X AND status = DISBURSED

Remaining Eligibility = Gross Eligibility - Outstanding Loan Balance

Months Contributed = member.consecutiveMonthsCounter

Eligible IF:
  - Months Contributed >= 6
  - Available Savings > 0
  - Remaining Eligibility > 0
  - Member Status = ACTIVE
```

### Filters
- Member Number (required)

### Export Options
- Excel
- PDF

### Frontend Integration
- Add to Reports.tsx
- Report Type: "loan-eligibility"
- Filters: Member Number

### Backend Implementation
**Controller:** ReportsController.java
- Endpoint: `GET /api/reports/loan-eligibility/{memberId}`
- Export Excel: `GET /api/reports/loan-eligibility/{memberId}/export/excel`
- Export PDF: `GET /api/reports/loan-eligibility/{memberId}/export/pdf`

**Service:** Create LoanEligibilityReportService.java
- Method: `generateLoanEligibilityReport(Long memberId)`

---

## REPORT 3: MONTHLY CONTRIBUTION TRACKING REPORT

### Purpose
Bulk upload monitoring - track monthly contributions and member eligibility progress

### Access Control
- Admin
- Treasurer
- Auditor

### Data Source
- `bulk_batches` table
- `bulk_transaction_items` table
- `member` table
- `account` table

### Report Structure - Per Bulk Upload Batch

**Batch Information:**
- Batch ID
- Batch Date
- Batch Status (PENDING, PROCESSING, COMPLETED, FAILED)
- Uploaded By (user name)

**Member Processing Summary:**
- Total Members Expected (from file)
- Total Members in File
- Missing Members (Expected - In File)
- Members Successfully Processed
- Members with Errors

**Contribution Summary:**
- Total Savings Posted (sum of DEPOSIT transactions)
- Total Loan Repayments Posted (sum of LOAN_REPAYMENT transactions)
- Total Transactions Processed

**Eligibility Progress:**
- Members Who Became Eligible This Month (reached 6 months)
- Members at Month 5 of 6 (one month away from eligibility)
- Members at Month 4 of 6
- Members at Month 3 of 6
- Members at Month 2 of 6
- Members at Month 1 of 6

**Member Details Table:**
- Member Number
- Member Name
- Contribution Amount
- Repayment Amount
- Months Contributed
- Status (ELIGIBLE, NOT_ELIGIBLE, PENDING)
- Errors (if any)

### Calculations
```
Total Members Expected = COUNT(DISTINCT member_id) in file

Total Members in File = COUNT(DISTINCT member_id) in bulk_transaction_items

Missing Members = Total Members Expected - Total Members in File

Total Savings Posted = SUM(amount) WHERE transaction_type = DEPOSIT

Total Loan Repayments Posted = SUM(amount) WHERE transaction_type = LOAN_REPAYMENT

Members Who Became Eligible = COUNT(member_id) 
                             WHERE months_contributed = 6 
                             AND this_month_first_time_reaching_6

Members at Month X = COUNT(member_id) WHERE months_contributed = X
```

### Filters
- Batch Date Range (start date to end date)
- Batch Status (optional)

### Export Options
- Excel
- PDF

### Frontend Integration
- Add to Reports.tsx
- Report Type: "monthly-contribution-tracking"
- Filters: Start Date, End Date, Batch Status

### Backend Implementation
**Controller:** ReportsController.java
- Endpoint: `GET /api/reports/monthly-contribution-tracking`
- Export Excel: `GET /api/reports/monthly-contribution-tracking/export/excel`
- Export PDF: `GET /api/reports/monthly-contribution-tracking/export/pdf`

**Service:** Create MonthlyContributionTrackingService.java
- Method: `generateMonthlyContributionTrackingReport(LocalDate startDate, LocalDate endDate, String batchStatus)`

---

## REPORT 4: WITHDRAWAL MONITORING REPORT

### Purpose
Track all withdrawals for audit and compliance

### Access Control
- Admin
- Treasurer
- Auditor

### Data Source
- `transaction` table (filtered by WITHDRAWAL type)
- `account` table
- `member` table
- `user` table (for who processed)

### Report Structure - Per Withdrawal Transaction

**Transaction Details:**
- Transaction ID
- Member Number
- Member Name
- Account Type (SAVINGS, SHARES, etc.)
- Withdrawal Amount
- Transaction Date
- Transaction Time
- Withdrawal Method (M_PESA, MANUAL_CASH, BANK_TRANSFER)
- Processed By (user name)
- Transaction Status (COMPLETED, PENDING, FAILED)

**Account Impact:**
- Account Balance Before Withdrawal
- Account Balance After Withdrawal
- Remaining Balance

**Summary Totals:**
- Total Withdrawals (count)
- Total Amount Withdrawn (sum)
- By Method:
  - M_PESA: Count and Amount
  - Manual Cash: Count and Amount
  - Bank Transfer: Count and Amount

### Calculations
```
Account Balance Before = account.balance + withdrawal.amount

Account Balance After = account.balance

Remaining Balance = account.balance

Total Withdrawals = COUNT(transaction_id) WHERE transaction_type = WITHDRAWAL

Total Amount Withdrawn = SUM(amount) WHERE transaction_type = WITHDRAWAL

By Method = GROUP BY withdrawal_method
```

### Filters
- Date Range (start date to end date)
- Member Number (optional)
- Withdrawal Method (optional)
- Transaction Status (optional)

### Export Options
- Excel
- PDF

### Frontend Integration
- Add to Reports.tsx
- Report Type: "withdrawal-monitoring"
- Filters: Start Date, End Date, Member Number, Withdrawal Method, Status

### Backend Implementation
**Controller:** ReportsController.java
- Endpoint: `GET /api/reports/withdrawal-monitoring`
- Export Excel: `GET /api/reports/withdrawal-monitoring/export/excel`
- Export PDF: `GET /api/reports/withdrawal-monitoring/export/pdf`

**Service:** Create WithdrawalMonitoringService.java
- Method: `generateWithdrawalMonitoringReport(LocalDate startDate, LocalDate endDate, String memberNumber, String method, String status)`

---

## Implementation Priority

### Phase 1 (Highest Priority)
1. **Withdrawal Monitoring Report** - Uses only transaction table, simplest to implement
2. **Guarantor Report** - Uses guarantor and account tables, medium complexity

### Phase 2 (Medium Priority)
3. **Loan Eligibility Report** - Uses multiple tables, requires eligibility logic
4. **Monthly Contribution Tracking Report** - Uses bulk_batches and bulk_transaction_items, requires aggregation

---

## Common Implementation Pattern

All 4 reports should follow this pattern:

### Backend Service
```java
@Service
public class [ReportName]Service {
    
    @Autowired
    private [Repository] repository;
    
    public [ReportDTO] generate[ReportName](parameters) {
        // Fetch data
        // Calculate metrics
        // Build DTO
        // Return report
    }
}
```

### Backend Controller
```java
@GetMapping("/[report-name]")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', ...)")
public ResponseEntity<ApiResponse<[ReportDTO]>> get[ReportName](parameters) {
    [ReportDTO] report = service.generate[ReportName](parameters);
    return ResponseEntity.ok(ApiResponse.success("Report generated", report));
}

@GetMapping("/[report-name]/export/excel")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', ...)")
public ResponseEntity<byte[]> export[ReportName]Excel(parameters) {
    [ReportDTO] report = service.generate[ReportName](parameters);
    byte[] excelFile = reportExportService.export[ReportName]ToExcel(report);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=[report-name].xlsx")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(excelFile);
}

@GetMapping("/[report-name]/export/pdf")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', ...)")
public ResponseEntity<byte[]> export[ReportName]Pdf(parameters) {
    [ReportDTO] report = service.generate[ReportName](parameters);
    byte[] pdfFile = reportExportService.export[ReportName]ToPdf(report);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=[report-name].pdf")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdfFile);
}
```

### Frontend Integration
```typescript
// In Reports.tsx
<SelectItem value="[report-name]">[Report Display Name]</SelectItem>

// In filters section
{reportType === "[report-name]" && (
    <div className="space-y-4 p-4 bg-accent rounded-lg">
        {/* Filter inputs */}
    </div>
)}

// In export handlers
else if (reportType === "[report-name]") {
    url += `/[report-name]/export/excel`;
    // Add filter parameters
}
```

---

## Verification Checklist

Before marking each report as complete:

- [ ] Service class created with generate method
- [ ] Controller endpoints created (GET, export/excel, export/pdf)
- [ ] DTO classes created for report structure
- [ ] Repository queries created (if needed)
- [ ] Access control (@PreAuthorize) applied correctly
- [ ] Frontend form added to Reports.tsx
- [ ] Filters implemented in frontend
- [ ] Excel export working
- [ ] PDF export working
- [ ] Report calculations verified with sample data
- [ ] All totals and summaries calculated correctly
- [ ] Date range filtering working
- [ ] Member filtering working
- [ ] Report tested end-to-end

