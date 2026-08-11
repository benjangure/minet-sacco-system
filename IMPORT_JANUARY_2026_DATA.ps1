# ========================================
# IMPORT JANUARY 2026 DATA FROM CSV
# ========================================
# This script:
# 1. Backs up the database
# 2. Clears all financial data
# 3. Imports January 2026 data from CSV
# 4. Matches members by name
# 5. Updates loans and shares
# ========================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  MINET SACCO - JANUARY 2026 DATA IMPORT" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$CSV_FILE = "C:\Users\Lenovo\Downloads\01 2026 - Listing Final(Listing).csv"
$MYSQL_USER = "root"
$MYSQL_PASSWORD = ""
$DATABASE_NAME = "sacco_db"
$OUTPUT_SQL_FILE = ".\JANUARY_2026_IMPORT.sql"
$BACKUP_DIR = ".\database_backups"

# ========================================
# STEP 1: BACKUP DATABASE
# ========================================
Write-Host "STEP 1: Creating database backup..." -ForegroundColor Yellow
Write-Host ""

$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"
$BACKUP_FILENAME = "backup_before_january_2026_import_$TIMESTAMP.sql"

if (!(Test-Path $BACKUP_DIR)) {
    New-Item -ItemType Directory -Path $BACKUP_DIR | Out-Null
}

$BACKUP_PATH = Join-Path $BACKUP_DIR $BACKUP_FILENAME

Write-Host "💾 Creating backup: $BACKUP_FILENAME" -ForegroundColor Gray

try {
    if ($MYSQL_PASSWORD -eq "") {
        & mysqldump -u $MYSQL_USER $DATABASE_NAME > $BACKUP_PATH
    } else {
        & mysqldump -u $MYSQL_USER -p$MYSQL_PASSWORD $DATABASE_NAME > $BACKUP_PATH
    }

    if (Test-Path $BACKUP_PATH) {
        $backupSize = (Get-Item $BACKUP_PATH).Length / 1MB
        Write-Host "✅ Backup created: $([math]::Round($backupSize, 2)) MB" -ForegroundColor Green
    } else {
        throw "Backup file not created"
    }
} catch {
    Write-Host "❌ BACKUP FAILED! Cannot proceed." -ForegroundColor Red
    exit 1
}

Write-Host ""

# ========================================
# STEP 2: READ AND PARSE CSV
# ========================================
Write-Host "STEP 2: Reading CSV file..." -ForegroundColor Yellow
Write-Host ""

if (!(Test-Path $CSV_FILE)) {
    Write-Host "❌ CSV file not found: $CSV_FILE" -ForegroundColor Red
    exit 1
}

# Read CSV with proper encoding
$csvContent = Get-Content $CSV_FILE -Encoding UTF8
Write-Host "📄 Found $($csvContent.Count) lines in CSV" -ForegroundColor Gray

# Find the header row (contains "Payroll #")
$headerIndex = 0
for ($i = 0; $i -lt $csvContent.Count; $i++) {
    if ($csvContent[$i] -match "Payroll #") {
        $headerIndex = $i
        break
    }
}

Write-Host "📋 Header found at line $($headerIndex + 1)" -ForegroundColor Gray

