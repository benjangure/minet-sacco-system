# ========================================
# IMPORT JANUARY 2026 DATA - SIMPLIFIED
# ========================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  JANUARY 2026 DATA IMPORT" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$CSV_FILE = "C:\Users\Lenovo\Downloads\01 2026 - Listing Final(Listing).csv"
$MYSQL_USER = "minetsacco"
$MYSQL_PASSWORD = "0a0b0c0D."
$DATABASE_NAME = "minetsacco"
$OUTPUT_SQL_FILE = ".\JANUARY_2026_IMPORT.sql"
$BACKUP_DIR = ".\database_backups"

# Create backup directory
if (!(Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR | Out-Null
}

# ========================================
# STEP 1: BACKUP
# ========================================
Write-Host "STEP 1: Creating backup..." -ForegroundColor Yellow

$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"
$BACKUP_FILENAME = "backup_jan2026_$TIMESTAMP.sql"
$BACKUP_PATH = Join-Path $BACKUP_DIR $BACKUP_FILENAME

try {
    & mysqldump -u $MYSQL_USER -p"$MYSQL_PASSWORD" $DATABASE_NAME > $BACKUP_PATH
    $backupSize = (Get-Item $BACKUP_PATH).Length / 1MB
    Write-Host "OK Backup created: $([math]::Round($backupSize, 2)) MB" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Backup failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""

# ========================================
# STEP 2: READ CSV
# ========================================
Write-Host "STEP 2: Reading CSV..." -ForegroundColor Yellow

if (!(Test-Path $CSV_FILE)) {
    Write-Host "ERROR: CSV not found" -ForegroundColor Red
    exit 1
}

$csvContent = Get-Content $CSV_FILE -Encoding UTF8
Write-Host "OK Found $($csvContent.Count) lines" -ForegroundColor Green
Write-Host ""

# ========================================
# STEP 3: GENERATE SQL
# ========================================
Write-Host "STEP 3: Generating SQL..." -ForegroundColor Yellow

$sql = @"
-- ========================================
-- JANUARY 2026 IMPORT
-- Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
-- ========================================

USE $DATABASE_NAME;

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- ========================================
-- CLEAR FINANCIAL DATA
-- ========================================
DELETE FROM loan_repayments;
DELETE FROM loan_topup_history;
DELETE FROM transactions;

UPDATE loans SET 
  amount = 0,
  outstanding_balance = 0,
  interest_collected = 0,
  principal_repaid = 0,
  total_interest = 0,
  monthly_repayment = 0,
  total_repayable = 0;

UPDATE accounts SET balance = 0;

SELECT 'Financial data cleared' AS Status;

-- ========================================
-- IMPORT JANUARY 2026 DATA
-- ========================================

"@

# Find header line
$headerIndex = 0
for ($i = 0; $i -lt $csvContent.Count; $i++) {
    if ($csvContent[$i] -match "Payroll #") {
        $headerIndex = $i
        break
    }
}

# Import CSV properly starting from header
$dataLines = $csvContent[($headerIndex)..$($csvContent.Count-1)]
$tempCsvFile = [System.IO.Path]::GetTempFileName()
$dataLines | Out-File -FilePath $tempCsvFile -Encoding UTF8

# Use PowerShell's CSV parser which handles quoted commas
$csvRecords = Import-Csv -Path $tempCsvFile -Header @(
    'PayrollNum', 'FullName', 'SharesBF', 'SharesI', 'SharesCF',
    'NormalBF', 'NormalP', 'NormalI', 'NormalCD',
    'Emerg1BF', 'Emerg1P', 'Emerg1I', 'Emerg1CD',
    'Emerg2BF', 'Emerg2P', 'Emerg2I', 'Emerg2CD',
    'GuarBF', 'GuarP', 'GuarCD', 'Extra1', 'Extra2'
) | Select-Object -Skip 1  # Skip the header row itself

Remove-Item $tempCsvFile

$recordCount = 0

# Parse data lines using proper CSV parser
foreach ($record in $csvRecords) {
    # Get values from record
    $payrollNum = $record.PayrollNum.Trim()
    $fullName = $record.FullName.Trim()
    
    # Skip empty or summary lines
    if ([string]::IsNullOrWhiteSpace($fullName) -or $fullName -match 'TOTAL|Grand Total' -or $fullName -match 'FullName') {
        continue
    }
    
    # Clean name for SQL
    $cleanName = $fullName -replace "'", "''"
    
    # Helper function to parse decimal
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
    
    # Parse shares
    $sharesCF = Parse-Decimal $record.SharesCF
    if ($sharesCF -gt 0) {
        $sql += @"

-- $fullName - Shares
UPDATE accounts a
JOIN members m ON a.member_id = m.id
SET a.balance = $sharesCF
WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('$cleanName'))
  AND a.account_type = 'SHARES';

"@
    }
    
    # Parse Normal Loan
    $normalCD = Parse-Decimal $record.NormalCD
    if ($normalCD -gt 0) {
        $normalBF = Parse-Decimal $record.NormalBF
        $normalP = Parse-Decimal $record.NormalP
        $normalI = Parse-Decimal $record.NormalI
            
        $sql += @"

-- $fullName - Normal Loan
UPDATE loans l
SET 
  l.outstanding_balance = $normalCD,
  l.principal_repaid = $normalP,
  l.interest_collected = $normalI,
  l.amount = $normalBF,
  l.original_principal = $normalBF
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('$cleanName'))
      AND lp.name LIKE '%Normal%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

"@
    }
    
    # Parse Emergency Loan 1
    $emerg1CD = Parse-Decimal $record.Emerg1CD
    if ($emerg1CD -gt 0) {
        $emerg1BF = Parse-Decimal $record.Emerg1BF
        $emerg1P = Parse-Decimal $record.Emerg1P
        $emerg1I = Parse-Decimal $record.Emerg1I
                
        $sql += @"

-- $fullName - Emergency Loan 1
UPDATE loans l
SET 
  l.outstanding_balance = $emerg1CD,
  l.principal_repaid = $emerg1P,
  l.interest_collected = $emerg1I,
  l.amount = $emerg1BF,
  l.original_principal = $emerg1BF
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('$cleanName'))
      AND lp.name LIKE '%Emergency%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC 
    LIMIT 1
  ) AS temp
);

