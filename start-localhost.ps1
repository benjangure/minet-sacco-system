# Start Minet SACCO System on Localhost
# This script starts the backend in development mode

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Minet SACCO System - Local Development" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Check if MySQL is running
Write-Host "Checking MySQL service..." -ForegroundColor Yellow
$mysqlService = Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue

if ($null -eq $mysqlService) {
    Write-Host "✗ MySQL80 service not found!" -ForegroundColor Red
    Write-Host "  Please install MySQL 8.0 or update the service name in this script." -ForegroundColor Red
    exit 1
}

if ($mysqlService.Status -ne "Running") {
    Write-Host "✗ MySQL is not running. Starting MySQL..." -ForegroundColor Yellow
    try {
        Start-Service MySQL80
        Write-Host "✓ MySQL started successfully!" -ForegroundColor Green
    } catch {
        Write-Host "✗ Failed to start MySQL: $_" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "✓ MySQL is running" -ForegroundColor Green
}

# Check if tminet database exists
Write-Host ""
Write-Host "Checking development database..." -ForegroundColor Yellow
$dbCheck = mysql -u tminet -p0a0b0c0D. -e "SELECT 1 FROM DUAL;" 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Database 'tminet' or user 'tminet' not found!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please run the database setup:" -ForegroundColor Yellow
    Write-Host "  mysql -u root -p < backend\setup-dev-database.sql" -ForegroundColor White
    Write-Host ""
    Write-Host "Or create manually:" -ForegroundColor Yellow
    Write-Host "  CREATE DATABASE tminet;" -ForegroundColor White
    Write-Host "  CREATE USER 'tminet'@'localhost' IDENTIFIED BY '0a0b0c0D.';" -ForegroundColor White
    Write-Host "  GRANT ALL PRIVILEGES ON tminet.* TO 'tminet'@'localhost';" -ForegroundColor White
    Write-Host "  FLUSH PRIVILEGES;" -ForegroundColor White
    Write-Host ""
    exit 1
} else {
    Write-Host "✓ Database 'tminet' is accessible" -ForegroundColor Green
}

# Start backend
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "Starting Backend (Development Profile)" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Profile:  dev" -ForegroundColor White
Write-Host "Database: tminet" -ForegroundColor White
Write-Host "Port:     9090" -ForegroundColor White
Write-Host "API URL:  http://localhost:9090/api" -ForegroundColor White
Write-Host ""
Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
Write-Host ""

Set-Location -Path "$PSScriptRoot\backend"
& ".\mvnw.cmd" spring-boot:run -Dspring-boot.run.profiles=dev
