# ============================================
# Check Backend Startup Issues
# ============================================

Write-Host "=== Backend Startup Diagnostics ===" -ForegroundColor Cyan
Write-Host ""

# Check if JAR exists
$jarPath = "C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar"
Write-Host "1. Checking JAR file..." -ForegroundColor Yellow
if (Test-Path $jarPath) {
    $jar = Get-Item $jarPath
    Write-Host "   ✓ JAR exists" -ForegroundColor Green
    Write-Host "   Size: $([math]::Round($jar.Length/1MB,2)) MB" -ForegroundColor Gray
    Write-Host "   Built: $($jar.LastWriteTime)" -ForegroundColor Gray
} else {
    Write-Host "   ✗ JAR not found!" -ForegroundColor Red
}

Write-Host ""
Write-Host "2. Checking if backend is running..." -ForegroundColor Yellow
$process = Get-Process java -ErrorAction SilentlyContinue | Where-Object {$_.CommandLine -like "*minet-sacco-backend*"}
if ($process) {
    Write-Host "   ✓ Backend process found (PID: $($process.Id))" -ForegroundColor Green
} else {
    Write-Host "   ✗ Backend not running" -ForegroundColor Red
}

Write-Host ""
Write-Host "3. Checking if port 9090 is in use..." -ForegroundColor Yellow
$port = netstat -ano | findstr ":9090"
if ($port) {
    Write-Host "   ✓ Port 9090 is in use:" -ForegroundColor Green
    Write-Host "   $port" -ForegroundColor Gray
} else {
    Write-Host "   ✗ Port 9090 is not in use" -ForegroundColor Red
}

Write-Host ""
Write-Host "4. To start backend locally:" -ForegroundColor Cyan
Write-Host "   cd backend" -ForegroundColor Gray
Write-Host "   java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar" -ForegroundColor Gray

Write-Host ""
Write-Host "5. To check logs if it's failing:" -ForegroundColor Cyan
Write-Host "   Check the console output for error messages" -ForegroundColor Gray
Write-Host "   Common issues:" -ForegroundColor Gray
Write-Host "   - Database connection failed" -ForegroundColor Gray
Write-Host "   - Port 9090 already in use" -ForegroundColor Gray
Write-Host "   - Missing environment variables" -ForegroundColor Gray

Write-Host ""
