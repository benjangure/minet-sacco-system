# ========================================
# IMPORT ALL MONTHS - CORRECT APPROACH
# ========================================
# Understanding:
# - B/F = Balance Brought Forward (opening from last month)
# - P = Principal paid this month
# - I = Interest paid this month  
# - C/D = Closing/Carried Down = B/F - P (outstanding balance)
# - C/F = Closing Forward = Next month's B/F
#
# For JANUARY 2026 (opening):
#   - Shares: B/F - 3000 = Opening balance
#   - Loans: C/D = Outstanding balance
#
# For FEBRUARY onwards:
#   - Record P + I as repayment transaction
#   - Update loan to C/D
#   - Update shares to C/F
# ========================================

$ErrorActionPreference = "Stop"

$MYSQL_USER = "minetsacco"
$MYSQL_PASSWORD = "0a0b0c0D."
$DATABASE_NAME = "minetsacco"

function Parse-Decimal($value) {
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match '^-\s*$') {
        return 0
    }
    $cleaned = $value.Trim() -replace '[",\s]', ''
    try {
        return [decimal]$cleaned
    } catch {
        return 0
    }
}

function Import-CSV-WithSkip($filePath, $skipLines) {
    $csvContent = Get-Content $filePath -Encoding UTF8
    
    # Find header line
    $headerIndex = 0
    for ($i = 0; $i -lt $csvContent.Count; $i++) {
        if ($csvContent[$i] -match "Payroll #|PayrollNum") {
            $headerIndex = $i
            break
        }
    }
    
    $dataLines = $csvContent[($headerIndex)..$($csvContent.Count-1)]
    $tempCsvFile = [System.IO.Path]::GetTempFileName()
    $dataLines | Out-File -FilePath $tempCsvFile -Encoding UTF8
    
    $records = Import-Csv -Path $tempCsvFile -Header @(
        'PayrollNum', 'FullName', 'SharesBF', 'SharesI', 'SharesCF',
        'NormalBF', 'NormalP', 'NormalI', 'NormalCD',
        'Emerg1BF', 'Emerg1P', 'Emerg1I', 'Emerg1CD',
        'Emerg2BF', 'Emerg2P', 'Emerg2I', 'Emerg2CD',
        'GuarBF', 'GuarP', 'GuarCD', 'Extra1', 'Extra2'
    ) | Select-Object -Skip 1
    
    Remove-Item $tempCsvFile
    return $records
}

# ========================================
# JANUARY 2026 - OPENING BALANCES
# ========================================

Write-Host "`n================================================" -ForegroundColor Cyan
Write-Host "  IMPORTING JANUARY 2026 - OPENING BALANCES" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

$JAN_CSV = "C:\Users\Lenovo\Downloads\01 2026 - Listing Final(Listing).csv"
$janRecords = Import-CSV-WithSkip -filePath $JAN_CSV -skipLines 6

Write-Host "Parsed $($janRecords.Count) January records" -ForegroundColor Green

$sql = @"
USE $DATABASE_NAME;
SET SQL_SAFE_UPDATES = 0;

-- ========================================
-- JANUARY 2026 - OPENING BALANCES
-- ========================================
-- Shares: B/F - 3000
-- Loans: C/D (Closing Down = Outstanding)
-- Generated: $(Get-Date)

"@

$janSharesCount = 0
$janLoansCount = 0

foreach ($record in $janRecords) {
    $payrollNum = $record.PayrollNum.Trim()
    $fullName = $record.FullName.Trim()
    
    if ([string]::IsNullOrWhiteSpace($fullName) -or $fullName -match 'TOTAL|FullName|Payroll') {
        continue
    }
    
    if ([string]::IsNullOrWhiteSpace($payrollNum)) {
        continue
    }
    
    # SHARES: B/F - 3000
    $sharesBF = Parse-Decimal $record.SharesBF
    $sharesOpening = $sharesBF - 3000
    
    if ($sharesOpening -gt 0) {
        $sql += @"

-- $fullName ($payrollNum) - Shares: $sharesBF - 3000 = $sharesOpening
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = $sharesOpening
WHERE m.employee_id = '$payrollNum' AND a.account_type = 'SHARES';

"@
        $janSharesCount++
    }
    
    # NORMAL LOAN: C/D
    $normalCD = Parse-Decimal $record.NormalCD
    if ($normalCD -gt 0) {
        $sql += @"

-- $fullName ($payrollNum) - Normal Loan Outstanding: $normalCD
UPDATE loans 
SET amount = $normalCD, outstanding_balance = $normalCD
WHERE member_id = (SELECT id FROM members WHERE employee_id = '$payrollNum')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

"@
        $janLoansCount++
    }
    
    # EMERGENCY 1: C/D
    $emerg1CD = Parse-Decimal $record.Emerg1CD
    if ($emerg1CD -gt 0) {
        $sql += @"

-- $fullName ($payrollNum) - Emergency 1: $emerg1CD
UPDATE loans 
SET amount = $emerg1CD, outstanding_balance = $emerg1CD
WHERE member_id = (SELECT id FROM members WHERE employee_id = '$payrollNum')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1;

"@
    }
    
    # EMERGENCY 2: C/D
    $emerg2CD = Parse-Decimal $record.Emerg2CD
    if ($emerg2CD -gt 0) {
        $sql += @"

-- $fullName ($payrollNum) - Emergency 2: $emerg2CD
UPDATE loans 
SET amount = $emerg2CD, outstanding_balance = $emerg2CD
WHERE member_id = (SELECT id FROM members WHERE employee_id = '$payrollNum')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC LIMIT 1 OFFSET 1;

"@
    }
}

$sql += @"

SET SQL_SAFE_UPDATES = 1;

SELECT 'January Import Complete' as Status;
SELECT COUNT(*) as loans_updated, SUM(outstanding_balance) as total_outstanding 
FROM loans WHERE outstanding_balance > 0;
SELECT COUNT(*) as shares_updated, SUM(balance) as total_shares 
FROM accounts WHERE account_type = 'SHARES' AND balance > 0;
"@

# Save and execute January
$JAN_SQL_FILE = ".\IMPORT_JANUARY_2026.sql"
$sql | Out-File -FilePath $JAN_SQL_FILE -Encoding UTF8

Write-Host "`nJanuary Summary:" -ForegroundColor Yellow
Write-Host "  Shares accounts: $janSharesCount" -ForegroundColor White
Write-Host "  Loans: $janLoansCount" -ForegroundColor White
Write-Host "`nExecuting January import..." -ForegroundColor Yellow

Get-Content $JAN_SQL_FILE | mysql -u $MYSQL_USER -p"$MYSQL_PASSWORD" $DATABASE_NAME 2>&1 | Where-Object { $_ -notmatch "Warning" }

Write-Host ""
Write-Host "=== DONE! January 2026 imported ===" -ForegroundColor Green
Write-Host "RESTART THE BACKEND to see changes." -ForegroundColor Yellow
Write-Host ""
