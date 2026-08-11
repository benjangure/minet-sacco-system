# ============================================
# Fix MinetSaccoBackend NSSM Service
# ============================================
# This script reconfigures the NSSM service to point to the correct backend JAR

Write-Host "=== MinetSaccoBackend Service Fix ===" -ForegroundColor Cyan
Write-Host ""

# Configuration
$nssmPath = "C:\Users\WakaeA\Downloads\nssm-2.24\win64\nssm.exe"
$serviceName = "MinetSaccoBackend"
$jarPath = "C:\minetsacco-deploy\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar"
$appDirectory = "C:\minetsacco-deploy\backend"
$logsDirectory = "C:\minetsacco-deploy\backend\logs"

# Step 1: Verify NSSM exists
Write-Host "[1/7] Checking NSSM..." -ForegroundColor Yellow
if (-not (Test-Path $nssmPath)) {
    Write-Host "ERROR: NSSM not found at $nssmPath" -ForegroundColor Red
    exit 1
}
Write-Host "✓ NSSM found" -ForegroundColor Green

# Step 2: Verify JAR exists
Write-Host "[2/7] Checking backend JAR..." -ForegroundColor Yellow
if (-not (Test-Path $jarPath)) {
    Write-Host "ERROR: Backend JAR not found at $jarPath" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Backend JAR found" -ForegroundColor Green

# Step 3: Find Java
Write-Host "[3/7] Locating Java..." -ForegroundColor Yellow
$javaPath = $null
try {
    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $javaPath = $javaCommand.Source
    }
} catch {}

if (-not $javaPath) {
    # Common Java installation paths
    $commonPaths = @(
        "C:\Program Files\Java\jdk-17\bin\java.exe",
        "C:\Program Files\Java\jdk-11\bin\java.exe",
        "C:\Program Files\Java\jre-17\bin\java.exe",
        "C:\Program Files\OpenJDK\jdk-17\bin\java.exe"
    )
    
    foreach ($path in $commonPaths) {
        if (Test-Path $path) {
            $javaPath = $path
            break
        }
    }
}

if (-not $javaPath) {
    Write-Host "ERROR: Java not found. Please install Java 17 or later." -ForegroundColor Red
    exit 1
}
Write-Host "✓ Java found at: $javaPath" -ForegroundColor Green

# Step 4: Create logs directory
Write-Host "[4/7] Creating logs directory..." -ForegroundColor Yellow
New-Item -ItemType Directory -Path $logsDirectory -Force | Out-Null
Write-Host "✓ Logs directory ready" -ForegroundColor Green

# Step 5: Stop service
Write-Host "[5/7] Stopping service..." -ForegroundColor Yellow
try {
    Stop-Service -Name $serviceName -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 3
    Write-Host "✓ Service stopped" -ForegroundColor Green
} catch {
    Write-Host "Warning: Could not stop service (may not be running)" -ForegroundColor Yellow
}

# Step 6: Reconfigure service
Write-Host "[6/7] Reconfiguring service..." -ForegroundColor Yellow

# Set application path
& $nssmPath set $serviceName Application "$javaPath"
Write-Host "  ✓ Application set to: $javaPath" -ForegroundColor Gray

# Set application parameters
& $nssmPath set $serviceName AppParameters "-jar `"$jarPath`""
Write-Host "  ✓ Parameters set to: -jar $jarPath" -ForegroundColor Gray

# Set working directory
& $nssmPath set $serviceName AppDirectory "$appDirectory"
Write-Host "  ✓ Working directory set to: $appDirectory" -ForegroundColor Gray

# Set stdout log
& $nssmPath set $serviceName AppStdout "$logsDirectory\stdout.log"
Write-Host "  ✓ Stdout log: $logsDirectory\stdout.log" -ForegroundColor Gray

# Set stderr log
& $nssmPath set $serviceName AppStderr "$logsDirectory\stderr.log"
Write-Host "  ✓ Stderr log: $logsDirectory\stderr.log" -ForegroundColor Gray

# Set file rotation
& $nssmPath set $serviceName AppStdoutCreationDisposition 4
& $nssmPath set $serviceName AppStderrCreationDisposition 4
Write-Host "  ✓ Log rotation configured" -ForegroundColor Gray

Write-Host "✓ Service reconfigured" -ForegroundColor Green

# Step 7: Start service
Write-Host "[7/7] Starting service..." -ForegroundColor Yellow
try {
    Start-Service -Name $serviceName
    Write-Host "✓ Service start command sent" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to start service: $_" -ForegroundColor Red
    exit 1
}

# Wait for service to stabilize
Write-Host ""
Write-Host "Waiting for service to start (15 seconds)..." -ForegroundColor Cyan
Start-Sleep -Seconds 15

# Check service status
Write-Host ""
Write-Host "=== Service Status ===" -ForegroundColor Cyan
$service = Get-Service -Name $serviceName
Write-Host "Status: $($service.Status)" -ForegroundColor $(if ($service.Status -eq 'Running') { 'Green' } else { 'Red' })

# Check if port 9090 is listening
Write-Host ""
Write-Host "=== Port 9090 Status ===" -ForegroundColor Cyan
$portCheck = netstat -ano | findstr :9090
if ($portCheck) {
    Write-Host "✓ Port 9090 is LISTENING!" -ForegroundColor Green
    Write-Host $portCheck
} else {
    Write-Host "✗ Port 9090 is NOT listening" -ForegroundColor Red
    Write-Host ""
    Write-Host "Checking logs for errors..." -ForegroundColor Yellow
    Write-Host ""
    
    if (Test-Path "$logsDirectory\stderr.log") {
        Write-Host "=== Recent Errors (stderr.log) ===" -ForegroundColor Red
        Get-Content "$logsDirectory\stderr.log" -Tail 30 -ErrorAction SilentlyContinue
    }
    
    Write-Host ""
    if (Test-Path "$logsDirectory\stdout.log") {
        Write-Host "=== Recent Output (stdout.log) ===" -ForegroundColor Yellow
        Get-Content "$logsDirectory\stdout.log" -Tail 30 -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "=== Fix Complete ===" -ForegroundColor Cyan
Write-Host "Service configuration has been updated." -ForegroundColor White
Write-Host ""
Write-Host "To view logs:" -ForegroundColor White
Write-Host "  Stdout: Get-Content '$logsDirectory\stdout.log' -Tail 50 -Wait" -ForegroundColor Gray
Write-Host "  Stderr: Get-Content '$logsDirectory\stderr.log' -Tail 50 -Wait" -ForegroundColor Gray
Write-Host ""
Write-Host "To test backend:" -ForegroundColor White
Write-Host "  Invoke-WebRequest -Uri 'http://localhost:9090/actuator/health' -UseBasicParsing" -ForegroundColor Gray
Write-Host ""
