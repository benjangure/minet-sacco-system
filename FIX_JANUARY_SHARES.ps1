# Fix January 2026 Shares - Use B/F - 3000 as opening balance
# The previous import used C/F incorrectly

$ErrorActionPreference = "Stop"

$MYSQL_USER = "minetsacco"
$MYSQL_PASSWORD = "0a0b0c0D."
$DATABASE_NAME = "minetsacco"
$CSV_FILE = "C:\Users\Lenovo\Downloads\01 2026 - Listing Final(Listing).csv"

Write-Host "=== FIX JANUARY 2026 SHARES ===" -ForegroundColor Cyan
Write-Host "Correct formula: Opening Balance = B/F - 3000" -ForegroundColor Yellow
Write-Host ""

# Read CSV
$csvContent = Get-Content $CSV_FILE -Encoding UTF8

# Find header
$headerIndex = 0
for ($i = 0; $i -lt $csvContent.Count; $i++) {
    if ($csvContent[$i] -match "Payroll #|PayrollNum") {
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

$sql = @"
USE $DATABASE_NAME;
SET SQL_SAFE_UPDATES = 0;

-- Fix January 2026 Shares - Use B/F - 3000
-- Generated: $(Get-Date)

"@

$updated = 0

foreach ($record in $csvRecords) {
    $payrollNum = $record.PayrollNum.Trim()
    $fullName = $record.FullName.Trim()
    
    if ([string]::IsNullOrWhiteSpace($fullName) -or $fullName -match 'TOTAL|FullName') {
        continue
    }
    
    if ([string]::IsNullOrWhiteSpace($payrollNum)) {
        continue
    }
    
    # Parse shares B/F
    $sharesBF = Parse-Decimal $record.SharesBF
    
    # Calculate opening balance: B/F - 3000
    $sharesOpening = $sharesBF - 3000
    
    if ($sharesOpening -gt 0) {
        $sql += @"

-- $fullName (Payroll: $payrollNum) - Shares Opening = B/F($sharesBF) - 3000
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = $sharesOpening
WHERE m.employee_id = '$payrollNum'
  AND a.account_type = 'SHARES';

"@
        $updated++
        Write-Host "✓ $payrollNum : $fullName - Shares = KES $sharesOpening (B/F $sharesBF - 3000)" -ForegroundColor Green
    }
}

$sql += @"

SET SQL_SAFE_UPDATES = 1;

SELECT COUNT(*) as accounts_updated, SUM(balance) as total_shares
FROM accounts WHERE account_type = 'SHARES' AND balance > 0;
"@

# Save SQL
$OUTPUT_FILE = ".\FIX_JANUARY_SHARES.sql"
$sql | Out-File -FilePath $OUTPUT_FILE -Encoding UTF8

Write-Host ""
Write-Host "=== Summary ===" -ForegroundColor Cyan
Write-Host "Shares accounts to update: $updated" -ForegroundColor Yellow
Write-Host "SQL saved: $OUTPUT_FILE" -ForegroundColor Cyan
Write-Host ""

# Execute
Write-Host "Execute fix? (yes/no)" -ForegroundColor White
$confirm = Read-Host

if ($confirm -eq "yes") {
    Write-Host "Executing..." -ForegroundColor Yellow
    Get-Content $OUTPUT_FILE | mysql -u $MYSQL_USER -p"$MYSQL_PASSWORD" $DATABASE_NAME 2>&1 | Where-Object { $_ -notmatch "Warning" }
    
    Write-Host ""
    Write-Host "✅ January shares fixed!" -ForegroundColor Green
} else {
    Write-Host "Cancelled" -ForegroundColor Yellow
}
