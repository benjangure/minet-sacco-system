# ========================================
# SCRIPT 4: IMPORT FROM EXCEL
# ========================================
# This script imports correct financial data from Excel file
# 
# PREREQUISITES:
#   1. ✅ Database backup completed
#   2. ✅ Financial data deleted (script 3)
#   3. ✅ Excel file prepared
#   4. ✅ ImportExcel module installed
# ========================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  MINET SACCO - IMPORT FROM EXCEL" -ForegroundColor Cyan
Write-Host "  Correct Financial Data Import" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$EXCEL_FILE = ".\financial_data_correct.xlsx"  # UPDATE THIS PATH
$MYSQL_USER = "root"
$MYSQL_PASSWORD = ""  # Change if you have a password
$DATABASE_NAME = "sacco_db"
$OUTPUT_SQL_FILE = ".\import_data_from_excel.sql"

# ========================================
# Check prerequisites
# ========================================

Write-Host "🔍 Checking prerequisites..." -ForegroundColor Yellow
Write-Host ""

# Check if Excel file exists
if (!(Test-Path $EXCEL_FILE)) {
    Write-Host "❌ ERROR: Excel file not found!" -ForegroundColor Red
    Write-Host "   Expected: $EXCEL_FILE" -ForegroundColor Red
    Write-Host "" -ForegroundColor Red
    Write-Host "Please update the EXCEL_FILE variable in this script" -ForegroundColor Yellow
    Write-Host "to point to your correct financial data Excel file." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Excel file found: $EXCEL_FILE" -ForegroundColor Green

# Check for ImportExcel module
Write-Host "🔍 Checking for ImportExcel PowerShell module..." -ForegroundColor Gray
$module = Get-Module -ListAvailable -Name ImportExcel

if ($null -eq $module) {
    Write-Host "⚠️  ImportExcel module not found. Installing..." -ForegroundColor Yellow
    try {
        Install-Module -Name ImportExcel -Scope CurrentUser -Force
        Write-Host "✅ ImportExcel module installed" -ForegroundColor Green
    } catch {
        Write-Host "❌ ERROR: Could not install ImportExcel module" -ForegroundColor Red
        Write-Host "   Please install manually: Install-Module ImportExcel" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✅ ImportExcel module available" -ForegroundColor Green
}

Write-Host ""

# ========================================
# Display Excel file info
# ========================================

Write-Host "📊 Reading Excel file..." -ForegroundColor Yellow
Write-Host ""

try {
    # Get worksheet names
    Import-Module ImportExcel
    $excelPackage = Open-ExcelPackage -Path $EXCEL_FILE
    $worksheetNames = $excelPackage.Workbook.Worksheets.Name
    Close-ExcelPackage $excelPackage

    Write-Host "📄 Worksheets found in Excel file:" -ForegroundColor Cyan
    $worksheetNames | ForEach-Object {
        Write-Host "   • $_" -ForegroundColor White
    }
    Write-Host ""

} catch {
    Write-Host "❌ ERROR: Could not read Excel file!" -ForegroundColor Red
    Write-Host "   $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# ========================================
# Prompt user for worksheet mapping
# ========================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  WORKSHEET MAPPING" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Please specify which worksheets contain your data:" -ForegroundColor Yellow
Write-Host ""

$loansSheet = Read-Host "Worksheet name for LOANS data (or press Enter to skip)"
$repaymentsSheet = Read-Host "Worksheet name for REPAYMENTS data (or press Enter to skip)"
$transactionsSheet = Read-Host "Worksheet name for TRANSACTIONS data (or press Enter to skip)"

Write-Host ""

# ========================================
# Generate SQL from Excel data
# ========================================

Write-Host "🔄 Processing Excel data and generating SQL..." -ForegroundColor Yellow
Write-Host ""

$sqlCommands = @()
$sqlCommands += "-- ========================================"
$sqlCommands += "-- GENERATED SQL FROM EXCEL"
$sqlCommands += "-- Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
$sqlCommands += "-- Source: $EXCEL_FILE"
$sqlCommands += "-- ========================================"
$sqlCommands += ""
$sqlCommands += "USE $DATABASE_NAME;"
$sqlCommands += ""
$sqlCommands += "SET FOREIGN_KEY_CHECKS = 0;"
$sqlCommands += ""

$importedRecords = @{
    loans = 0
    repayments = 0
    transactions = 0
}

# ========================================
# Process LOANS data
# ========================================

if ($loansSheet -and $loansSheet.Trim() -ne "") {
    Write-Host "📋 Processing LOANS worksheet: $loansSheet" -ForegroundColor Cyan
    
    try {
        $loansData = Import-Excel -Path $EXCEL_FILE -WorksheetName $loansSheet
        
        Write-Host "   Found $($loansData.Count) loan records" -ForegroundColor Gray
        
        if ($loansData.Count -gt 0) {
            # Display column names
            Write-Host "   Columns detected:" -ForegroundColor Gray
            $loansData[0].PSObject.Properties.Name | ForEach-Object {
                Write-Host "      • $_" -ForegroundColor DarkGray
            }
            
            $sqlCommands += "-- ========================================"
            $sqlCommands += "-- LOANS DATA"
            $sqlCommands += "-- ========================================"
            $sqlCommands += ""
            
            foreach ($loan in $loansData) {
                # Map Excel columns to database columns
                # ADJUST THESE MAPPINGS BASED ON YOUR EXCEL STRUCTURE
                
                $id = if ($loan.id) { $loan.id } else { "NULL" }
                $memberId = if ($loan.member_id) { $loan.member_id } else { "NULL" }
                $loanProductId = if ($loan.loan_product_id) { $loan.loan_product_id } else { 1 }
                $amount = if ($loan.amount -or $loan.principal) { 
                    if ($loan.amount) { $loan.amount } else { $loan.principal }
                } else { 0 }
                $interestRate = if ($loan.interest_rate) { $loan.interest_rate } else { 0 }
                $termMonths = if ($loan.term_months -or $loan.term) { 
                    if ($loan.term_months) { $loan.term_months } else { $loan.term }
                } else { 12 }
                $monthlyRepayment = if ($loan.monthly_repayment) { $loan.monthly_repayment } else { 0 }
                $totalInterest = if ($loan.total_interest) { $loan.total_interest } else { 0 }
                $totalRepayable = if ($loan.total_repayable) { $loan.total_repayable } else { 0 }
                $outstandingBalance = if ($loan.outstanding_balance) { $loan.outstanding_balance } else { $amount }
                $interestCollected = if ($loan.interest_collected) { $loan.interest_collected } else { 0 }
                $principalRepaid = if ($loan.principal_repaid) { $loan.principal_repaid } else { 0 }
                $loanNumber = if ($loan.loan_number) { "'$($loan.loan_number)'" } else { "NULL" }
                $status = if ($loan.status) { "'$($loan.status)'" } else { "'DISBURSED'" }
                
                # Generate UPDATE statement (safer than INSERT for existing loans)
                if ($id -ne "NULL") {
                    $sql = @"
UPDATE loans
SET 
    amount = $amount,
    interest_rate = $interestRate,
    term_months = $termMonths,
    monthly_repayment = $monthlyRepayment,
    total_interest = $totalInterest,
    total_repayable = $totalRepayable,
    outstanding_balance = $outstandingBalance,
    interest_collected = $interestCollected,
    principal_repaid = $principalRepaid,
    original_principal = $amount
WHERE id = $id;
"@
                    $sqlCommands += $sql
                    $importedRecords.loans++
                }
            }
            
            $sqlCommands += ""
            Write-Host "   ✅ Generated SQL for $($importedRecords.loans) loans" -ForegroundColor Green
        }
        
    } catch {
        Write-Host "   ⚠️  WARNING: Could not process loans worksheet" -ForegroundColor Yellow
        Write-Host "      $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

# ========================================
# Process REPAYMENTS data
# ========================================

if ($repaymentsSheet -and $repaymentsSheet.Trim() -ne "") {
    Write-Host "📋 Processing REPAYMENTS worksheet: $repaymentsSheet" -ForegroundColor Cyan
    
    try {
        $repaymentsData = Import-Excel -Path $EXCEL_FILE -WorksheetName $repaymentsSheet
        
        Write-Host "   Found $($repaymentsData.Count) repayment records" -ForegroundColor Gray
        
        if ($repaymentsData.Count -gt 0) {
            $sqlCommands += "-- ========================================"
            $sqlCommands += "-- REPAYMENTS DATA"
            $sqlCommands += "-- ========================================"
            $sqlCommands += ""
            
            foreach ($repayment in $repaymentsData) {
                $loanId = if ($repayment.loan_id) { $repayment.loan_id } else { "NULL" }
                $amount = if ($repayment.amount) { $repayment.amount } else { 0 }
                $principalAmount = if ($repayment.principal_amount) { $repayment.principal_amount } else { 0 }
                $interestAmount = if ($repayment.interest_amount) { $repayment.interest_amount } else { 0 }
                $repaymentDate = if ($repayment.repayment_date) { 
                    "'$($repayment.repayment_date)'" 
                } else { "NOW()" }
                $paymentMethod = if ($repayment.payment_method) { 
                    "'$($repayment.payment_method)'" 
                } else { "'CASH'" }
                
                if ($loanId -ne "NULL") {
                    $sql = @"
INSERT INTO loan_repayments 
(loan_id, amount, principal_amount, interest_amount, repayment_date, payment_method)
VALUES 
($loanId, $amount, $principalAmount, $interestAmount, $repaymentDate, $paymentMethod);
"@
                    $sqlCommands += $sql
                    $importedRecords.repayments++
                }
            }
            
            $sqlCommands += ""
            Write-Host "   ✅ Generated SQL for $($importedRecords.repayments) repayments" -ForegroundColor Green
        }
        
    } catch {
        Write-Host "   ⚠️  WARNING: Could not process repayments worksheet" -ForegroundColor Yellow
        Write-Host "      $($_.Exception.Message)" -ForegroundColor Yellow
    }
    
    Write-Host ""
}

# ========================================
# Finalize SQL
# ========================================

$sqlCommands += ""
$sqlCommands += "SET FOREIGN_KEY_CHECKS = 1;"
$sqlCommands += ""
$sqlCommands += "-- ========================================"
$sqlCommands += "-- IMPORT COMPLETE"
$sqlCommands += "-- ========================================"
$sqlCommands += "SELECT 'Import SQL generated successfully' AS Status;"

# ========================================
# Save SQL to file
# ========================================

Write-Host "💾 Saving generated SQL to file..." -ForegroundColor Yellow
$sqlCommands | Out-File -FilePath $OUTPUT_SQL_FILE -Encoding UTF8
Write-Host "   ✅ SQL saved to: $OUTPUT_SQL_FILE" -ForegroundColor Green
Write-Host ""

# ========================================
# Display summary
# ========================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  IMPORT SUMMARY" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📊 Records to be imported:" -ForegroundColor Yellow
Write-Host "   • Loans: $($importedRecords.loans)" -ForegroundColor White
Write-Host "   • Repayments: $($importedRecords.repayments)" -ForegroundColor White
Write-Host "   • Transactions: $($importedRecords.transactions)" -ForegroundColor White
Write-Host ""
Write-Host "📄 Generated SQL file: $OUTPUT_SQL_FILE" -ForegroundColor White
Write-Host ""

# ========================================
# Ask to execute SQL
# ========================================

Write-Host "================================================" -ForegroundColor Yellow
Write-Host "  READY TO IMPORT" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "⚠️  Please review the generated SQL file before importing!" -ForegroundColor Yellow
Write-Host ""
$confirm = Read-Host "Do you want to execute the SQL now? (yes/no)"

if ($confirm -eq "yes") {
    Write-Host ""
    Write-Host "🔄 Executing SQL import..." -ForegroundColor Yellow
    
    try {
        if ($MYSQL_PASSWORD -eq "") {
            Get-Content $OUTPUT_SQL_FILE | mysql -u $MYSQL_USER $DATABASE_NAME
        } else {
            Get-Content $OUTPUT_SQL_FILE | mysql -u $MYSQL_USER -p$MYSQL_PASSWORD $DATABASE_NAME
        }
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "✅ SQL IMPORT SUCCESSFUL!" -ForegroundColor Green
            Write-Host ""
            Write-Host "Next step: Run verification script" -ForegroundColor Yellow
            Write-Host "   5_VERIFY_IMPORTED_DATA.sql" -ForegroundColor White
        } else {
            Write-Host ""
            Write-Host "❌ SQL IMPORT FAILED!" -ForegroundColor Red
            Write-Host "   Check error messages above" -ForegroundColor Red
            Write-Host "   SQL file saved at: $OUTPUT_SQL_FILE" -ForegroundColor Red
        }
        
    } catch {
        Write-Host "❌ ERROR: $($_.Exception.Message)" -ForegroundColor Red
    }
    
} else {
    Write-Host ""
    Write-Host "⏸️  Import skipped" -ForegroundColor Yellow
    Write-Host "   You can manually execute the SQL file:" -ForegroundColor White
    Write-Host "   mysql -u $MYSQL_USER $DATABASE_NAME < $OUTPUT_SQL_FILE" -ForegroundColor Gray
    Write-Host ""
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "SCRIPT COMPLETE" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
