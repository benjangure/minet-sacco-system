# Minet SACCO Database Setup Script
# This script helps set up the MySQL database for local development

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Minet SACCO - Database Setup" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Function to test MySQL connection
function Test-MySQLConnection {
    param (
        [string]$Password
    )
    
    $result = mysql -u root -p"$Password" -e "SELECT 1;" 2>&1
    return $LASTEXITCODE -eq 0
}

# Check if MySQL service is running
Write-Host "Checking MySQL service..." -ForegroundColor Yellow
$mysqlService = Get-Service -Name MySQL80 -ErrorAction SilentlyContinue

if ($null -eq $mysqlService) {
    Write-Host "ERROR: MySQL80 service not found!" -ForegroundColor Red
    Write-Host "Please install MySQL 8.0 or ensure the service name is correct." -ForegroundColor Red
    exit 1
}

if ($mysqlService.Status -ne 'Running') {
    Write-Host "MySQL service is not running. Starting it..." -ForegroundColor Yellow
    Start-Service -Name MySQL80
    Start-Sleep -Seconds 3
}

Write-Host "✓ MySQL service is running" -ForegroundColor Green
Write-Host ""

# Prompt for MySQL root password
Write-Host "Please enter your MySQL root password:" -ForegroundColor Yellow
Write-Host "(If you don't know it, press Enter and follow the reset instructions)" -ForegroundColor Gray
$password = Read-Host -AsSecureString "Password"
$passwordPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password))

if ([string]::IsNullOrEmpty($passwordPlain)) {
    Write-Host ""
    Write-Host "No password entered. You need to reset your MySQL password." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To reset MySQL password:" -ForegroundColor Cyan
    Write-Host "1. Run PowerShell as Administrator" -ForegroundColor White
    Write-Host "2. Stop MySQL: net stop MySQL80" -ForegroundColor White
    Write-Host "3. Start safe mode: mysqld --skip-grant-tables --skip-networking" -ForegroundColor White
    Write-Host "4. Open NEW PowerShell and run:" -ForegroundColor White
    Write-Host "   mysql -u root" -ForegroundColor Gray
    Write-Host "   FLUSH PRIVILEGES;" -ForegroundColor Gray
    Write-Host "   ALTER USER 'root'@'localhost' IDENTIFIED BY 'admin123';" -ForegroundColor Gray
    Write-Host "   EXIT;" -ForegroundColor Gray
    Write-Host "5. Close safe mode window and run: net start MySQL80" -ForegroundColor White
    Write-Host "6. Run this script again with password: admin123" -ForegroundColor White
    Write-Host ""
    exit 1
}

# Test MySQL connection
Write-Host "Testing MySQL connection..." -ForegroundColor Yellow
if (-not (Test-MySQLConnection -Password $passwordPlain)) {
    Write-Host "✗ Failed to connect to MySQL with provided password" -ForegroundColor Red
    Write-Host "Please check your password and try again." -ForegroundColor Red
    exit 1
}

Write-Host "✓ MySQL connection successful" -ForegroundColor Green
Write-Host ""

# Create database
Write-Host "Creating sacco_db database..." -ForegroundColor Yellow
$createDB = mysql -u root -p"$passwordPlain" -e "CREATE DATABASE IF NOT EXISTS sacco_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Database created successfully" -ForegroundColor Green
} else {
    Write-Host "✗ Failed to create database" -ForegroundColor Red
    Write-Host $createDB
    exit 1
}

# Verify database exists
Write-Host "Verifying database..." -ForegroundColor Yellow
$dbExists = mysql -u root -p"$passwordPlain" -e "SHOW DATABASES LIKE 'sacco_db';" 2>&1

if ($dbExists -match "sacco_db") {
    Write-Host "✓ Database verified" -ForegroundColor Green
} else {
    Write-Host "✗ Database verification failed" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Database Setup Complete!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Update application.properties
Write-Host "Updating application.properties..." -ForegroundColor Yellow
$propsFile = "src\main\resources\application.properties"

if (Test-Path $propsFile) {
    $content = Get-Content $propsFile -Raw
    $pattern = 'spring\.datasource\.password=.*'
    $replacement = "spring.datasource.password=$passwordPlain"
    $content = $content -replace $pattern, $replacement
    Set-Content -Path $propsFile -Value $content -NoNewline
    Write-Host "✓ application.properties updated with your password" -ForegroundColor Green
} else {
    Write-Host "⚠ Could not find application.properties" -ForegroundColor Yellow
    Write-Host "Please manually set: spring.datasource.password=$passwordPlain" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "1. Run the backend: .\mvnw.cmd spring-boot:run" -ForegroundColor White
Write-Host "2. Backend will run Flyway migrations automatically" -ForegroundColor White
Write-Host "3. Access API at: http://localhost:8080" -ForegroundColor White
Write-Host "4. Access Swagger UI at: http://localhost:8080/swagger-ui/index.html" -ForegroundColor White
Write-Host ""
Write-Host "Database Details:" -ForegroundColor Cyan
Write-Host "  Host: localhost:3306" -ForegroundColor White
Write-Host "  Database: sacco_db" -ForegroundColor White
Write-Host "  Username: root" -ForegroundColor White
Write-Host "  Password: $passwordPlain" -ForegroundColor White
Write-Host ""
