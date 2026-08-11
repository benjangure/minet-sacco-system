# ========================================
# FINAL FIX - Match by Employee ID
# ========================================
# This uses Payroll # from CSV to match with employee_id in database
# This is more reliable than name matching
# ========================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  FINAL FIX - EMPLOYEE ID MATCHING" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$MYSQL_USER = "minetsacco"
$MYSQL_PASSWORD = "0a0b0c0D."
$DATABASE_NAME = "minetsacco"
$CSV_FILE = "C:\Users\Lenovo\Downloads\01 2026 - Listing Final(Listing).csv"

# Read CSV
Write-Host "Reading CSV..." -ForegroundColor Yellow
$csvContent = Get-Content $CSV_FILE -Encoding UTF8

# Find header
$headerIndex = 0
for ($i = 0; $i -lt $csvContent.Count; $i++) {
    if ($csvContent[$i] -match "Payroll #") {
        $headerIndex = $i
        break
    }
}

# Import CSV
$dataLines = $csvContent[($headerIndex)..$($csvContent.Count-1)]
$tempCsvFile = [System.IO.Path]::GetTempFileName()
$dataLines | Out-File -FilePath $tempCsvFile -Encoding UTF8

$csvRecords = Import-Csv -Path $tempCsvFile -Header @(
    'PayrollNum', 'FullName', 'SharesBF', 'SharesI', 'SharesCF',
    'NormalBF', 'NormalP', 'NormalI', 'NormalCD',
    'Emerg1BF', 'Emerg1P', 'Emerg1I', 'Emerg1CD',
    'Emerg2BF', 'Emerg2P', 'Emerg2I', 'Emerg2CD',
    'GuarBF', 'GuarP', 'GuarCD', 'Extra1', 'Extra2'
) | Select-Object -Skip 1

Remove-Item $tempCsvFile
Write-Host "OK Parsed $($csvRecords.Count) records" -ForegroundColor Green
Write-Host ""

# Helper function
function Parse-Decimal($value) {
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match '^-\s*$') {
        return 0
    }
    $cleaned = $value.Trim() -replace '[,\s]', ''
    try {
        return [decimal]$cleaned
    } catch {
        return 0
    }
}

# Generate SQL using employee_id
Write-Host "Generating SQL using Employee ID matching..." -ForegroundColor Yellow
$sql = @"
USE $DATABASE_NAME;
SET SQL_SAFE_UPDATES = 0;

-- ========================================
-- FINAL FIX - EMPLOYEE ID MATCHING
-- ========================================

"@

$matched = 0
$skipped = 0

foreach ($record in $csvRecords) {
    $payrollNum = $record.PayrollNum.Trim()
    $fullName = $record.FullName.Trim()
    
    if ([string]::IsNullOrWhiteSpace($fullName) -or $fullName -match 'TOTAL|FullName') {
        continue
    }
    
    if ([string]::IsNullOrWhiteSpace($payrollNum) -or $payrollNum -match 'Payroll') {
        $skipped++
        continue
    }
    
    # Parse shares
    $sharesCF = Parse-Decimal $record.SharesCF
    
    # Parse loan data
    $normalCD = Parse-Decimal $record.NormalCD
    $emerg1CD = Parse-Decimal $record.Emerg1CD
    $emerg2CD = Parse-Decimal $record.Emerg2CD
    
    # Update shares by employee_id
    if ($sharesCF -gt 0) {
        $sql += @"

-- $fullName (Payroll: $payrollNum) - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = $sharesCF
WHERE m.employee_id = '$payrollNum'
  AND a.account_type = 'SHARES';

"@
    }
    
    # Update Normal Loan
    if ($normalCD -gt 0) {
        $normalBF = Parse-Decimal $record.NormalBF
        $normalP = Parse-Decimal $record.NormalP
        $normalI = Parse-Decimal $record.NormalI
        
        $sql += @"

-- $fullName (Payroll: $payrollNum) - Normal Loan
UPDATE loans 
SET 
  amount = $normalBF,
  outstanding_balance = $normalCD,
  principal_repaid = $normalP,
  interest_collected = $normalI,
  original_principal = $normalBF
WHERE member_id = (SELECT id FROM members WHERE employee_id = '$payrollNum')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

"@
        $matched++
    }
    
    # Update Emergency Loan 1
    if ($emerg1CD -gt 0) {
        $emerg1BF = Parse-Decimal $record.Emerg1BF
        $emerg1P = Parse-Decimal $record.Emerg1P
        $emerg1I = Parse-Decimal $record.Emerg1I
        
        $sql += @"

-- $fullName (Payroll: $payrollNum) - Emergency 1
UPDATE loans 
SET 
  amount = $emerg1BF,
  outstanding_balance = $emerg1CD,
  principal_repaid = $emerg1P,
  interest_collected = $emerg1I,
  original_principal = $emerg1BF
WHERE member_id = (SELECT id FROM members WHERE employee_id = '$payrollNum')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

"@
    }
    
    # Update Emergency Loan 2
    if ($emerg2CD -gt 0) {
        $emerg2BF = Parse-Decimal $record.Emerg2BF
        $emerg2P = Parse-Decimal $record.Emerg2P
        $emerg2I = Parse-Decimal $record.Emerg2I
        
        $sql += @"

-- $fullName (Payroll: $payrollNum) - Emergency 2
UPDATE loans 
SET 
  amount = $emerg2BF,
  outstanding_balance = $emerg2CD,
  principal_repaid = $emerg2P,
  interest_collected = $emerg2I,
  original_principal = $emerg2BF
WHERE member_id = (SELECT id FROM members WHERE employee_id = '$payrollNum')
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1 OFFSET 1;

"@
    }
}

$sql += @"

SET SQL_SAFE_UPDATES = 1;

-- Summary
SELECT 'Fix complete' AS Status;
SELECT COUNT(*) AS loans_with_data, SUM(outstanding_balance) AS total_outstanding 
FROM loans WHERE outstanding_balance > 0;
"@

# Save SQL
$OUTPUT_FILE = ".\FINAL_FIX_BY_EMPLOYEE_ID.sql"
$sql | Out-File -FilePath $OUTPUT_FILE -Encoding UTF8

Write-Host "OK Generated SQL for $matched loan updates" -ForegroundColor Green
Write-Host "⚠️  Skipped: $skipped records (no payroll #)" -ForegroundColor Yellow
Write-Host ""

# Execute
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Execute final fix? (yes/no)" -ForegroundColor White
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$confirm = Read-Host "Type yes to continue"

if ($confirm -eq "yes") {
    Write-Host ""
    Write-Host "Executing..." -ForegroundColor Yellow
    
    Get-Content $OUTPUT_FILE | mysql -u $MYSQL_USER -p"$MYSQL_PASSWORD" $DATABASE_NAME 2>&1 | Where-Object { $_ -notmatch "Warning" }
    
    Write-Host ""
    Write-Host "✅ COMPLETE!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Verifying results..." -ForegroundColor Yellow
    mysql -u $MYSQL_USER -p"$MYSQL_PASSWORD" $DATABASE_NAME -e "SELECT COUNT(*) as total_with_amount, SUM(amount) as total_principal, SUM(outstanding_balance) as total_outstanding FROM loans WHERE status IN ('DISBURSED', 'ACTIVE') AND amount > 0;" 2>&1 | Where-Object { $_ -notmatch "Warning" }
    
    Write-Host ""
    Write-Host "🔄 RESTART THE BACKEND to see changes!" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "Cancelled. SQL saved: $OUTPUT_FILE" -ForegroundColor Yellow
}

Write-Host ""
