Write-Host "=== Minet Sacco Backend Crash Diagnostics ===" -ForegroundColor Cyan

# 1. Check if Java is available
Write-Host "`n1. Checking Java..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "Java: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "Java not found or not in PATH!" -ForegroundColor Red
}

# 2. Check if MySQL is running
Write-Host "`n2. Checking MySQL..." -ForegroundColor Yellow
$mysqlService = Get-Service -Name "MySQL*" -ErrorAction SilentlyContinue
if ($mysqlService) {
    $status = $mysqlService.Status
    Write-Host "MySQL Status: $status" -ForegroundColor $(if ($status -eq 'Running') {'Green'} else {'Red'})
    if ($status -ne 'Running') {
        Write-Host "Attempting to start MySQL..." -ForegroundColor Yellow
        try {
            Start-Service $mysqlService.Name
            Write-Host "MySQL started successfully" -ForegroundColor Green
        } catch {
            Write-Host "Failed to start MySQL: $_" -ForegroundColor Red
        }
    }
} else {
    Write-Host "MySQL service not found!" -ForegroundColor Red
}

# 3. Check if port 9090 is in use
Write-Host "`n3. Checking Port 9090..." -ForegroundColor Yellow
$portCheck = netstat -ano | Select-String ":9090" | Select-String "LISTENING"
if ($portCheck) {
    Write-Host "Port 9090 is in use by:" -ForegroundColor Red
    $portCheck | ForEach-Object {
        $line = $_.ToString()
        if ($line -match '\s+(\d+)$') {
            $pid = $matches[1]
            $process = Get-Process -Id $pid -ErrorAction SilentlyContinue
            if ($process) {
                Write-Host "  PID: $pid - Process: $($process.ProcessName)" -ForegroundColor Yellow
            } else {
                Write-Host "  PID: $pid - Process not found" -ForegroundColor Yellow
            }
        }
    }
    Write-Host "`nTo kill the process, run: taskkill /PID <PID> /F" -ForegroundColor Cyan
} else {
    Write-Host "Port 9090 is available" -ForegroundColor Green
}

# 4. Check JAR file
Write-Host "`n4. Checking JAR file..." -ForegroundColor Yellow
$jarPath = "target\minet-sacco-backend-0.0.1-SNAPSHOT.jar"
if (Test-Path $jarPath) {
    $jarInfo = Get-ChildItem $jarPath
    Write-Host "JAR found: Last modified $($jarInfo.LastWriteTime)" -ForegroundColor Green
    $sizeMB = [math]::Round($jarInfo.Length / 1MB, 2)
    Write-Host "JAR size: $sizeMB MB" -ForegroundColor Green
} else {
    Write-Host "JAR file not found at $jarPath!" -ForegroundColor Red
    Write-Host "Run: mvn clean package -DskipTests" -ForegroundColor Yellow
}

# 5. Check available memory
Write-Host "`n5. Checking System Memory..." -ForegroundColor Yellow
try {
    $os = Get-CimInstance -ClassName Win32_OperatingSystem
    $freeMemoryMB = [math]::Round($os.FreePhysicalMemory / 1024, 2)
    $totalMemoryMB = [math]::Round($os.TotalVisibleMemorySize / 1024, 2)
    $usedPercent = [math]::Round((($totalMemoryMB - $freeMemoryMB) / $totalMemoryMB) * 100, 1)
    Write-Host "Free Memory: $freeMemoryMB MB / $totalMemoryMB MB ($usedPercent% used)" -ForegroundColor $(if ($freeMemoryMB -gt 1000) {'Green'} else {'Yellow'})
    
    if ($freeMemoryMB -lt 500) {
        Write-Host "WARNING: Low memory! Close unnecessary applications." -ForegroundColor Red
    }
} catch {
    Write-Host "Could not check memory: $_" -ForegroundColor Yellow
}