# Parse CSV starting from header
$csvData = @()
for ($i = $headerIndex + 1; $i -lt $csvContent.Count; $i++) {
    $line = $csvContent[$i]
    
    # Skip empty lines or summary lines
    if ($line -match '^\s*$' -or $line -match 'TOTAL|Grand Total') {
        continue
    }
    
    # Split by comma
    $fields = $line -split ','
    
    if ($fields.Count -ge 6) {
        $payrollNum = $fields[0].Trim()
        $fullName = $fields[1].Trim()
        
        # Skip if no name
        if ([string]::IsNullOrWhiteSpace($fullName)) {
            continue
        }
        
        # Parse financial data (remove commas and spaces)
        $sharesBF = $fields[2].Trim() -replace '[,\s]', ''
        $sharesContrib = $fields[3].Trim() -replace '[,\s]', ''
        $sharesCF = $fields[4].Trim() -replace '[,\s]', ''
        
        # Normal Loan
        $normalBF = $fields[5].Trim() -replace '[,\s]', ''
        $normalP = $fields[6].Trim() -replace '[,\s]', ''
        $normalI = $fields[7].Trim() -replace '[,\s]', ''
        $normalCD = $fields[8].Trim() -replace '[,\s]', ''
        
        # Emergency Loan 1
        $emerg1BF = if ($fields.Count -gt 9) { $fields[9].Trim() -replace '[,\s]', '' } else { '' }
        $emerg1P = if ($fields.Count -gt 10) { $fields[10].Trim() -replace '[,\s]', '' } else { '' }
        $emerg1I = if ($fields.Count -gt 11) { $fields[11].Trim() -replace '[,\s]', '' } else { '' }
        $emerg1CD = if ($fields.Count -gt 12) { $fields[12].Trim() -replace '[,\s]', '' } else { '' }
        
        # Emergency Loan 2
        $emerg2BF = if ($fields.Count -gt 13) { $fields[13].Trim() -replace '[,\s]', '' } else { '' }
        $emerg2P = if ($fields.Count -gt 14) { $fields[14].Trim() -replace '[,\s]', '' } else { '' }
        $emerg2I = if ($fields.Count -gt 15) { $fields[15].Trim() -replace '[,\s]', '' } else { '' }
        $emerg2CD = if ($fields.Count -gt 16) { $fields[16].Trim() -replace '[,\s]', '' } else { '' }
        
        # Guarantors
        $guarantorBF = if ($fields.Count -gt 17) { $fields[17].Trim() -replace '[,\s]', '' } else { '' }
        $guarantorP = if ($fields.Count -gt 18) { $fields[18].Trim() -replace '[,\s]', '' } else { '' }
        $guarantorCD = if ($fields.Count -gt 19) { $fields[19].Trim() -replace '[,\s]', '' } else { '' }
        
        $csvData += [PSCustomObject]@{
            PayrollNumber = $payrollNum
            FullName = $fullName
            SharesBF = $sharesBF
            SharesContribution = $sharesContrib
            SharesCF = $sharesCF
            NormalLoanBF = $normalBF
            NormalLoanPrincipal = $normalP
            NormalLoanInterest = $normalI
            NormalLoanCD = $normalCD
            Emergency1BF = $emerg1BF
            Emergency1Principal = $emerg1P
            Emergency1Interest = $emerg1I
            Emergency1CD = $emerg1CD
            Emergency2BF = $emerg2BF
            Emergency2Principal = $emerg2P
            Emergency2Interest = $emerg2I
            Emergency2CD = $emerg2CD
            GuarantorBF = $guarantorBF
            GuarantorPrincipal = $guarantorP
            GuarantorCD = $guarantorCD
        }
    }
}

Write-Host "✅ Parsed $($csvData.Count) member records" -ForegroundColor Green
Write-Host ""

# ========================================
# STEP 3: GENERATE SQL
# ========================================
Write-Host "STEP 3: Generating SQL import script..." -ForegroundColor Yellow
Write-Host ""

$sqlCommands = @()
$sqlCommands += "-- ========================================"
$sqlCommands += "-- JANUARY 2026 DATA IMPORT"
$sqlCommands += "-- Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$sqlCommands += "-- Source: $CSV_FILE"
$sqlCommands += "-- Members: $($csvData.Count)"
$sqlCommands += "-- ========================================"
$sqlCommands += ""
$sqlCommands += "USE $DATABASE_NAME;"
$sqlCommands += ""
$sqlCommands += 'SET FOREIGN_KEY_CHECKS = 0;'
$sqlCommands += 'SET SQL_SAFE_UPDATES = 0;'
$sqlCommands += ""

