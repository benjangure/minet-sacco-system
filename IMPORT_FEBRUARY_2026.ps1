# Import February 2026 Data
# This script imports February 2026 loan balances using employee ID matching

$ErrorActionPreference = "Stop"

# Database connection details
$dbUser = "minetsacco"
$dbPass = "0a0b0c0D."
$dbName = "minetsacco"

# Read CSV file, skipping the first 6 header rows
$csvContent = Get-Content "C:\Users\Lenovo\Downloads\02_2026_Listing_Final.csv" | Select-Object -Skip 6
$tempCsvPath = "C:\Users\Lenovo\Downloads\temp_feb_2026.csv"
$csvContent | Set-Content $tempCsvPath

$csv = Import-Csv $tempCsvPath

Write-Host "=== February 2026 Data Import ===" -ForegroundColor Cyan
Write-Host "Total rows in CSV: $($csv.Count)" -ForegroundColor Yellow

# Generate SQL file
$sqlFile = "c:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\IMPORT_FEBRUARY_2026.sql"
$sql = @"
-- Import February 2026 Data
-- Generated: $(Get-Date)

USE minetsacco;

"@

$successCount = 0
$skipCount = 0
$errorCount = 0

foreach ($row in $csv) {
    try {
        # Clean the payroll number (remove spaces)
        $payrollNum = $row.'Payroll #'.Trim()
        
        if ([string]::IsNullOrWhiteSpace($payrollNum)) {
            continue
        }
        
        # Clean and parse numeric values (remove quotes, commas, spaces)
        $sharesBF = $row.' B/F '.Replace('"', '').Replace(',', '').Replace(' ', '').Trim()
        $sharesI = $row.' I '.Replace('"', '').Replace(',', '').Replace(' ', '').Trim()
        $sharesCF = $row.' Balance C/F '.Replace('"', '').Replace(',', '').Replace(' ', '').Trim()
        
        # Normal Loan columns
        $normalBF = $row.' B/F '.Replace('"', '').Replace(',', '').Replace(' ', '').Trim()
        $normalP = $row.' P '.Replace('"', '').Replace(',', '').Replace(' ', '').Trim()
        $normalI = $row.' I '.Replace('"', '').Replace(',', '').Replace(' ', '').Trim()
        $normalCD = $row.' C/D '.Replace('"', '').Replace(',', '').Replace(' ', '').Trim()
        
        # Convert dashes to 0
        if ($sharesBF -eq '-' -or $sharesBF -eq '') { $sharesBF = '0' }
        if ($sharesI -eq '-' -or $sharesI -eq '') { $sharesI = '0' }
        if ($sharesCF -eq '-' -or $sharesCF -eq '') { $sharesCF = '0' }
        if ($normalBF -eq '-' -or $normalBF -eq '') { $normalBF = '0' }
        if ($normalP -eq '-' -or $normalP -eq '') { $normalP = '0' }
        if ($normalI -eq '-' -or $normalI -eq '') { $normalI = '0' }
        if ($normalCD -eq '-' -or $normalCD -eq '') { $normalCD = '0' }
        
        # Parse to decimal
        $sharesCFValue = [decimal]$sharesCF
        $normalCDValue = [decimal]$normalCD
        
        # Update shares account
        if ($sharesCFValue -gt 0) {
            $sql += @"

-- Update shares for employee $payrollNum
UPDATE shares_accounts sa
JOIN members m ON sa.member_id = m.id
SET sa.current_balance = $sharesCFValue,
    sa.last_updated = NOW()
WHERE m.employee_id = '$payrollNum';

"@
        }
        
        # Update loan balance (use C/D as the outstanding balance)
        if ($normalCDValue -gt 0) {
            $sql += @"

-- Update loan for employee $payrollNum  
UPDATE loans l
JOIN members m ON l.member_id = m.id
SET l.amount = $normalCDValue,
    l.outstanding_balance = $normalCDValue,
    l.updated_at = NOW()
WHERE m.employee_id = '$payrollNum'
  AND l.loan_product_id = (SELECT id FROM loan_products WHERE name = 'Normal Loan')
  AND l.status IN ('DISBURSED', 'ACTIVE');

"@
            $successCount++
            Write-Host "✓ Employee $payrollNum : Loan C/D = KES $normalCDValue" -ForegroundColor Green
        } else {
            $skipCount++
        }
        
    } catch {
        $errorCount++
        Write-Host "✗ Error processing row: $_" -ForegroundColor Red
    }
}

# Save SQL file
$sql | Out-File -FilePath $sqlFile -Encoding UTF8

Write-Host "`n=== Summary ===" -ForegroundColor Cyan
Write-Host "Successfully processed: $successCount" -ForegroundColor Green
Write-Host "Skipped (no balance): $skipCount" -ForegroundColor Yellow
Write-Host "Errors: $errorCount" -ForegroundColor Red
Write-Host "`nSQL file created: $sqlFile" -ForegroundColor Cyan

# Execute the SQL
Write-Host "`n=== Executing SQL Updates ===" -ForegroundColor Yellow
$result = mysql -u $dbUser -p"$dbPass" $dbName < "$sqlFile" 2>&1 | Where-Object { $_ -notmatch "Warning" }

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ February 2026 data imported successfully!" -ForegroundColor Green
} else {
    Write-Host "✗ Error executing SQL:" -ForegroundColor Red
    Write-Host $result
}

# Cleanup temp file
Remove-Item $tempCsvPath -ErrorAction SilentlyContinue
