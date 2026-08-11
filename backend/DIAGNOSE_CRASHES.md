# Diagnosing Backend Crashes

## Step 1: Check the Logs

The most important step is to see WHY it's crashing. Check these logs:

```powershell
# Check Spring Boot application logs
Get-Content "C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\logs\spring.log" -Tail 100

# Or if you're running manually, check the console output
```

Look for:
- `OutOfMemoryError` - Memory issues
- `Connection refused` - Database connection issues
- `Table doesn't exist` - Missing database tables/columns
- `ClassNotFoundException` - Missing dependencies
- `BindException: Address already in use` - Port 9090 already taken

---

## Step 2: Common Issues and Fixes

### Issue 1: Out of Memory Error

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Fix:** Increase memory allocation

In your start script or service configuration, add:
```bash
java -Xms512m -Xmx2048m -jar sacco-0.0.1-SNAPSHOT.jar
```

Or add to `application.properties`:
```properties
# JVM options
spring.jmx.enabled=false
```

### Issue 2: Database Connection Issues

**Symptoms:**
```
Connection refused
Communications link failure
Unable to acquire JDBC Connection
```

**Fix:** Check database connection in `application.properties` or `.env`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/minet_sacco?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password

# Add connection pool settings
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=60000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Issue 3: Missing Database Columns

**Symptoms:**
```
Unknown column 'xxx' in 'field list'
```

**Fix:** You already ran the SQL migration scripts. Verify all columns exist:
```sql
SHOW COLUMNS FROM loans;
SHOW COLUMNS FROM loan_topup_requests;
SHOW COLUMNS FROM loan_topup_history;
```

### Issue 4: Port Already in Use

**Symptoms:**
```
Port 9090 was already in use
Address already in use: bind
```

**Fix:** Kill the process using port 9090:
```powershell
# Find process using port 9090
netstat -ano | findstr :9090

# Kill the process (replace PID with actual process ID)
taskkill /PID <PID> /F
```

Or change the port in `application.properties`:
```properties
server.port=9091
```

### Issue 5: Circular Dependency or Bean Creation Error

**Symptoms:**
```
The dependencies of some of the beans in the application context form a cycle
Error creating bean with name 'xxx'
```

**Fix:** This is a code issue. Check recent changes to:
- Service classes with @Autowired
- Configuration classes
- Component scanning

### Issue 6: File/Resource Not Found

**Symptoms:**
```
FileNotFoundException
NoSuchFileException
```

**Fix:** Check that all required files exist:
- `application.properties`
- `application-prod.properties`
- `.env` file (if using)
- SSL certificates (if configured)

---

## Step 3: Compare with Working Version

Since the previous backend wasn't crashing, let's find out what changed:

### Check Git History
```powershell
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend

# See recent changes
git log --oneline -20

# See what files changed recently
git diff HEAD~5 HEAD --name-only

# Compare with last stable version
git diff <last-stable-commit> HEAD
```

### Compare Configuration Files

Check if these files were modified:
- `pom.xml` - Dependencies might have changed
- `application.properties` - Configuration might be wrong
- `.env` - Environment variables might be missing

---

## Step 4: Test with Different Profiles

Try running with the dev profile to see if it's a production-specific issue:

```powershell
java -jar target/sacco-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

If dev works but prod doesn't, compare:
- `application-dev.properties`
- `application-prod.properties`

---

## Step 5: Check System Resources

### Memory Usage
```powershell
# Check available memory
Get-WmiObject -Class Win32_OperatingSystem | Select-Object FreePhysicalMemory, TotalVisibleMemorySize
```

If memory is low:
- Close unnecessary applications
- Increase Java heap size
- Add more RAM to server

### Disk Space
```powershell
# Check disk space
Get-PSDrive -PSProvider FileSystem
```

If disk is full:
- Clean up old logs
- Delete unnecessary files
- Increase disk space

### CPU Usage
```powershell
# Monitor CPU while backend is running
Get-Process java | Select-Object CPU, WorkingSet
```

---

## Step 6: Run with Debug Logging

Enable debug logging to see detailed error information:

In `application.properties` (or `application-prod.properties`):
```properties
# Enable debug logging
logging.level.root=INFO
logging.level.com.minet.sacco=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# Log to file
logging.file.name=logs/application.log
logging.file.max-size=10MB
logging.file.max-history=10
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

Then check the logs after restart:
```powershell
Get-Content "logs/application.log" -Tail 200
```

---

## Step 7: Test Specific Components

Test if the crash happens during startup or during operation:

