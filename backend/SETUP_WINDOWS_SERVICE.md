# Setup Spring Boot Backend as Windows Service with Auto-Restart

## Option 1: Using NSSM (Recommended - Easy Setup)

### Step 1: Download NSSM
1. Download NSSM from: https://nssm.cc/download
2. Extract the ZIP file
3. Copy `nssm.exe` (from win64 folder) to a permanent location like `C:\nssm\nssm.exe`

### Step 2: Install the Service

Open PowerShell as Administrator and run:

```powershell
# Navigate to where you saved nssm.exe
cd C:\nssm

# Install the service
.\nssm.exe install MinetSaccoBackend
```

### Step 3: Configure the Service in the GUI

The NSSM GUI will open. Configure as follows:

**Application Tab:**
- Path: `C:\Program Files\Java\jdk-17\bin\java.exe` (adjust to your Java path)
- Startup directory: `C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target`
- Arguments: `-jar sacco-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`

**Details Tab:**
- Display name: `Minet Sacco Backend`
- Description: `Minet Sacco Management System Backend Service`
- Startup type: `Automatic`

**I/O Tab:**
- Output (stdout): `C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\logs\service-out.log`
- Error (stderr): `C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\logs\service-error.log`

**Rotation Tab:**
- Check "Rotate files"
- Rotate when bigger than: 10000000 bytes (10MB)

**Exit Actions Tab:**
- This is KEY for auto-restart!
- Throttle: 5000 milliseconds (wait 5 seconds before restart)
- Restart: For all exit codes (including crashes)

**Environment Tab (if needed):**
Add environment variables if your app needs them:
```
SPRING_PROFILES_ACTIVE=prod
```

Click "Install service"

### Step 4: Start the Service

```powershell
# Start the service
nssm start MinetSaccoBackend

# Check service status
nssm status MinetSaccoBackend

# View logs
Get-Content "C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\logs\service-out.log" -Tail 50
```

### Step 5: Useful Commands

```powershell
# Stop the service
nssm stop MinetSaccoBackend

# Restart the service
nssm restart MinetSaccoBackend

# Edit service configuration
nssm edit MinetSaccoBackend

# Remove the service (if needed)
nssm remove MinetSaccoBackend confirm
```

---

## Option 2: Using Windows Service Wrapper (WinSW)

### Step 1: Download WinSW
1. Download from: https://github.com/winsw/winsw/releases
2. Download `WinSW-x64.exe`
3. Rename it to `MinetSaccoBackend.exe`
4. Place it in: `C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\`

### Step 2: Create Configuration File

Create `MinetSaccoBackend.xml` in the same folder:

```xml
<service>
  <id>MinetSaccoBackend</id>
  <name>Minet Sacco Backend</name>
  <description>Minet Sacco Management System Backend Service</description>
  
  <executable>C:\Program Files\Java\jdk-17\bin\java.exe</executable>
  <arguments>-jar "C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target\sacco-0.0.1-SNAPSHOT.jar" --spring.profiles.active=prod</arguments>
  
  <workingdirectory>C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target</workingdirectory>
  
  <logmode>rotate</logmode>
  <logpath>C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\logs</logpath>
  
  <!-- Auto-restart configuration -->
  <onfailure action="restart" delay="10 sec"/>
  <onfailure action="restart" delay="20 sec"/>
  <onfailure action="restart" delay="30 sec"/>
  
  <!-- Reset failure count after 1 hour of successful running -->
  <resetfailure>1 hour</resetfailure>
  
  <startmode>Automatic</startmode>
  
  <!-- Environment variables -->
  <env name="SPRING_PROFILES_ACTIVE" value="prod"/>
</service>
```

### Step 3: Install and Start

Open PowerShell as Administrator:

```powershell
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend

# Install the service
.\MinetSaccoBackend.exe install

# Start the service
.\MinetSaccoBackend.exe start

# Check status
.\MinetSaccoBackend.exe status
```

### Step 4: Useful Commands

```powershell
# Stop the service
.\MinetSaccoBackend.exe stop