# Clear existing financial data
$sqlCommands += "-- ========================================"
$sqlCommands += "-- STEP 1: CLEAR EXISTING FINANCIAL DATA"
$sqlCommands += "-- ========================================"
$sqlCommands += ""
$sqlCommands += 'DELETE FROM loan_repayments;'
$sqlCommands += 'DELETE FROM loan_topup_history;'
$sqlCommands += 'DELETE FROM transactions;'
$sqlCommands += ''
$sqlCommands += '-- Reset loan financial fields'
$sqlCommands += 'UPDATE loans SET '
$sqlCommands += '  amount = 0,'
$sqlCommands += '  outstanding_balance = 0,'
$sqlCommands += '  interest_collected = 0,'
$sqlCommands += '  principal_repaid = 0,'
$sqlCommands += '  total_interest = 0,'
$sqlCommands += '  monthly_repayment = 0,'
$sqlCommands += '  total_repayable = 0;'
$sqlCommands += ''
$sqlCommands += '-- Reset account balances'
$sqlCommands += 'UPDATE accounts SET balance = 0;'
$sqlCommands += ''
$sqlCommands += "SELECT 'Financial data cleared' AS Status;"
$sqlCommands += ""

# Note: B/F is the opening balance for January 2026
# For shares: Balance C/F minus 3000 adjustment as mentioned
$sqlCommands += "-- ========================================"
$sqlCommands += "-- STEP 2: IMPORT JANUARY 2026 DATA"
$sqlCommands += "-- B/F = Opening Balance (minus 3000 for shares)"
$sqlCommands += "-- C/D = Closing/Carried Down Balance"
$sqlCommands += "-- ========================================"
$sqlCommands += ""

$membersProcessed = 0
$membersNotFound = @()

