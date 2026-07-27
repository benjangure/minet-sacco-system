# GL Reports Integration Issue & Solution

## 🔴 PROBLEM IDENTIFIED

The Reports page is **NOT connected** to the new GL system. It's still using the old ReportsService logic instead of calling the GL endpoints.

### Current Flow (WRONG ❌)
```
Reports.tsx (Frontend)
    ↓
    calls /api/reports/trial-balance
    ↓
ReportsController
    ↓
    calls reportsService.generateTrialBalance() [OLD LOGIC]
    ↓
Uses member-based trial balance (not GL accounts)
```

### What Should Happen (CORRECT ✅)
```
Reports.tsx (Frontend)
    ↓
    calls /api/gl/trial-balance
    ↓
GLController
    ↓
    calls glCalculationService.generateTrialBalance()
    ↓
Uses GL accounts + GL manual entries + period filtering
```

---

## 🎯 THE ISSUE

The ReportsService.generateTrialBalance() method:
- Pulls data from `accounts` table
- Uses member IDs and account types
- Does NOT include GL manual entries
- Does NOT support period-based filtering
- Does NOT use GL account structure (section labels, normal balance, etc.)

This means:
- ❌ When treasurer creates GL accounts and manual entries, reports DON'T reflect them
- ❌ Period-sensitive entries are ignored
- ❌ GL structure (balance sheet format) is not applied
- ❌ Journal entries have no effect on reports

---

## ✅ THE SOLUTION

### Step 1: Update ReportsController to call GL Endpoints

Replace the old trial-balance endpoint to delegate to GLController:

```java
// OLD (ReportsController.java)
@GetMapping("/trial-balance")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
public ResponseEntity<ApiResponse<ReportsService.TrialBalanceReport>> getTrialBalance(
        @RequestParam(required = false) String memberNumber,
        @RequestParam(required = false) String accountType) {
    ReportsService.TrialBalanceReport report = reportsService.generateTrialBalance(memberNumber, accountType);
    return ResponseEntity.ok(ApiResponse.success("Trial balance report generated successfully", report));
}

// NEW (ReportsController.java) - delegates to GL
@GetMapping("/trial-balance")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
public ResponseEntity<ApiResponse<GLTrialBalanceReportDTO>> getTrialBalance(
        @RequestParam(required = false) LocalDate asOfDate,
        @RequestParam(required = false) Integer periodMonth,
        @RequestParam(required = false) Integer periodYear) {
    
    if (asOfDate == null) {
        asOfDate = LocalDate.now();
    }
    if (periodMonth == null) {
        periodMonth = asOfDate.getMonthValue();
    }
    if (periodYear == null) {
        periodYear = asOfDate.getYear();
    }
    
    GLTrialBalanceReportDTO report = glCalculationService.generateTrialBalance(asOfDate, periodMonth, periodYear);
    return ResponseEntity.ok(ApiResponse.success("Trial balance report generated from GL", report));
}
```

### Step 2: Update Balance Sheet Endpoint

Replace the old balance-sheet endpoint:

```java
// OLD
@GetMapping("/balance-sheet")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
public ResponseEntity<ApiResponse<ReportsService.BalanceSheetReport>> getBalanceSheet() {
    ReportsService.BalanceSheetReport report = reportsService.generateBalanceSheet();
    return ResponseEntity.ok(ApiResponse.success("Balance sheet generated", report));
}

// NEW
@GetMapping("/balance-sheet")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
public ResponseEntity<ApiResponse<BalanceSheetDTO>> getBalanceSheet(
        @RequestParam(required = false) LocalDate asOfDate,
        @RequestParam(required = false) Integer periodMonth,
        @RequestParam(required = false) Integer periodYear) {
    
    if (asOfDate == null) {
        asOfDate = LocalDate.now();
    }
    if (periodMonth == null) {
        periodMonth = asOfDate.getMonthValue();
    }
    if (periodYear == null) {
        periodYear = asOfDate.getYear();
    }
    
    BalanceSheetDTO report = balanceSheetService.generateBalanceSheet(asOfDate, periodMonth, periodYear);
    return ResponseEntity.ok(ApiResponse.success("Balance sheet generated from GL", report));
}
```

### Step 3: Update Income Statement Endpoint

Replace the old profit-loss endpoint:

