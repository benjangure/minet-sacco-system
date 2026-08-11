# ============================================
# Copy New JAR to Production Server
# ============================================

Write-Host "=== Copy Backend JAR to Production ===" -ForegroundColor Cyan
Write-Host ""

$localJar = "C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar"
$productionServer = "10.39.60.15"
$productionPath = "\\$productionServer\c$\minetsacco-deploy\backend\target"
$productionJar = "$productionPath\minet-sacco-backend-0.0.1-SNAPSHOT.jar"

# Check local JAR
Write-Host "Checking local JAR..." -ForegroundColor Yellow
if (Test-Path $localJar) {
    $jarInfo = Get-Item $localJar
    Write-Host "✓ Local JAR found:" -ForegroundColor Green
    Write-Host "  Size: $([math]::Round($jarInfo.Length/1MB,2)) MB" -ForegroundColor Gray
    Write-Host "  Built: $($jarInfo.LastWriteTime)" -ForegroundColor Gray
} else {
    Write-Host "✗ Local JAR not found!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Attempting to copy to production server..." -ForegroundColor Yellow

# Try network share copy
try {
    # Test if production path is accessible
    if (-not (Test-Path $productionPath)) {
        Write-Host "Creating target directory..." -ForegroundColor Yellow
        New-Item -ItemType Directory -Path $productionPath -Force | Out-Null
    }
    
    # Copy the JAR
    Copy-Item $localJar $productionJar -Force
    
    # Verify
    if (Test-Path $productionJar) {
        $prodJarInfo = Get-Item $productionJar
        Write-Host "✓ JAR copied successfully!" -ForegroundColor Green
        Write-Host "  Size: $([math]::Round($prodJarInfo.Length/1MB,2)) MB" -ForegroundColor Gray
        Write-Host "  Copied: $($prodJarInfo.LastWriteTime)" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Network copy failed: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "=== Manual Copy Instructions ===" -ForegroundColor Cyan
    Write-Host "1. Copy from your local machine:" -ForegroundColor White
    Write-Host "   $localJar" -ForegroundColor Gray
    Write-Host ""
    Write-Host "2. Paste to production server at:" -ForegroundColor White
    Write-Host "   C:\minetsacco-deploy\backend\target\" -ForegroundColor Gray
    Write-Host ""
    Write-Host "3. Use Remote Desktop, USB drive, or shared folder" -ForegroundColor White
    exit 1
}

Write-Host ""
Write-Host "=== Next Steps (On Production Server) ===" -ForegroundColor Cyan
Write-Host "Run these commands on 10.39.60.15:" -ForegroundColor White
Write-Host ""
Write-Host "cd C:\Users\WakaeA\Downloads\nssm-2.24\win64" -ForegroundColor Gray
Write-Host ".\nssm.exe stop MinetSaccoBackend" -ForegroundColor Gray
Write-Host "Start-Sleep -Seconds 5" -ForegroundColor Gray
Write-Host ".\nssm.exe start MinetSaccoBackend" -ForegroundColor Gray
Write-Host "Start-Sleep -Seconds 20" -ForegroundColor Gray
Write-Host "netstat -ano | findstr :9090" -ForegroundColor Gray
Write-Host ""
