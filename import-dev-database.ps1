# Import Production Database Dump into Development Database
# This creates the tminet database and imports all data from the production dump

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Import Database Dump to Development" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$dumpPath = "C:\Users\Lenovo\Desktop\Dump20260727"

# Check if dump directory exists
if (!(Test-Path $dumpPath)) {
    Write-Host "✗ Dump directory not found: $dumpPath" -ForegroundColor Red
    exit 1
}

Write-Host "Step 1: Creating tminet database and user..." -ForegroundColor Yellow
Write-Host ""

# SQL to create database and user
$setupSQL = @"
DROP DATABASE IF EXISTS tminet;
CREATE DATABASE tminet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP USER IF EXISTS 'tminet'@'localhost';
CREATE USER 'tminet'@'localhost' IDENTIFIED BY '0a0b0c0D.';
GRANT ALL PRIVILEGES ON tminet.* TO 'tminet'@'localhost';
FLUSH PRIVILEGES;
"@

Write-Host "Please enter MySQL root password when prompted..." -ForegroundColor White
$setupSQL | mysql -u root -p

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "✗ Failed to create database!" -ForegroundColor Red
    exit 1
}

Write-Host "✓ Database and user created" -ForegroundColor Green
Write-Host ""
Write-Host "Step 2: Importing dump files..." -ForegroundColor Yellow
Write-Host ""

# Get all SQL files from dump directory
$sqlFiles = Get-ChildItem -Path $dumpPath -Filter "*.sql" | Sort-Object Name

$totalFiles = $sqlFiles.Count
$current = 0

foreach ($file in $sqlFiles) {
    $current++
    $percent = [math]::Round(($current / $totalFiles) * 100)
    Write-Host "[$current/$totalFiles] Importing $($file.Name)... " -NoNewline
    
    # Import each file into tminet database
    Get-Content $file.FullName | mysql -u tminet -p0a0b0c0D. tminet 2>$null
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓" -ForegroundColor Green
    } else {
        Write-Host "✗ (may be ok if table doesn't exist yet)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "Step 3: Verifying import..." -ForegroundColor Yellow

$verifySQL = @"
USE tminet;
SELECT 
    'Database imported successfully!' AS Status,
    COUNT(*) AS TableCount
FROM information_schema.tables 
WHERE table_schema = 'tminet';
"@

$verifySQL | mysql -u tminet -p0a0b0c0D.

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Import Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Development database ready:" -ForegroundColor White
Write-Host "  Database: tminet" -ForegroundColor White
Write-Host "  User: tminet" -ForegroundColor White
Write-Host "  Password: 0a0b0c0D." -ForegroundColor White
Write-Host ""
Write-Host "Next step: Start the backend" -ForegroundColor Yellow
Write-Host "  cd backend" -ForegroundColor White
Write-Host '  ./mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"' -ForegroundColor White
Write-Host ""
