# ========================================
# SCRIPT 1: BACKUP DATABASE
# ========================================
# Creates a full backup before financial data deletion
# 
# CRITICAL: Do not proceed to next step without successful backup!
# ========================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  MINET SACCO - DATABASE BACKUP" -ForegroundColor Cyan
Write-Host "  Before Financial Data Reset" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$MYSQL_USER = "root"
$MYSQL_PASSWORD = ""  # Change if you have a password
$DATABASE_NAME = "sacco_db"
$TIMESTAMP = Get-Date -Format "yyyyMMdd_HHmmss"
$BACKUP_FILENAME = "backup_before_financial_reset_$TIMESTAMP.sql"
$BACKUP_DIR = ".\database_backups"
$BACKUP_PATH = Join-Path $BACKUP_DIR $BACKUP_FILENAME

Write-Host "⏳ Starting database backup..." -ForegroundColor Yellow
Write-Host ""

# Create backup directory if it doesn't exist
if (!(Test-Path $BACKUP_DIR)) {
    Write-Host "📁 Creating backup directory: $BACKUP_DIR" -ForegroundColor Gray
    New-Item -ItemType Directory -Path $BACKUP_DIR | Out-Null
}

# Check if MySQL is accessible
Write-Host "🔍 Checking MySQL connection..." -ForegroundColor Gray
$mysqlCheck = if ($MYSQL_PASSWORD -eq "") {
    & mysql -u $MYSQL_USER -e "SELECT 1;" 2>&1
} else {
    & mysql -u $MYSQL_USER -p$MYSQL_PASSWORD -e "SELECT 1;" 2>&1
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ ERROR: Cannot connect to MySQL!" -ForegroundColor Red
    Write-Host "   Please ensure MySQL is running and credentials are correct." -ForegroundColor Red
    exit 1
}

Write-Host "✅ MySQL connection successful" -ForegroundColor Green
Write-Host ""

# Perform backup
Write-Host "💾 Creating backup: $BACKUP_FILENAME" -ForegroundColor Yellow
Write-Host "   Database: $DATABASE_NAME" -ForegroundColor Gray
Write-Host "   Location: $BACKUP_PATH" -ForegroundColor Gray
Write-Host ""

try {
    if ($MYSQL_PASSWORD -eq "") {
        & mysqldump -u $MYSQL_USER $DATABASE_NAME > $BACKUP_PATH
    } else {
        & mysqldump -u $MYSQL_USER -p$MYSQL_PASSWORD $DATABASE_NAME > $BACKUP_PATH
    }

    if ($LASTEXITCODE -ne 0) {
        throw "mysqldump failed with exit code: $LASTEXITCODE"
    }

    # Verify backup file exists and has content
    if (!(Test-Path $BACKUP_PATH)) {
        throw "Backup file was not created!"
    }

    $backupSize = (Get-Item $BACKUP_PATH).Length
    if ($backupSize -eq 0) {
        throw "Backup file is empty!"
    }

    # Display backup info
    $backupSizeMB = [math]::Round($backupSize / 1MB, 2)
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Green
    Write-Host "✅ BACKUP SUCCESSFUL!" -ForegroundColor Green
    Write-Host "================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "📊 Backup Details:" -ForegroundColor Cyan
    Write-Host "   File: $BACKUP_FILENAME" -ForegroundColor White
    Write-Host "   Size: $backupSizeMB MB" -ForegroundColor White
    Write-Host "   Path: $BACKUP_PATH" -ForegroundColor White
    Write-Host "   Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor White
    Write-Host ""

    # Verify backup content
    Write-Host "🔍 Verifying backup content..." -ForegroundColor Yellow
    $backupContent = Get-Content $BACKUP_PATH -TotalCount 50
    $hasCreateTable = $backupContent | Select-String "CREATE TABLE"
    $hasInsert = $backupContent | Select-String "INSERT INTO"

    if ($hasCreateTable) {
        Write-Host "   ✅ Backup contains table definitions" -ForegroundColor Green
    }
    if ($hasInsert) {
        Write-Host "   ✅ Backup contains data" -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host "  NEXT STEPS" -ForegroundColor Cyan
    Write-Host "================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. ✅ Backup completed successfully" -ForegroundColor Green
    Write-Host "2. ⏭️  Review the analysis document:" -ForegroundColor Yellow
    Write-Host "      FINANCIAL_DATA_RESET_ANALYSIS.md" -ForegroundColor White
    Write-Host "3. ⏭️  Run verification script:" -ForegroundColor Yellow
    Write-Host "      2_VERIFY_CURRENT_DATA.sql" -ForegroundColor White
    Write-Host "4. ⏭️  Provide your Excel file with correct data" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "⚠️  KEEP THIS BACKUP SAFE!" -ForegroundColor Red
    Write-Host "   You will need it to rollback if anything goes wrong." -ForegroundColor Red
    Write-Host ""

    # Save backup info to log file
    $logFile = Join-Path $BACKUP_DIR "backup_log.txt"
    $logEntry = @"
========================================
Backup Created: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')
File: $BACKUP_FILENAME
Size: $backupSizeMB MB
Database: $DATABASE_NAME
Purpose: Financial Data Reset
========================================

"@
    Add-Content -Path $logFile -Value $logEntry

} catch {
    Write-Host ""
    Write-Host "================================================" -ForegroundColor Red
    Write-Host "❌ BACKUP FAILED!" -ForegroundColor Red
    Write-Host "================================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "⛔ DO NOT PROCEED WITH DATA DELETION!" -ForegroundColor Red
    Write-Host "   Fix the backup issue first." -ForegroundColor Red
    Write-Host ""
    exit 1
}
