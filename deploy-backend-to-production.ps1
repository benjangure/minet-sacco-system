# ============================================
# Deploy Backend JAR to Production Server
# ============================================
# This script copies the newly built backend JAR to production and restarts the service

Write-Host "=== Minet SACCO Backend Deployment ===" -ForegroundColor Cyan
Write-Host ""

# Configuration
$localJar = "C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar"
$productionServer = "10.39.60.15"
$productionPath = "\\$productionServer\c$\minetsacco-deploy\backend\target\"
$productionJar = "$productionPath\minet-sacco-backend-0.0.1-SNAPSHOT.jar"
$serviceName = "MinetSaccoBackend"

# Step 1: Verify local JAR exists
Write-Host "[1/6] Verifying local JAR..." -ForegroundColor Yellow
if (-not (Test-Path $localJar)) {
    Write-Host "ERROR: Local JAR not found at $localJar" -ForegroundColor Red
    Write-Host "Please rebuild the backend first: cd backend ; .\mvnw.cmd clean package -DskipTests" -ForegroundColor Yellow
    exit 1
}
$jarSize = (Get-Item $localJar).Length / 1MB
Write-Host "✓ Local JAR found ($([math]::Round($jarSize, 2)) MB)" -ForegroundColor Green

# Step 2: Test production server connectivity
Write-Host "[2/6] Testing production server connectivity..." -ForegroundColor Yellow
$pingResult = Test-Connection -ComputerName $productionServer -Count 1 -Quiet
if (-not $pingResult) {
    Write-Host "ERROR: Cannot reach production server $productionServer" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Production server reachable" -ForegroundColor Green

# Step 3: Stop the production service (via remote PowerShell)
Write-Host "[3/6] Stopping production service..." -ForegroundColor Yellow
Write-Host "NOTE: You may need to run this on the production server manually:" -ForegroundColor Yellow
Write-Host "  cd C:\Users\WakaeA\Downloads\nssm-2.24\win64" -ForegroundColor Gray
Write-Host "  .\nssm.exe stop MinetSaccoBackend" -ForegroundColor Gray
Write-Host ""
Write-Host "Press Enter after stopping the service on production server..." -ForegroundColor Cyan
Read-Host

# Step 4: Backup old JAR
Write-Host "[4/6] Backing up old JAR..." -ForegroundColor Yellow
if (Test-Path $productionJar) {
    $backupName = "minet-sacco-backend-0.0.1-SNAPSHOT.jar.backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    $backupPath = "$productionPath\$backupName"
    try {
        Copy-Item $productionJar $backupPath -Force
        Write-Host "✓ Old JAR backed up to: $backupName" -ForegroundColor Green
    } catch {
        Write-Host "Warning: Could not backup old JAR: $_" -ForegroundColor Yellow
    }
} else {
    Write-Host "✓ No existing JAR to backup" -ForegroundColor Green
}

# Step 5: Copy new JAR to production
Write-Host "[5/6] Copying new JAR to production..." -ForegroundColor Yellow
try {
    # Ensure target directory exists
    if (-not (Test-Path $productionPath)) {
        New-Item -ItemType Directory -Path $productionPath -Force | Out-Null
    }
    
    # Copy the JAR
    Copy-Item $localJar $productionJar -Force
    
    # Verify copy
    if (Test-Path $productionJar) {
        $prodJarSize = (Get-Item $productionJar).Length / 1MB
        Write-Host "✓ JAR copied successfully ($([math]::Round($prodJarSize, 2)) MB)" -ForegroundColor Green
    } else {
        Write-Host "ERROR: JAR copy verification failed" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "ERROR: Failed to copy JAR: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Alternative: Manually copy the JAR using these steps:" -ForegroundColor Yellow
    Write-Host "1. Copy from: $localJar" -ForegroundColor Gray
    Write-Host "2. Copy to: $productionServer at C:\minetsacco-deploy\backend\target\" -ForegroundColor Gray
    exit 1
}

# Step 6: Start the production service
Write-Host "[6/6] Starting production service..." -ForegroundColor Yellow
Write-Host "NOTE: Run this on the production server:" -ForegroundColor Yellow
Write-Host "  cd C:\Users\WakaeA\Downloads\nssm-2.24\win64" -ForegroundColor Gray
Write-Host "  .\nssm.exe start MinetSaccoBackend" -ForegroundColor Gray
Write-Host "  Start-Sleep -Seconds 20" -ForegroundColor Gray
Write-Host "  netstat -ano | findstr :9090" -ForegroundColor Gray
Write-Host ""
Write-Host "✓ Deployment complete!" -ForegroundColor Green

Write-Host ""
Write-Host "=== Next Steps ===" -ForegroundColor Cyan
Write-Host "1. On production server, start the service:" -ForegroundColor White
Write-Host "   cd C:\Users\WakaeA\Downloads\nssm-2.24\win64" -ForegroundColor Gray
Write-Host "   .\nssm.exe start MinetSaccoBackend" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Wait 20 seconds, then verify port 9090 is listening:" -ForegroundColor White
Write-Host "   netstat -ano | findstr :9090" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Test the backend:" -ForegroundColor White
Write-Host "   Invoke-WebRequest -Uri 'http://localhost:9090/actuator/health' -UseBasicParsing" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Check logs if needed:" -ForegroundColor White
Write-Host "   Get-Content C:\minetsacco-deploy\backend\logs\stdout.log -Tail 50" -ForegroundColor Gray
Write-Host ""
