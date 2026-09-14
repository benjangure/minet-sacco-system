Set-Location "c:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system"

$files = @(
    "backend/src/main/java/com/minet/sacco/controller/GLPeriodEntryController.java",
    "backend/src/main/java/com/minet/sacco/controller/ReportsController.java",
    "backend/src/main/java/com/minet/sacco/dto/GLPeriodEntryRequestDTO.java",
    "backend/src/main/java/com/minet/sacco/entity/GLManualEntry.java",
    "backend/src/main/java/com/minet/sacco/entity/Transaction.java",
    "backend/src/main/java/com/minet/sacco/repository/AccountRepository.java",
    "backend/src/main/java/com/minet/sacco/repository/LoanRepository.java",
    "backend/src/main/java/com/minet/sacco/service/GLCalculationService.java",
    "minetsacco-main/src/pages/GLConfiguration.tsx",
    "minetsacco-main/src/pages/GLManualEntries.tsx",
    "test_gl_full.ps1"
)

foreach ($f in $files) {
    git add $f
    Write-Output "Staged: $f"
}

git add -f "backend/src/main/resources/db/migration/V150__Default_GL_Chart_Of_Accounts.sql"
Write-Output "Staged: V150 migration"

Write-Output ""
Write-Output "=== Staged files ==="
git diff --cached --name-only

Write-Output ""
Write-Output "=== Committing ==="
$msg = @"
GL/Accounting production fixes + Member Contributions export + Share Transfer

Backend:
- GLCalculationService: implement calculateFormula() (FORMULA accounts now work)
- GLCalculationService: implement calculateComputed() (RETAINED_EARNINGS, NET_INCOME,
  TOTAL_EQUITY, BALANCE_CHECK now return real values)
- GLCalculationService: make loans/accounts aggregations date-aware
- Transaction.TransactionType: add ENTRANCE_FEE, BANK_CHARGE, SHARE_TRANSFER
- GLPeriodEntryController: fix isDebit defaulting from account normalBalance
- GLPeriodEntryRequestDTO: add isDebit field
- GLManualEntry: fix ByteBuddyInterceptor JSON serialization error on lazy associations
- ReportsController: fix split-brain - wire JSON endpoints to real GL services
- LoanRepository: add sumOutstandingBalanceAsOf date-aware query
- AccountRepository: add sumAccountBalanceAsOf date-aware query
- AccountService: add transferShares() with audit log + getMemberContributionsReport()
- AccountController: add POST /accounts/transfer-shares
- ReportsController: add /member-contributions/{id} export endpoints
- MemberContributionsReportDTO + ShareTransferRequest DTOs
- ReportExportService: exportMemberContributionsToExcel/ToPdf
- V150: seed default SACCO chart of accounts (27 accounts, only if GL is empty)

Frontend:
- GLManualEntries.tsx: rewrite with red theme tokens + role-aware UX
- GLConfiguration.tsx: fix POSTED badge from blue to accent token
- Reports.tsx: member-contributions view-modal + searchable member picker
- Savings.tsx: Transfer Shares dialog

Tests:
- test_gl_full.ps1: 22-step end-to-end GL test (48 PASS, 0 FAIL)
"@

git commit -m $msg
Write-Output "=== Pushing ==="
git push origin main
Write-Output "=== Done ==="