# 6. Check disk space
Write-Host "`n6. Checking Disk Space..." -ForegroundColor Yellow
Get-PSDrive -PSProvider FileSystem | Where-Object { $_.Used -ne $null } | ForEach-Object {
    $freeMB = [math]::Round($_.Free / 1MB, 2)
    $usedMB = [math]::Round($_.Used / 1MB, 2)
    $totalMB = $freeMB + $usedMB
    $usedPercent = [math]::Round(($usedMB / $totalMB) * 100, 1)
    Write-Host "$($_.Name) drive: $freeMB MB free / $totalMB MB total ($usedPercent% used)" -ForegroundColor $(if ($freeMB -gt 1000) {'Green'} else {'Yellow'})
}

# 7. Check if Windows service exists
Write-Host "`n7. Checking Windows Service..." -ForegroundColor Yellow
$service = Get-Service -Name "MinetSaccoBackend" -ErrorAction SilentlyContinue
if ($service) {
    Write-Host "Service Status: $($service.Status)" -ForegroundColor $(if ($service.Status -eq 'Running') {'Green'} else {'Yellow'})
    Write-Host "Service Startup Type: $($service.StartType)" -ForegroundColor Cyan
} else {
    Write-Host "No Windows service installed (running manually)" -ForegroundColor Yellow
}

# 8. Check recent application logs
Write-Host "`n8. Checking Recent Logs..." -ForegroundColor Yellow
$logPaths = @(
    "logs\application.log",
    "logs\spring.log",
    "logs\service-error.log",
    "logs\service-out.log"
)

$logFound = $false
foreach ($logPath in $logPaths) {
    if (Test-Path $logPath) {
        $logFound = $true
        Write-Host "`nLast 20 lines from $logPath" -ForegroundColor Cyan
        Get-Content $logPath -Tail 20 -ErrorAction SilentlyContinue
        Write-Host "`n" + ("=" * 80) -ForegroundColor Gray
    }
}

if (-not $logFound) {
    Write-Host "No log files found. Check these locations:" -ForegroundColor Yellow
    foreach ($logPath in $logPaths) {
        Write-Host "  - $logPath" -ForegroundColor Gray
    }
}

# 9. Check database connection
Write-Host "`n9. Testing Database Connection..." -ForegroundColor Yellow
$mysqlExe = Get-Command mysql -ErrorAction SilentlyContinue
if ($mysqlExe) {
    Write-Host "MySQL client found. Testing connection to minetsacco database..." -ForegroundColor Cyan
    # Note: This requires user to input password manually if needed
    Write-Host "Run this command to test manually: mysql -u minetsacco -p minetsacco -e 'SELECT 1;'" -ForegroundColor Gray
} else {
    Write-Host "MySQL client not in PATH. Cannot test database connection automatically." -ForegroundColor Yellow
}

# 10. Summary and recommendations
Write-Host "`n=== SUMMARY & RECOMMENDATIONS ===" -ForegroundColor Cyan

$issues = @()
if (-not $mysqlService -or $mysqlService.Status -ne 'Running') {
    $issues += "MySQL is not running - start it first"
}
if ($portCheck) {
    $issues += "Port 9090 is blocked - kill the process or use a different port"
}
if (-not (Test-Path $jarPath)) {
    $issues += "JAR file missing - rebuild with 'mvn clean package -DskipTests'"
}
if ($freeMemoryMB -lt 500) {
    $issues += "Low memory - close unnecessary applications or increase RAM"
}

if ($issues.Count -eq 0) {
    Write-Host "`nNo obvious issues detected. Backend should be able to start." -ForegroundColor Green
    Write-Host "`nTo start the backend, run:" -ForegroundColor Cyan
    Write-Host "  java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.datasource.username=minetsacco --spring.datasource.password=0a0b0c0D. --spring.datasource.url=jdbc:mysql://localhost:3306/minetsacco?createDatabaseIfNotExist=true" -ForegroundColor Yellow
} else {
    Write-Host "`nIssues found:" -ForegroundColor Red
    $issues | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
    Write-Host "`nFix these issues before attempting to start the backend." -ForegroundColor Cyan
}

Write-Host "`n=== Diagnostics Complete ===" -ForegroundColor Cyan