### Test 1: Can it start successfully?
```powershell
java -jar target/sacco-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Watch the console. If it crashes within first 30 seconds = startup issue
If it runs fine for a while then crashes = runtime issue

### Test 2: Can it connect to database?
```powershell
# Test database connection
mysql -h localhost -u root -p
USE minet_sacco;
SELECT COUNT(*) FROM members;
```

### Test 3: Can it serve requests?
```powershell
# Test health endpoint
Invoke-WebRequest -Uri "http://localhost:9090/actuator/health" -Method GET
```

---

## Most Likely Causes Based on Your Situation

Since you just:
1. Added new database columns
2. Modified frontend code (TopUpGuarantorApprovalModal)
3. Added top-up functionality

The crash is likely due to:

### 1. Missing Database Columns (Most Likely)

**Check:** Did you run ALL the SQL migration scripts?
```sql
-- Verify these columns exist
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'loans' 
AND COLUMN_NAME IN (
    'interest_collected',
    'interest_collected_manual_override',
    'principal_repaid',
    'principal_repaid_manual_override',
    'interest_remaining'
);
```

Should return 5 rows. If not, run the migration scripts again.

### 2. Old JAR File Running

**Check:** Are you running the newly built JAR?
```powershell
# Build new JAR
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend
mvn clean package -DskipTests

# Check build date
Get-ChildItem target\sacco-0.0.1-SNAPSHOT.jar | Select-Object Name, LastWriteTime

# Make sure you're running THIS jar file
```

### 3. Database Connection Lost

**Check:** Is MySQL running?
```powershell
# Check MySQL service
Get-Service -Name "MySQL*"

# If stopped, start it
Start-Service -Name "MySQL80"  # Adjust name as needed
```

---

## Quick Diagnostic Script

Save this as `diagnose-backend.ps1`:

```powershell
Write-Host "=== Minet Sacco Backend Diagnostics ===" -ForegroundColor Cyan

# 1. Check if Java is available
Write-Host "`n1. Checking Java..." -ForegroundColor Yellow
java -version 2>&1 | Select-Object -First 1

# 2. Check if MySQL is running
Write-Host "`n2. Checking MySQL..." -ForegroundColor Yellow
$mysqlService = Get-Service -Name "MySQL*" -ErrorAction SilentlyContinue
if ($mysqlService) {
    Write-Host "MySQL Status: $($mysqlService.Status)" -ForegroundColor $(if ($mysqlService.Status -eq 'Running') {'Green'} else {'Red'})
} else {
    Write-Host "MySQL service not found!" -ForegroundColor Red
}

# 3. Check if port 9090 is in use
Write-Host "`n3. Checking Port 9090..." -ForegroundColor Yellow
$portCheck = netstat -ano | Select-String ":9090"
if ($portCheck) {
    Write-Host "Port 9090 is in use:" -ForegroundColor Red
    $portCheck | ForEach-Object { Write-Host $_ }
} else {
    Write-Host "Port 9090 is available" -ForegroundColor Green
}

# 4. Check JAR file
Write-Host "`n4. Checking JAR file..." -ForegroundColor Yellow
$jarPath = "C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target\sacco-0.0.1-SNAPSHOT.jar"
if (Test-Path $jarPath) {
    $jarInfo = Get-ChildItem $jarPath
    Write-Host "JAR found: $($jarInfo.LastWriteTime)" -ForegroundColor Green
} else {
    Write-Host "JAR file not found!" -ForegroundColor Red
}

# 5. Check available memory
Write-Host "`n5. Checking System Memory..." -ForegroundColor Yellow
$os = Get-WmiObject -Class Win32_OperatingSystem
$freeMemoryMB = [math]::Round($os.FreePhysicalMemory / 1024, 2)
$totalMemoryMB = [math]::Round($os.TotalVisibleMemorySize / 1024, 2)
Write-Host "Free Memory: $freeMemoryMB MB / $totalMemoryMB MB" -ForegroundColor $(if ($freeMemoryMB -gt 1000) {'Green'} else {'Yellow'})

# 6. Check recent logs
Write-Host "`n6. Checking Recent Logs..." -ForegroundColor Yellow
$logPath = "C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\logs\application.log"
if (Test-Path $logPath) {
    Write-Host "Last 10 lines of log:" -ForegroundColor Cyan
    Get-Content $logPath -Tail 10
} else {
    Write-Host "No log file found at $logPath" -ForegroundColor Yellow
}

Write-Host "`n=== Diagnostics Complete ===" -ForegroundColor Cyan
```

Run it:
```powershell
.\diagnose-backend.ps1
```

---

## Next Steps

1. **Run the diagnostic script above**
2. **Check the actual error in logs**
3. **Share the error message** so I can help with the specific issue
4. **Verify database columns** are all added
5. **Rebuild and run the new JAR**

The previous backend wasn't crashing because it didn't have the new code/columns. Now that we've added features, we need to ensure:
- All database migrations ran successfully
- The new JAR is properly built
- Configuration is correct

Let me know what the logs show and I'll help fix the specific issue!