foreach ($record in $csvData) {
    $name = $record.FullName
    
    # Clean up name for SQL
    $cleanName = $name -replace "'", "''"
    
    $sqlCommands += "-- ========================================" 
    $sqlCommands += "-- $name (Payroll: $($record.PayrollNumber))"
    $sqlCommands += "-- ========================================" 
    $sqlCommands += ""
    
    # Function to convert string to decimal, handling empty/dash values
    function ConvertTo-Decimal($value) {
        if ([string]::IsNullOrWhiteSpace($value) -or $value -eq '-') {
            return 0
        }
        $cleaned = $value -replace '[,\s-]', ''
        try {
            return [decimal]$cleaned
        } catch {
            return 0
        }
    }
    
    $sharesCF = ConvertTo-Decimal $record.SharesCF
    $sharesOpening = if ($sharesCF -gt 0) { $sharesCF - 3000 } else { 0 }
    if ($sharesOpening -lt 0) { $sharesOpening = 0 }
    
    $normalCD = ConvertTo-Decimal $record.NormalLoanCD
    $emerg1CD = ConvertTo-Decimal $record.Emergency1CD
    $emerg2CD = ConvertTo-Decimal $record.Emergency2CD
    
    # Update shares balance
    if ($sharesCF -gt 0) {
        $sqlCommands += "UPDATE accounts a"
        $sqlCommands += "JOIN members m ON a.member_id = m.id"
        $sqlCommands += "SET a.balance = $sharesCF"
        $sqlCommands += "WHERE UPPER(TRIM(m.name)) = UPPER(TRIM('$cleanName'))"
        $sqlCommands += "  AND a.account_type = 'SHARES';"
        $sqlCommands += ""
    }
    
    # Update Normal Loan
    if ($normalCD -gt 0) {
        $normalBF = ConvertTo-Decimal $record.NormalLoanBF
        $normalP = ConvertTo-Decimal $record.NormalLoanPrincipal
        $normalI = ConvertTo-Decimal $record.NormalLoanInterest
        
        $sqlCommands += "-- Normal Loan: Opening=$normalBF, Principal Paid=$normalP, Interest=$normalI, Balance=$normalCD"
        $sqlCommands += "UPDATE loans l"
        $sqlCommands += "JOIN members m ON l.member_id = m.id"
        $sqlCommands += "JOIN loan_products lp ON l.loan_product_id = lp.id"
        $sqlCommands += "SET "
        $sqlCommands += "  l.outstanding_balance = $normalCD,"
        $sqlCommands += "  l.principal_repaid = $normalP,"
        $sqlCommands += "  l.interest_collected = $normalI,"
        $sqlCommands += "  l.amount = " + ($normalBF) + ","
        $sqlCommands += "  l.original_principal = " + ($normalBF)
        $sqlCommands += "WHERE UPPER(TRIM(m.name)) = UPPER(TRIM('$cleanName'))"
        $sqlCommands += "  AND lp.name LIKE '%Normal%'"
        $sqlCommands += "  AND l.status IN ('DISBURSED', 'ACTIVE')"
        $sqlCommands += "ORDER BY l.disbursement_date DESC LIMIT 1;"
        $sqlCommands += ""
    }
    
    # Update Emergency Loan 1
    if ($emerg1CD -gt 0) {
        $emerg1BF = ConvertTo-Decimal $record.Emergency1BF
        $emerg1P = ConvertTo-Decimal $record.Emergency1Principal
        $emerg1I = ConvertTo-Decimal $record.Emergency1Interest
        
        $sqlCommands += "-- Emergency Loan 1: Opening=$emerg1BF, Principal Paid=$emerg1P, Interest=$emerg1I, Balance=$emerg1CD"
        $sqlCommands += "UPDATE loans l"
        $sqlCommands += "JOIN members m ON l.member_id = m.id"
        $sqlCommands += "JOIN loan_products lp ON l.loan_product_id = lp.id"
        $sqlCommands += "SET "
        $sqlCommands += "  l.outstanding_balance = $emerg1CD,"
        $sqlCommands += "  l.principal_repaid = $emerg1P,"
        $sqlCommands += "  l.interest_collected = $emerg1I,"
        $sqlCommands += "  l.amount = " + ($emerg1BF) + ","
        $sqlCommands += "  l.original_principal = " + ($emerg1BF)
        $sqlCommands += "WHERE UPPER(TRIM(m.name)) = UPPER(TRIM('$cleanName'))"
        $sqlCommands += "  AND lp.name LIKE '%Emergency%'"
        $sqlCommands += "  AND l.status IN ('DISBURSED', 'ACTIVE')"
        $sqlCommands += "ORDER BY l.disbursement_date DESC LIMIT 1;"
        $sqlCommands += ""
    }
    
    # Update Emergency Loan 2 (if exists)
    if ($emerg2CD -gt 0) {
        $emerg2BF = ConvertTo-Decimal $record.Emergency2BF
        $emerg2P = ConvertTo-Decimal $record.Emergency2Principal
        $emerg2I = ConvertTo-Decimal $record.Emergency2Interest
        
        $sqlCommands += "-- Emergency Loan 2: Opening=$emerg2BF, Principal Paid=$emerg2P, Interest=$emerg2I, Balance=$emerg2CD"
        $sqlCommands += "UPDATE loans l"
        $sqlCommands += "JOIN members m ON l.member_id = m.id"
        $sqlCommands += "JOIN loan_products lp ON l.loan_product_id = lp.id"
        $sqlCommands += "SET "
        $sqlCommands += "  l.outstanding_balance = $emerg2CD,"
        $sqlCommands += "  l.principal_repaid = $emerg2P,"
        $sqlCommands += "  l.interest_collected = $emerg2I,"
        $sqlCommands += "  l.amount = " + ($emerg2BF) + ","
        $sqlCommands += "  l.original_principal = " + ($emerg2BF)
        $sqlCommands += "WHERE UPPER(TRIM(m.name)) = UPPER(TRIM('$cleanName'))"
        $sqlCommands += "  AND lp.name LIKE '%Emergency%'"
        $sqlCommands += "  AND l.status IN ('DISBURSED', 'ACTIVE')"
        $sqlCommands += "ORDER BY l.disbursement_date DESC"
        $sqlCommands += "LIMIT 1 OFFSET 1;"
        $sqlCommands += ""
    }
    
    $sqlCommands += ""
    $membersProcessed++
}

