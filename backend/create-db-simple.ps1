# Simple Database Setup
Write-Host "=== MySQL Database Setup ===" -ForegroundColor Cyan
Write-Host ""

# Prompt for password
$securePassword = Read-Host "Enter your MySQL root password (or press Enter if no password)" -AsSecureString
$password = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword))

# Create database
Write-Host ""
Write-Host "Creating database..." -ForegroundColor Yellow

if ([string]::IsNullOrEmpty($password)) {
    mysql -u root -e "CREATE DATABASE IF NOT EXISTS sacco_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1
    $exitCode = $LASTEXITCODE
} else {
    $env:MYSQL_PWD = $password
    mysql -u root -e "CREATE DATABASE IF NOT EXISTS sacco_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>&1
    $exitCode = $LASTEXITCODE
    Remove-Item Env:\MYSQL_PWD
}

if ($exitCode -eq 0) {
    Write-Host "✓ Database created!" -ForegroundColor Green
    
    # Update application.properties
    $propsFile = "src\main\resources\application.properties"
    $content = Get-Content $propsFile -Raw
    $content = $content -replace 'spring.datasource.password=YOUR_MYSQL_PASSWORD_HERE', "spring.datasource.password=$password"
    Set-Content -Path $propsFile -Value $content -NoNewline
    
    Write-Host "✓ Configuration updated!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Database 'sacco_db' is ready!" -ForegroundColor Green
    Write-Host "You can now start the backend." -ForegroundColor Green
} else {
    Write-Host "✗ Failed to create database" -ForegroundColor Red
    Write-Host "Please check your password and try again" -ForegroundColor Yellow
}
