# ============================================
# PRODUCTION BACKEND DIAGNOSTIC SCRIPT
# Run this on the production server (10.39.60.15)
# ============================================

Write-Host "=== MINET SACCO BACKEND DIAGNOSTIC ===" -ForegroundColor Cyan
Write-Host ""

# 1. Check JAR file on production
Write-Host "[1/8] Checking JAR file on production server..." -ForegroundColor Yellow
$productionJar = "C:\minetsacco-deploy\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar"
if (Test-Path $productionJar) {
    $jarInfo = Get-Item $productionJar
    Write-Host "✓ JAR exists at: $productionJar" -ForegroundColor Green
    Write-Host "  Size: $([math]::Round($jarInfo.Length/1MB,2)) MB" -ForegroundColor Gray
    Write-Host "  Last Modified: $($jarInfo.LastWriteTime)" -ForegroundColor Gray
    Write-Host "  Expected: Should be 8/5/2026 4:27 PM or later" -ForegroundColor Yellow
    
    if ($jarInfo.LastWriteTime -lt (Get-Date "2026-08-05 16:27:00")) {
        Write-Host "  ⚠ WARNING: This is the OLD JAR! Needs to be replaced." -ForegroundColor Red
    } else {
        Write-Host "  ✓ This appears to be the NEW JAR" -ForegroundColor Green
    }
} else {
    Write-Host "✗ JAR NOT FOUND at: $productionJar" -ForegroundColor Red
}

Write-Host ""