$sqlCommands += ""
$sqlCommands += "SET FOREIGN_KEY_CHECKS = 1;"
$sqlCommands += "SET SQL_SAFE_UPDATES = 1;"
$sqlCommands += ""
$sqlCommands += "-- ========================================"
$sqlCommands += "-- IMPORT COMPLETE"
$sqlCommands += "-- ========================================"
$sqlCommands += "SELECT 'January 2026 data imported successfully' AS Status;"
$sqlCommands += "SELECT COUNT(*) AS total_members FROM members;"
$sqlCommands += "SELECT SUM(balance) AS total_shares FROM accounts WHERE account_type = 'SHARES';"
$sqlCommands += "SELECT SUM(outstanding_balance) AS total_loans FROM loans WHERE status IN ('DISBURSED', 'ACTIVE');"

# Save SQL to file
Write-Host "💾 Saving SQL to file: $OUTPUT_SQL_FILE" -ForegroundColor Gray
$sqlCommands | Out-File -FilePath $OUTPUT_SQL_FILE -Encoding UTF8
Write-Host "✅ SQL file generated ($([math]::Round((Get-Item $OUTPUT_SQL_FILE).Length / 1KB, 2)) KB)" -ForegroundColor Green
Write-Host ""

# ========================================
# STEP 4: CONFIRM AND EXECUTE
# ========================================
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  READY TO IMPORT" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 Summary:" -ForegroundColor Yellow
Write-Host "   • CSV Records: $($csvData.Count)" -ForegroundColor White
Write-Host "   • Backup Created: $BACKUP_FILENAME" -ForegroundColor White
Write-Host "   • SQL File: $OUTPUT_SQL_FILE" -ForegroundColor White
Write-Host ""
Write-Host "⚠️  This will:" -ForegroundColor Yellow
Write-Host "   1. Delete all loan repayments" -ForegroundColor White
Write-Host "   2. Delete all transactions" -ForegroundColor White
Write-Host "   3. Reset all loan balances" -ForegroundColor White
Write-Host "   4. Reset all account balances" -ForegroundColor White
Write-Host "   5. Import January 2026 data" -ForegroundColor White
Write-Host ""

$confirm = Read-Host "Type 'IMPORT' to proceed"

if ($confirm -eq "IMPORT") {
    Write-Host ""
    Write-Host "🔄 Executing SQL import..." -ForegroundColor Yellow
    Write-Host ""
    
    try {
        if ($MYSQL_PASSWORD -eq "") {
            Get-Content $OUTPUT_SQL_FILE | mysql -u $MYSQL_USER $DATABASE_NAME
        } else {
            Get-Content $OUTPUT_SQL_FILE | mysql -u $MYSQL_USER -p$MYSQL_PASSWORD $DATABASE_NAME
        }
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "================================================" -ForegroundColor Green
            Write-Host "✅ IMPORT SUCCESSFUL!" -ForegroundColor Green
            Write-Host "================================================" -ForegroundColor Green
            Write-Host ""
            Write-Host "Next steps:" -ForegroundColor Yellow
            Write-Host "  1. Verify data in application" -ForegroundColor White
            Write-Host "  2. Check member loan balances" -ForegroundColor White
            Write-Host "  3. Review shares balances" -ForegroundColor White
            Write-Host "  4. Import data for February-May" -ForegroundColor White
            Write-Host ""
        } else {
            Write-Host ""
            Write-Host "❌ IMPORT FAILED!" -ForegroundColor Red
            Write-Host ""
            Write-Host "To rollback:" -ForegroundColor Yellow
            Write-Host "  mysql -u $MYSQL_USER $DATABASE_NAME < $BACKUP_PATH" -ForegroundColor Gray
        }
        
    } catch {
        Write-Host "❌ ERROR: $($_.Exception.Message)" -ForegroundColor Red
    }
    
} else {
    Write-Host ""
    Write-Host "⏸️  Import cancelled" -ForegroundColor Yellow
    Write-Host "   SQL file saved at: $OUTPUT_SQL_FILE" -ForegroundColor White
    Write-Host "   You can review and execute it manually" -ForegroundColor White
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