# Restart the service
.\MinetSaccoBackend.exe restart

# Uninstall the service
.\MinetSaccoBackend.exe uninstall
```

---

## Option 3: Using Native Spring Boot Windows Service Support

### Step 1: Add Maven Plugin to pom.xml

Add this to your `backend/pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <executable>true</executable>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Step 2: Download WinSW and Configure

Follow the WinSW steps above but with this simpler XML:

```xml
<service>
  <id>MinetSaccoBackend</id>
  <name>Minet Sacco Backend</name>
  <description>Minet Sacco Management System Backend Service</description>
  
  <executable>C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target\sacco-0.0.1-SNAPSHOT.jar</executable>
  
  <logmode>rotate</logmode>
  <onfailure action="restart" delay="10 sec"/>
  <startmode>Automatic</startmode>
</service>
```

---

## Troubleshooting Auto-Restart Issues

### 1. Check Windows Event Viewer
```powershell
# Open Event Viewer
eventvwr.msc

# Navigate to: Windows Logs > Application
# Look for errors from your service
```

### 2. Check Service Recovery Options

If using NSSM or WinSW doesn't work, manually configure Windows Service Recovery:

1. Open Services: `services.msc`
2. Find your service
3. Right-click > Properties > Recovery tab
4. Set:
   - First failure: Restart the Service
   - Second failure: Restart the Service
   - Subsequent failures: Restart the Service
   - Restart service after: 1 minute

### 3. Common Issues and Solutions

**Service won't start:**
- Check Java path is correct
- Verify JAR file exists at specified path
- Check logs folder exists
- Ensure database is accessible

**Service starts but crashes immediately:**
- Check application.properties database connection
- Verify all required environment variables are set
- Check application logs in the logs folder

**Service doesn't auto-restart:**
- In NSSM: Verify Exit Actions tab is configured
- In WinSW: Check `<onfailure>` tags in XML
- Check Windows Service Recovery settings

### 4. Monitor the Service

Create a monitoring script `check-service.ps1`:

```powershell
$serviceName = "MinetSaccoBackend"
$service = Get-Service -Name $serviceName -ErrorAction SilentlyContinue

if ($service.Status -ne 'Running') {
    Write-Host "Service is not running. Starting..."
    Start-Service -Name $serviceName
    Start-Sleep -Seconds 5
    
    $service = Get-Service -Name $serviceName
    if ($service.Status -eq 'Running') {
        Write-Host "Service started successfully"
    } else {
        Write-Host "Failed to start service"
    }
} else {
    Write-Host "Service is running normally"
}
```

Schedule this script to run every 5 minutes using Task Scheduler.

---

## Recommended Approach

**For Production:** Use NSSM (Option 1)
- Most reliable
- Easy to configure
- Built-in auto-restart
- Good logging support
- Easy to manage

**For Development:** Run manually with auto-restart script
- More flexible
- Easier to debug
- Can see logs in real-time

---

## After Setup Checklist

✅ Service starts automatically on system boot
✅ Service restarts automatically on crash
✅ Logs are being written and rotated
✅ Can manually stop/start/restart service
✅ Service survives system restart
✅ Application is accessible on http://localhost:9090

---

## Quick Reference Commands

### NSSM
```powershell
nssm install MinetSaccoBackend
nssm start MinetSaccoBackend
nssm stop MinetSaccoBackend
nssm restart MinetSaccoBackend
nssm status MinetSaccoBackend
nssm edit MinetSaccoBackend
```

### Windows Services
```powershell
Get-Service MinetSaccoBackend
Start-Service MinetSaccoBackend
Stop-Service MinetSaccoBackend
Restart-Service MinetSaccoBackend
```

### Check if service is running
```powershell
Get-Service MinetSaccoBackend | Select-Object Status
```

### View logs
```powershell
Get-Content "C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\logs\service-out.log" -Tail 50 -Wait
```
