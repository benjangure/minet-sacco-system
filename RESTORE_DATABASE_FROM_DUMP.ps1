# ========================================
# RESTORE DATABASE FROM DUMP
# ========================================
# This script will:
# 1. Drop the existing minetsacco database
# 2. Recreate it
# 3. Import all tables from the dump
# ========================================

$ErrorActionPreference = "Stop"

$MYSQL_USER = "minetsacco"
$MYSQL_PASSWORD = "0a0b0c0D."
$DATABASE_NAME = "minetsacco"
$DUMP_DIR = "C:\Users\Lenovo\Desktop\Dump20260810"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DATABASE RESTORE FROM DUMP" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "WARNING: This will DELETE ALL DATA in the minetsacco database!" -ForegroundColor Red
Write-Host "Dump source: $DUMP_DIR" -ForegroundColor Yellow
Write-Host ""

# Create backup first
Write-Host "Step 1: Creating backup of current database..." -ForegroundColor Yellow
$backupDir = ".\database_backups"
if (!(Test-Path $backupDir)) { New-Item -ItemType Directory -Path $backupDir | Out-Null }
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = "$backupDir\minetsacco_backup_before_restore_$timestamp.sql"

mysqldump -u $MYSQL_USER -p"$MYSQL_PASSWORD" $DATABASE_NAME > $backupFile 2>&1 | Out-Null

if (Test-Path $backupFile) {
    $size = (Get-Item $backupFile).Length / 1MB
    Write-Host "  + Backup created: $backupFile ($([math]::Round($size, 2)) MB)" -ForegroundColor Green
} else {
    Write-Host "  x Backup failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Step 2: Dropping existing database..." -ForegroundColor Yellow

mysql -u $MYSQL_USER -p"$MYSQL_PASSWORD" -e "DROP DATABASE IF EXISTS $DATABASE_NAME;" 2>&1 | Out-Null
Write-Host "  + Database dropped" -ForegroundColor Green

Write-Host ""
Write-Host "Step 3: Creating fresh database..." -ForegroundColor Yellow

mysql -u $MYSQL_USER -p"$MYSQL_PASSWORD" -e "CREATE DATABASE $DATABASE_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1 | Out-Null
Write-Host "  + Database created" -ForegroundColor Green

Write-Host ""
Write-Host "Step 4: Importing dump files..." -ForegroundColor Yellow

# Get all SQL files from dump directory
$sqlFiles = Get-ChildItem -Path $DUMP_DIR -Filter "*.sql" | Sort-Object Name

$totalFiles = $sqlFiles.Count
$currentFile = 0

foreach ($file in $sqlFiles) {
    $currentFile++
    $percent = [math]::Round(($currentFile / $totalFiles) * 100)
    Write-Host "  [$percent%] Importing $($file.Name)..." -ForegroundColor Gray
    
    try {
        Get-Content $file.FullName | mysql -u $MYSQL_USER -p"$MYSQL_PASSWORD" $DATABASE_NAME 2>&1 | Out-Null
    } catch {
        Write-Host "    x Error importing $($file.Name): $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "Step 5: Verifying restore..." -ForegroundColor Yellow

$verification = mysql -u $MYSQL_USER -p"$MYSQL_PASSWORD" $DATABASE_NAME -e "
SELECT 'Members' as TableName, COUNT(*) as RowCount FROM members
UNION ALL
SELECT 'Loans', COUNT(*) FROM loans
UNION ALL
SELECT 'Accounts', COUNT(*) FROM accounts
UNION ALL
SELECT 'Transactions', COUNT(*) FROM transactions
UNION ALL
SELECT 'Loan Repayments', COUNT(*) FROM loan_repayments;
" 2>&1 | Where-Object { $_ -notmatch "Warning" }

Write-Host ""
Write-Host $verification
Write-Host ""

Write-Host "========================================" -ForegroundColor Green
Write-Host "  DATABASE RESTORE COMPLETE!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Stop any running backend process" -ForegroundColor White
Write-Host "2. Restart the backend to load new data" -ForegroundColor White
Write-Host "3. Clear browser cache (Ctrl+Shift+Delete)" -ForegroundColor White
Write-Host ""