"@
    }
    
    # Parse Emergency Loan 2
    $emerg2CD = Parse-Decimal $record.Emerg2CD
    if ($emerg2CD -gt 0) {
        $emerg2BF = Parse-Decimal $record.Emerg2BF
        $emerg2P = Parse-Decimal $record.Emerg2P
        $emerg2I = Parse-Decimal $record.Emerg2I
                
        $sql += @"

-- $fullName - Emergency Loan 2
UPDATE loans l
SET 
  l.outstanding_balance = $emerg2CD,
  l.principal_repaid = $emerg2P,
  l.interest_collected = $emerg2I,
  l.amount = $emerg2BF,
  l.original_principal = $emerg2BF
WHERE l.id = (
  SELECT id FROM (
    SELECT l2.id 
    FROM loans l2
    JOIN members m ON l2.member_id = m.id
    JOIN loan_products lp ON l2.loan_product_id = lp.id
    WHERE UPPER(TRIM(m.full_name)) = UPPER(TRIM('$cleanName'))
      AND lp.name LIKE '%Emergency%'
      AND l2.status IN ('DISBURSED', 'ACTIVE')
    ORDER BY l2.disbursement_date DESC
    LIMIT 1 OFFSET 1
  ) AS temp
);

"@
    }
    
    $recordCount++
}

$sql += @"

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

-- ========================================
-- SUMMARY
-- ========================================
SELECT 'Import complete' AS Status;
SELECT SUM(balance) AS total_shares FROM accounts WHERE account_type = 'SHARES';
SELECT SUM(outstanding_balance) AS total_loans FROM loans WHERE status IN ('DISBURSED', 'ACTIVE');

"@

# Save SQL
$sql | Out-File -FilePath $OUTPUT_SQL_FILE -Encoding UTF8
Write-Host "OK Generated SQL for $recordCount members" -ForegroundColor Green
Write-Host ""

# ========================================
# STEP 4: CONFIRM
# ========================================
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  READY TO IMPORT" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Members to process: $recordCount" -ForegroundColor White
Write-Host "Backup: $BACKUP_FILENAME" -ForegroundColor White
Write-Host "SQL file: $OUTPUT_SQL_FILE" -ForegroundColor White
Write-Host ""
Write-Host "This will CLEAR all financial data and import January 2026!" -ForegroundColor Yellow
Write-Host ""

$confirm = Read-Host "Type IMPORT to proceed"

if ($confirm -eq "IMPORT") {
    Write-Host ""
    Write-Host "Executing import..." -ForegroundColor Yellow
    
    try {
        Get-Content $OUTPUT_SQL_FILE | mysql -u $MYSQL_USER -p"$MYSQL_PASSWORD" $DATABASE_NAME 2>&1
        
        Write-Host ""
        Write-Host "================================================" -ForegroundColor Green
        Write-Host "SUCCESS" -ForegroundColor Green
        Write-Host "================================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "January 2026 data imported!" -ForegroundColor Green
        Write-Host "Backup saved: $BACKUP_PATH" -ForegroundColor Gray
        Write-Host ""
        
    } catch {
        Write-Host ""
        Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host ""
        Write-Host "To rollback:" -ForegroundColor Yellow
        Write-Host "  mysql -u $MYSQL_USER $DATABASE_NAME < $BACKUP_PATH" -ForegroundColor Gray
    }
} else {
    Write-Host ""
    Write-Host "Cancelled. SQL saved at: $OUTPUT_SQL_FILE" -ForegroundColor Yellow
}

Write-Host ""
