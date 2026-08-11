# Setup Development Database
# Run this script to create the tminet database and user

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Development Database Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# SQL commands to run
$setupSQL = @"
CREATE DATABASE IF NOT EXISTS tminet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP USER IF EXISTS 'tminet'@'localhost';
CREATE USER 'tminet'@'localhost' IDENTIFIED BY '0a0b0c0D.';
GRANT ALL PRIVILEGES ON tminet.* TO 'tminet'@'localhost';
FLUSH PRIVILEGES;
SELECT 'Database and user created successfully!' AS Status;
"@

Write-Host "This will create:" -ForegroundColor Yellow
Write-Host "  - Database: tminet" -ForegroundColor White
Write-Host "  - User: tminet" -ForegroundColor White
Write-Host "  - Password: 0a0b0c0D." -ForegroundColor White
Write-Host ""
Write-Host "Please enter MySQL root password when prompted..." -ForegroundColor Yellow
Write-Host ""

# Execute SQL
$setupSQL | mysql -u root -p

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✓ Setup completed successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Test connection:" -ForegroundColor Yellow
    Write-Host "  mysql -u tminet -p0a0b0c0D. tminet" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "✗ Setup failed!" -ForegroundColor Red
    Write-Host "Please check MySQL is running and you entered correct root password." -ForegroundColor Red
    Write-Host ""
}