# 2. Extract and check config from production JAR
Write-Host "[2/8] Extracting configuration from production JAR..." -ForegroundColor Yellow
try {
    $tempDir = "C:\minetsacco-deploy\backend\target\temp_extract"
    if (Test-Path $tempDir) {
        Remove-Item $tempDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
    
    cd $tempDir
    jar -xf $productionJar BOOT-INF/classes/application.properties 2>$null
    
    if (Test-Path "BOOT-INF\classes\application.properties") {
        Write-Host "✓ Extracted application.properties" -ForegroundColor Green
        
        $profile = Get-Content "BOOT-INF\classes\application.properties" | Select-String -Pattern "^spring.profiles.active"
        $dbUrl = Get-Content "BOOT-INF\classes\application.properties" | Select-String -Pattern "^spring.datasource.url"
        $dbUser = Get-Content "BOOT-INF\classes\application.properties" | Select-String -Pattern "^spring.datasource.username"
        
        Write-Host "  Profile: $profile" -ForegroundColor Cyan
        Write-Host "  DB URL: $dbUrl" -ForegroundColor Cyan
        Write-Host "  DB User: $dbUser" -ForegroundColor Cyan
        
        if ($dbUser -match "tminet") {
            Write-Host "  ⚠ PROBLEM: JAR contains 'tminet' - OLD JAR!" -ForegroundColor Red
        } elseif ($dbUser -match "minetsacco") {
            Write-Host "  ✓ JAR correctly configured with 'minetsacco'" -ForegroundColor Green
        }
    } else {
        Write-Host "✗ Could not extract application.properties" -ForegroundColor Red
    }
    
    # Cleanup
    cd ..
    Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue
} catch {
    Write-Host "✗ Error extracting config: $_" -ForegroundColor Red
}

Write-Host ""

# 3. Check NSSM service configuration
Write-Host "[3/8] Checking NSSM service configuration..." -ForegroundColor Yellow
$service = Get-WmiObject Win32_Service | Where-Object {$_.Name -eq "MinetSaccoBackend"}
if ($service) {
    Write-Host "  Service Name: $($service.Name)" -ForegroundColor Gray
    Write-Host "  Display Name: $($service.DisplayName)" -ForegroundColor Gray
    Write-Host "  State: $($service.State)" -ForegroundColor Gray
    Write-Host "  Start Mode: $($service.StartMode)" -ForegroundColor Gray
    Write-Host "  Path Name: $($service.PathName)" -ForegroundColor Gray
    
    if ($service.PathName -like "*nssm.exe*") {
        Write-Host "  ⚠ Service points to NSSM (wrapper)" -ForegroundColor Yellow
    }
} else {
    Write-Host "✗ Service not found!" -ForegroundColor Red
}

Write-Host ""

# 4. Check NSSM parameters
Write-Host "[4/8] Checking NSSM application parameters..." -ForegroundColor Yellow
$nssmPath = "C:\Users\WakaeA\Downloads\nssm-2.24\win64\nssm.exe"
if (Test-Path $nssmPath) {
    $appPath = & $nssmPath get MinetSaccoBackend Application
    $appParams = & $nssmPath get MinetSaccoBackend AppParameters
    $appDir = & $nssmPath get MinetSaccoBackend AppDirectory
    
    Write-Host "  Application: $appPath" -ForegroundColor Cyan
    Write-Host "  Parameters: $appParams" -ForegroundColor Cyan
    Write-Host "  Directory: $appDir" -ForegroundColor Cyan
    
    # Check if JAR path in parameters matches our expected path
    if ($appParams -like "*$productionJar*") {
        Write-Host "  ✓ Service configured to use correct JAR path" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ Service JAR path mismatch!" -ForegroundColor Red
        Write-Host "    Expected: $productionJar" -ForegroundColor Yellow
        Write-Host "    Actual: $appParams" -ForegroundColor Yellow
    }
} else {
    Write-Host "✗ NSSM not found at: $nssmPath" -ForegroundColor Red
}

Write-Host ""

# 5. Check Java version
Write-Host "[5/8] Checking Java installation..." -ForegroundColor Yellow
try {
    $javaVersion = & java -version 2>&1 | Select-Object -First 1
    Write-Host "  Java: $javaVersion" -ForegroundColor Gray
} catch {
    Write-Host "✗ Java not found in PATH" -ForegroundColor Red
}

Write-Host ""

# 6. Check port 9090
Write-Host "[6/8] Checking port 9090..." -ForegroundColor Yellow
$port9090 = netstat -ano | findstr :9090
if ($port9090) {
    Write-Host "✓ Port 9090 is in use:" -ForegroundColor Green
    Write-Host "  $port9090" -ForegroundColor Gray
} else {
    Write-Host "✗ Port 9090 is NOT listening" -ForegroundColor Red
}

Write-Host ""

# 7. Check database connectivity
Write-Host "[7/8] Checking database..." -ForegroundColor Yellow
$mysqlService = Get-Service -Name "*MariaDB*","*MySQL*" -ErrorAction SilentlyContinue
if ($mysqlService) {
    foreach ($svc in $mysqlService) {
        Write-Host "  $($svc.DisplayName): $($svc.Status)" -ForegroundColor Gray
    }
} else {
    Write-Host "✗ No MariaDB/MySQL service found" -ForegroundColor Red
}

Write-Host ""

# 8. Check recent logs
Write-Host "[8/8] Checking recent errors in logs..." -ForegroundColor Yellow
$logFile = "C:\minetsacco-deploy\backend\logs\stdout.log"
if (Test-Path $logFile) {
    Write-Host "✓ Log file exists" -ForegroundColor Green
    $tminetrefs = Get-Content $logFile | Select-String "tminet" -SimpleMatch | Select-Object -Last 1
    if ($tminetrefs) {
        Write-Host "  ⚠ Found 'tminet' in logs (OLD JAR is running):" -ForegroundColor Red
        Write-Host "    $($tminetrefs.Line)" -ForegroundColor Yellow
    }
    
    $minetsaccorefs = Get-Content $logFile | Select-String "minetsacco" -SimpleMatch | Select-Object -Last 1
    if ($minetsaccorefs) {
        Write-Host "  ✓ Found 'minetsacco' in logs (NEW JAR started):" -ForegroundColor Green
        Write-Host "    $($minetsaccorefs.Line)" -ForegroundColor Gray
    }
} else {
    Write-Host "✗ Log file not found: $logFile" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== DIAGNOSIS COMPLETE ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "=== SUMMARY ===" -ForegroundColor Cyan
Write-Host "If JAR timestamp is OLD (before 4:27 PM):" -ForegroundColor White
Write-Host "  → Copy new JAR from local machine to production" -ForegroundColor Yellow
Write-Host "  → File: C:\minetsacco-deploy\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar" -ForegroundColor Gray
Write-Host ""
Write-Host "If JAR timestamp is NEW but logs show 'tminet':" -ForegroundColor White
Write-Host "  → Service hasn't restarted or is cached" -ForegroundColor Yellow
Write-Host "  → Kill any Java processes and restart service" -ForegroundColor Gray
Write-Host ""