```java
// OLD
@GetMapping("/profit-loss")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
public ResponseEntity<ApiResponse<ReportsService.ProfitLossReport>> getProfitLossReport() {
    ReportsService.ProfitLossReport report = reportsService.generateProfitAndLossReport();
    return ResponseEntity.ok(ApiResponse.success("Profit and loss report generated", report));
}

// NEW
@GetMapping("/profit-loss")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
public ResponseEntity<ApiResponse<IncomeStatementDTO>> getProfitLossReport(
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate,
        @RequestParam(required = false) Integer periodMonth,
        @RequestParam(required = false) Integer periodYear) {
    
    if (toDate == null) {
        toDate = LocalDate.now();
    }
    if (fromDate == null) {
        fromDate = toDate.withDayOfMonth(1);
    }
    if (periodMonth == null) {
        periodMonth = toDate.getMonthValue();
    }
    if (periodYear == null) {
        periodYear = toDate.getYear();
    }
    
    IncomeStatementDTO report = incomeStatementService.generateIncomeStatement(fromDate, toDate, periodMonth, periodYear);
    return ResponseEntity.ok(ApiResponse.success("Income statement generated from GL", report));
}
```

### Step 4: Update Exports to Use GL Data

Update export methods to use GL-based reports:

```java
// Trial Balance Export
@GetMapping("/trial-balance/export/excel")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
public ResponseEntity<byte[]> exportTrialBalanceExcel(
        @RequestParam(required = false) LocalDate asOfDate,
        @RequestParam(required = false) Integer periodMonth,
        @RequestParam(required = false) Integer periodYear) throws Exception {
    
    if (asOfDate == null) asOfDate = LocalDate.now();
    if (periodMonth == null) periodMonth = asOfDate.getMonthValue();
    if (periodYear == null) periodYear = asOfDate.getYear();
    
    GLTrialBalanceReportDTO report = glCalculationService.generateTrialBalance(asOfDate, periodMonth, periodYear);
    byte[] excelFile = reportExportService.exportTrialBalanceToExcel(report);
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=trial_balance_gl_" + LocalDate.now() + ".xlsx")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(excelFile);
}

// Balance Sheet Export
@GetMapping("/balance-sheet/export/excel")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
public ResponseEntity<byte[]> exportBalanceSheetExcel(
        @RequestParam(required = false) LocalDate asOfDate,
        @RequestParam(required = false) Integer periodMonth,
        @RequestParam(required = false) Integer periodYear) throws Exception {
    
    if (asOfDate == null) asOfDate = LocalDate.now();
    if (periodMonth == null) periodMonth = asOfDate.getMonthValue();
    if (periodYear == null) periodYear = asOfDate.getYear();
    
    BalanceSheetDTO report = balanceSheetService.generateBalanceSheet(asOfDate, periodMonth, periodYear);
    byte[] excelFile = reportExportService.exportBalanceSheetToExcel(report);
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=balance_sheet_gl_" + LocalDate.now() + ".xlsx")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(excelFile);
}

// Income Statement Export
@GetMapping("/profit-loss/export/excel")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_AUDITOR')")
public ResponseEntity<byte[]> exportProfitLossExcel(
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate,
        @RequestParam(required = false) Integer periodMonth,
        @RequestParam(required = false) Integer periodYear) throws Exception {
    
    if (toDate == null) toDate = LocalDate.now();
    if (fromDate == null) fromDate = toDate.withDayOfMonth(1);
    if (periodMonth == null) periodMonth = toDate.getMonthValue();
    if (periodYear == null) periodYear = toDate.getYear();
    
    IncomeStatementDTO report = incomeStatementService.generateIncomeStatement(fromDate, toDate, periodMonth, periodYear);
    byte[] excelFile = reportExportService.exportIncomeStatementToExcel(report);
    
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=income_statement_gl_" + LocalDate.now() + ".xlsx")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(excelFile);
}
```

### Step 5: Add Needed Autowired Fields

In ReportsController, add:

```java
@Autowired
private GLCalculationService glCalculationService;

@Autowired
private BalanceSheetService balanceSheetService;

@Autowired
private IncomeStatementService incomeStatementService;
```

### Step 6: Update Frontend to Pass Period Parameters

Update Reports.tsx to send `periodMonth` and `periodYear`:

```typescript
// Trial Balance
let url = `${API_BASE_URL}/reports/trial-balance`;
const asOfDate = new Date().toISOString().split("T")[0];
url += `?asOfDate=${asOfDate}`;
if (periodMonth) url += `&periodMonth=${periodMonth}`;
if (periodYear) url += `&periodYear=${periodYear}`;

// Balance Sheet
url = `${API_BASE_URL}/reports/balance-sheet`;
url += `?asOfDate=${asOfDate}`;
if (periodMonth) url += `&periodMonth=${periodMonth}`;
if (periodYear) url += `&periodYear=${periodYear}`;

// Income Statement
url = `${API_BASE_URL}/reports/profit-loss`;
const fromDate = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split("T")[0];
url += `?fromDate=${fromDate}&toDate=${asOfDate}`;
if (periodMonth) url += `&periodMonth=${periodMonth}`;
if (periodYear) url += `&periodYear=${periodYear}`;
```

---

## 📊 Result After Fix

### Trial Balance Report
```
WILL NOW SHOW:
✅ All GL accounts (configured by treasurer)
✅ GL manual entries (approved by admin)
✅ Period-filtered entries (if marked period_sensitive)
✅ Grouped by section labels
✅ Normal balance (DEBIT/CREDIT) applied
✅ Complete audit trail
```

### Balance Sheet Report
```
WILL NOW SHOW:
✅ Assets (all ASSET gl_accounts)
✅ Liabilities (all LIABILITY gl_accounts)
✅ Equity (all EQUITY gl_accounts)
✅ Manual entries affecting each line
✅ Verification: Assets = Liabilities + Equity
```

### Income Statement Report
```
WILL NOW SHOW:
✅ Revenues (all REVENUE gl_accounts)
✅ Expenses (all EXPENSE gl_accounts)
✅ Manual entries for the period
✅ Net Income/Loss calculated correctly
✅ All approved journal entries reflected
```

---

## 🔗 Data Flow (After Fix)

```
1. Treasurer creates GL Accounts
   ↓
2. Treasurer adds GL Manual Entries
   ↓
3. Admin approves entries (stored in gl_manual_entries with ApprovalStatus=APPROVED)
   ↓
4. User goes to Reports page
   ↓
5. Selects "Trial Balance" or "Balance Sheet"
   ↓
6. Frontend calls /api/reports/trial-balance (or /api/reports/balance-sheet)
   ↓
7. ReportsController delegates to GLCalculationService
   ↓
8. GLCalculationService:
   - Loads all GL accounts
   - For AGGREGATION accounts: pulls from loans/accounts/transactions
   - For MANUAL_ENTRY accounts: pulls approved entries from gl_manual_entries
   - Applies period filtering if period_sensitive=true
   - Calculates balances
   ↓
9. Report is generated with GL data
   ↓
10. Frontend displays with GL structure and filters
    - Export to Excel/PDF
    - Group by section
    - Show DEBIT/CREDIT columns
```

---

## 🚀 Implementation Steps

1. **Update ReportsController** - Add GL delegation
2. **Add Autowired fields** - For GL services
3. **Update export methods** - Use GL reports
4. **Update Reports.tsx** - Pass period parameters
5. **Test end-to-end** - Create GL accounts → Add entries → Approve → Generate report
6. **Verify data flow** - Entries appear in reports

---

## ⚠️ IMPORTANT NOTES

- The old ReportsService methods can remain for backward compatibility with member-based reports
- Cashbook, Member Statement, Loan Register can still use old logic (they don't involve GL)
- Only Trial Balance, Balance Sheet, and Income Statement should use GL
- After fix, reports will be GL-centric, not member-centric

---

## Testing Checklist

```
□ Create GL account "BANK_ACCOUNT" (ASSET type)
□ Create GL manual entry for 100,000 KES
□ Approve the entry (ApprovalStatus=APPROVED)
□ Go to Reports > Trial Balance
□ Verify "BANK_ACCOUNT" appears with 100,000 balance
□ Export to Excel and verify formatting
□ Test with period filtering (periodMonth=1, periodYear=2026)
□ Verify entries marked period_sensitive only show for that period
```

---

## Summary

**Current Status:** ❌ Reports NOT connected to GL
**Required Fix:** Update ReportsController to call GL endpoints instead of old ReportsService
**Impact:** All GL entries will appear in reports once approved
**Timeline:** ~2 hours to implement + 1 hour testing
