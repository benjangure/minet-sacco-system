# ========================================
# FIX LOAN MATCHING - Smart Name Matching
# ========================================
# This script fixes loans that didn't match due to name format differences
# CSV: "MBURU FREDRICK MAINA" vs DB: "Mr Fredrick Maina Mburu"
# ========================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  FIX LOAN MATCHING" -ForegroundColor Cyan
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

# Get all members from database
Write-Host "Fetching members from database..." -ForegroundColor Yellow
$members = @{}
$memberQuery = "SELECT id, full_name, employee_id FROM members;" | mysql -u $MYSQL_USER -p"$MYSQL_PASSWORD" $DATABASE_NAME -s -N 2>&1 | Where-Object { $_ -notmatch "Warning" }

foreach ($line in $memberQuery) {
    $parts = $line -split "`t"
    if ($parts.Count -ge 3) {
        $id = $parts[0]
        $fullName = $parts[1]
        $empId = $parts[2]
        
        # Create lookup key - remove titles, spaces, make uppercase
        $cleanName = $fullName -replace '(Mr |Ms |Mrs |Miss |Dr |Prof )', '' -replace '\s+', '' -replace '\.', '' 
        $cleanName = $cleanName.ToUpper()
        
        $members[$cleanName] = @{
            id = $id
            full_name = $fullName
            employee_id = $empId
        }
        
        # Also add reverse name (LAST FIRST MIDDLE)
        $nameParts = $fullName -replace '(Mr |Ms |Mrs |Miss |Dr |Prof )', '' -split '\s+'
        if ($nameParts.Count -ge 2) {
            $reverseName = ($nameParts[-1] + $nameParts[0..($nameParts.Count-2)]) -join ''
            $reverseName = $reverseName.ToUpper()
            $members[$reverseName] = @{
                id = $id
                full_name = $fullName
                employee_id = $empId
            }
        }
    }
}

Write-Host "OK Loaded $($members.Count) member name variations" -ForegroundColor Green
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

# Generate update SQL
Write-Host "Matching members and generating SQL..." -ForegroundColor Yellow
$sql = @"
USE $DATABASE_NAME;
SET SQL_SAFE_UPDATES = 0;

-- ========================================
-- FIX LOAN MATCHING
-- ========================================

"@

$matched = 0
$notMatched = @()

foreach ($record in $csvRecords) {
    $fullName = $record.FullName.Trim()
    
    if ([string]::IsNullOrWhiteSpace($fullName) -or $fullName -match 'TOTAL|FullName') {
        continue
    }
    
    # Clean name for matching
    $cleanName = $fullName -replace '\s+', '' -replace '\.', ''
    $cleanName = $cleanName.ToUpper()
    
    # Try to find member
    $member = $null
    if ($members.ContainsKey($cleanName)) {
        $member = $members[$cleanName]
    } else {
        # Try reversing name parts
        $nameParts = $fullName -split '\s+'
        if ($nameParts.Count -ge 2) {
            $reverseName = ($nameParts[-1] + $nameParts[0..($nameParts.Count-2)]) -join ''
            $reverseName = $reverseName.ToUpper()
            if ($members.ContainsKey($reverseName)) {
                $member = $members[$reverseName]
            }
        }
    }
    
    if ($null -eq $member) {
        $notMatched += $fullName
        continue
    }
    
    $memberId = $member.id
    $dbName = $member.full_name
    
    # Parse loan data
    $normalCD = Parse-Decimal $record.NormalCD
    $emerg1CD = Parse-Decimal $record.Emerg1CD
    $emerg2CD = Parse-Decimal $record.Emerg2CD
    
    if ($normalCD -gt 0) {
        $normalBF = Parse-Decimal $record.NormalBF
        $normalP = Parse-Decimal $record.NormalP
        $normalI = Parse-Decimal $record.NormalI
        
        $sql += @"

-- $dbName (CSV: $fullName)
UPDATE loans 
SET 
  amount = $normalBF,
  outstanding_balance = $normalCD,
  principal_repaid = $normalP,
  interest_collected = $normalI,
  original_principal = $normalBF
WHERE member_id = $memberId
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Normal%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

"@
        $matched++
    }
    
    if ($emerg1CD -gt 0) {
        $emerg1BF = Parse-Decimal $record.Emerg1BF
        $emerg1P = Parse-Decimal $record.Emerg1P
        $emerg1I = Parse-Decimal $record.Emerg1I
        
        $sql += @"

-- $dbName Emergency 1 (CSV: $fullName)
UPDATE loans 
SET 
  amount = $emerg1BF,
  outstanding_balance = $emerg1CD,
  principal_repaid = $emerg1P,
  interest_collected = $emerg1I,
  original_principal = $emerg1BF
WHERE member_id = $memberId
  AND loan_product_id IN (SELECT id FROM loan_products WHERE name LIKE '%Emergency%')
  AND status IN ('DISBURSED', 'ACTIVE')
ORDER BY disbursement_date DESC
LIMIT 1;

"@
    }
    
    if ($emerg2CD -gt 0) {
        $emerg2BF = Parse-Decimal $record.Emerg2BF
        $emerg2P = Parse-Decimal $record.Emerg2P
        $emerg2I = Parse-Decimal $record.Emerg2I
        
        $sql += @"

-- $dbName Emergency 2 (CSV: $fullName)
UPDATE loans 
SET 
  amount = $emerg2BF,
  outstanding_balance = $emerg2CD,
  principal_repaid = $emerg2P,
  interest_collected = $emerg2I,
  original_principal = $emerg2BF
WHERE member_id = $memberId
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
SELECT COUNT(*) AS loans_updated, SUM(outstanding_balance) AS total_outstanding 
FROM loans WHERE outstanding_balance > 0;
"@

# Save SQL
$OUTPUT_FILE = ".\FIX_LOAN_MATCHING.sql"
$sql | Out-File -FilePath $OUTPUT_FILE -Encoding UTF8

Write-Host "OK Matched $matched loans" -ForegroundColor Green
if ($notMatched.Count -gt 0) {
    Write-Host "⚠️  Not matched: $($notMatched.Count) members" -ForegroundColor Yellow
}
Write-Host ""

# Execute
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "Matched: $matched | Not matched: $($notMatched.Count)" -ForegroundColor White
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

$confirm = Read-Host "Execute fix? (yes/no)"

if ($confirm -eq "yes") {
    Write-Host ""
    Write-Host "Executing..." -ForegroundColor Yellow
    
    Get-Content $OUTPUT_FILE | mysql -u $MYSQL_USER -p"$MYSQL_PASSWORD" $DATABASE_NAME 2>&1 | Where-Object { $_ -notmatch "Warning" }
    
    Write-Host ""
    Write-Host "✅ DONE!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Verify:" -ForegroundColor Yellow
    Write-Host "  mysql -u $MYSQL_USER -p`"$MYSQL_PASSWORD`" $DATABASE_NAME -e `"SELECT COUNT(*) FROM loans WHERE outstanding_balance > 0;`"" -ForegroundColor Gray
} else {
    Write-Host ""
    Write-Host "Cancelled. SQL saved: $OUTPUT_FILE" -ForegroundColor Yellow
}

Write-Host ""
