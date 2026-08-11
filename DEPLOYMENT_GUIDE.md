# Deployment Guide for Minet SACCO System

**Date:** July 28, 2026  
**Target Server:** 10.39.60.15 (Windows Server)  
**Status:** ✅ Build Successful - Ready for Deployment

---

## Build Status

### ✅ Frontend Build
- **Status:** SUCCESS
- **Build Time:** 16.06s
- **Output Directory:** `minetsacco-main/dist/`
- **Main Bundle Size:** 1,534.90 kB (375.31 kB gzipped)
- **CSS Size:** 82.63 kB (13.91 kB gzipped)

### ✅ Backend Build
- **Status:** SUCCESS
- **Build Time:** 02:01 min
- **Artifact:** `backend/target/minet-sacco-backend-0.0.1-SNAPSHOT.jar`
- **Java Version:** 17
- **Spring Boot Version:** 3.2.0

---

## Pre-Deployment Checklist

### 1. Server Requirements
- ✅ Java 17 installed
- ✅ MySQL 8.0 running
- ✅ IIS installed and configured
- ⚠️ MySQL database `minetsacco` and user must be configured

### 2. Database Setup Required on Server

**Connect to MySQL as root:**
```powershell
mysql -u root -p
```

**Run this setup script:**
```sql
-- Create database
CREATE DATABASE IF NOT EXISTS minetsacco 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- Create user
DROP USER IF EXISTS 'minetsacco'@'localhost';
CREATE USER 'minetsacco'@'localhost' IDENTIFIED BY '0a0b0c0D.';

-- Grant permissions
GRANT ALL PRIVILEGES ON minetsacco.* TO 'minetsacco'@'localhost';
GRANT CREATE, ALTER, DROP, INSERT, UPDATE, DELETE, SELECT, REFERENCES 
  ON minetsacco.* TO 'minetsacco'@'localhost';

FLUSH PRIVILEGES;

-- Verify
USE minetsacco;
SHOW TABLES;
SELECT User, Host FROM mysql.user WHERE User='minetsacco';
```

**Test connection:**
```powershell
mysql -u minetsacco -p0a0b0c0D. minetsacco
```

### 3. Configuration Files Review

#### Backend Configuration
**File:** `backend/src/main/resources/application.properties`

Key settings:
```properties
# Server
server.port=9090
server.address=0.0.0.0

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/minetsacco?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=minetsacco
spring.datasource.password=0a0b0c0D.

# CORS (allows 10.39.* network)
# Configured in CorsConfig.java

# JWT Secret
jwt.secret=YourVerySecureSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm
jwt.expiration=1800000
```

#### Frontend Configuration
**File:** `minetsacco-main/src/config/api.ts`

```typescript
export function getApiBaseUrl(): string {
  const hostname = window.location.hostname;
  
  if (hostname === 'localhost' || hostname === '127.0.0.1') {
    return 'http://localhost:9090/api';
  }
  
  return `http://${hostname}:9090/api`;
}
```

This automatically uses `http://10.39.60.15:9090/api` when accessed from the server.

---

## Deployment Steps

### Step 1: Transfer Files to Server

**Option A: Using Remote Desktop**
1. Connect to server: `mstsc /v:10.39.60.15`
2. Copy files via RDP clipboard or shared drive

**Option B: Using Network Share**
```powershell
# From your local machine
Copy-Item -Path "c:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar" -Destination "\\10.39.60.15\C$\minetsacco-deploy\backend\" -Force

Copy-Item -Path "c:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\minetsacco-main\dist\*" -Destination "\\10.39.60.15\C$\inetpub\minetsacco\" -Recurse -Force
```

**Option C: Using SCP/WinSCP**
- Backend JAR: Upload to `C:\minetsacco-deploy\backend\`
- Frontend dist: Upload to `C:\inetpub\minetsacco\`

### Step 2: Deploy Backend

**On the server, create deployment directory:**
```powershell
New-Item -ItemType Directory -Path "C:\minetsacco-deploy\backend" -Force
```

**Copy the JAR file:**
```powershell
# If not already copied
Copy-Item "path\to\minet-sacco-backend-0.0.1-SNAPSHOT.jar" -Destination "C:\minetsacco-deploy\backend\"
```

**Test the backend:**
```powershell
cd C:\minetsacco-deploy\backend
java -jar minet-sacco-backend-0.0.1-SNAPSHOT.jar
```

**Expected output:**
```
Started MinetSaccoBackendApplication in X.XXX seconds
```

**Test backend is running:**
```powershell
# Open browser or use curl
curl http://localhost:9090/api/members
```

### Step 3: Configure Backend as Windows Service

**Create a service using NSSM (Non-Sucking Service Manager):**

1. **Download NSSM:**
   ```powershell
   # Download from https://nssm.cc/download
   # Or use chocolatey
   choco install nssm
   ```

2. **Install service:**
   ```powershell
   nssm install MinetSaccoBackend "C:\Program Files\Java\jdk-17\bin\java.exe" "-jar C:\minetsacco-deploy\backend\minet-sacco-backend-0.0.1-SNAPSHOT.jar"
   ```

3. **Configure service:**
   ```powershell
   nssm set MinetSaccoBackend AppDirectory "C:\minetsacco-deploy\backend"
   nssm set MinetSaccoBackend DisplayName "Minet SACCO Backend Service"
   nssm set MinetSaccoBackend Description "Minet SACCO Management System Backend API"
   nssm set MinetSaccoBackend Start SERVICE_AUTO_START
   
   # Set output logs
   nssm set MinetSaccoBackend AppStdout "C:\minetsacco-deploy\backend\logs\service.log"
   nssm set MinetSaccoBackend AppStderr "C:\minetsacco-deploy\backend\logs\error.log"
   ```

4. **Start the service:**
   ```powershell
   nssm start MinetSaccoBackend
   
   # Check status
   nssm status MinetSaccoBackend
   
   # Or use Windows services
   Get-Service MinetSaccoBackend
   ```

**Alternative: Using Task Scheduler (simpler)**

Create a scheduled task that runs at startup:
```powershell
$action = New-ScheduledTaskAction -Execute "java.exe" -Argument "-jar C:\minetsacco-deploy\backend\minet-sacco-backend-0.0.1-SNAPSHOT.jar" -WorkingDirectory "C:\minetsacco-deploy\backend"
$trigger = New-ScheduledTaskTrigger -AtStartup
$principal = New-ScheduledTaskPrincipal -UserId "SYSTEM" -LogonType ServiceAccount -RunLevel Highest
Register-ScheduledTask -TaskName "MinetSaccoBackend" -Action $action -Trigger $trigger -Principal $principal -Description "Minet SACCO Backend Service"
```

### Step 4: Deploy Frontend to IIS

**Create IIS website:**
```powershell
# Import IIS module
Import-Module WebAdministration

# Create application pool
New-WebAppPool -Name "MinetSaccoPool"

# Create website directory
New-Item -ItemType Directory -Path "C:\inetpub\minetsacco" -Force

# Create IIS website
New-Website -Name "MinetSacco" -Port 8090 -PhysicalPath "C:\inetpub\minetsacco" -ApplicationPool "MinetSaccoPool"

# Set binding to listen on all IPs
New-WebBinding -Name "MinetSacco" -IPAddress "*" -Port 8090 -Protocol http
```

**Copy frontend files:**
```powershell
# Copy built frontend
Copy-Item -Path "path\to\minetsacco-main\dist\*" -Destination "C:\inetpub\minetsacco\" -Recurse -Force
```

**Configure URL Rewrite (for React Router):**

Create `C:\inetpub\minetsacco\web.config`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <system.webServer>
    <rewrite>
      <rules>
        <rule name="React Routes" stopProcessing="true">
          <match url=".*" />
          <conditions logicalGrouping="MatchAll">
            <add input="{REQUEST_FILENAME}" matchType="IsFile" negate="true" />
            <add input="{REQUEST_FILENAME}" matchType="IsDirectory" negate="true" />
          </conditions>
          <action type="Rewrite" url="/" />
        </rule>
      </rules>
    </rewrite>
    <staticContent>
      <mimeMap fileExtension=".json" mimeType="application/json" />
      <mimeMap fileExtension=".woff" mimeType="application/font-woff" />
      <mimeMap fileExtension=".woff2" mimeType="application/font-woff2" />
    </staticContent>
  </system.webServer>
</configuration>
```

**Start the website:**
```powershell
Start-Website -Name "MinetSacco"
```

### Step 5: Configure Firewall

**Allow backend port:**
```powershell
New-NetFirewallRule -DisplayName "Minet SACCO Backend" -Direction Inbound -LocalPort 9090 -Protocol TCP -Action Allow
```

**Allow frontend port:**
```powershell
New-NetFirewallRule -DisplayName "Minet SACCO Frontend" -Direction Inbound -LocalPort 8090 -Protocol TCP -Action Allow
```

---

## Post-Deployment Testing

### 1. Test Backend API

**Health check:**
```powershell
curl http://10.39.60.15:9090/api/members
```

**Expected:** JSON response or 401 Unauthorized (which means API is running)

### 2. Test Frontend

**Open in browser:**
```
http://10.39.60.15:8090
```

**Expected:** Login page loads

### 3. Test Login

1. Navigate to `http://10.39.60.15:8090`
2. Login with credentials
3. Verify dashboard loads
4. Check browser console for errors

### 4. Test Treasurer Features

**As TREASURER user:**

1. Navigate to Loans page
2. Try to edit a loan's principal and outstanding balance
3. Try to delete a loan
4. Verify audit logs are created

**Test endpoints:**
- `PUT /api/loans/{id}/update-financials` - Update loan financials
- `DELETE /api/loans/{id}` - Delete loan

---

## Monitoring & Logs

### Backend Logs

**If using NSSM:**
```powershell
Get-Content "C:\minetsacco-deploy\backend\logs\service.log" -Tail 50 -Wait
Get-Content "C:\minetsacco-deploy\backend\logs\error.log" -Tail 50 -Wait
```

**Spring Boot application logs:**
```powershell
Get-Content "C:\minetsacco-deploy\backend\logs\spring-boot-application.log" -Tail 50 -Wait
```

### IIS Logs

```powershell
Get-Content "C:\inetpub\logs\LogFiles\W3SVC*\*.log" -Tail 50 -Wait
```

### Check Service Status

```powershell
# If using NSSM
nssm status MinetSaccoBackend

# Or Windows Service
Get-Service MinetSaccoBackend | Select-Object Name, Status, StartType

# Check process
Get-Process java | Where-Object {$_.CommandLine -like "*minet-sacco*"}
```

---

## Troubleshooting

### Issue 1: Backend Won't Start

**Check MySQL connection:**
```powershell
mysql -u minetsacco -p0a0b0c0D. minetsacco
```

**Check Java version:**
```powershell
java -version
# Should show Java 17
```

**Check port 9090 is available:**
```powershell
netstat -ano | findstr :9090
```

**View backend logs:**
```powershell
cd C:\minetsacco-deploy\backend
java -jar minet-sacco-backend-0.0.1-SNAPSHOT.jar
# Watch console output for errors
```

### Issue 2: Frontend Shows API Connection Errors

**Check backend is running:**
```powershell
curl http://localhost:9090/api/members
```

**Check CORS configuration:**
- Verify `CorsConfig.java` allows your IP range
- Current config allows: `10.39.*`, `10.0.*`

**Check browser console:**
- Open DevTools (F12)
- Look for CORS errors or 404s
- Verify API base URL is correct

### Issue 3: 404 on Frontend Routes

**Ensure web.config exists:**
```powershell
Test-Path "C:\inetpub\minetsacco\web.config"
```

**Install URL Rewrite module:**
- Download from: https://www.iis.net/downloads/microsoft/url-rewrite
- Install and restart IIS

**Restart IIS:**
```powershell
iisreset
```

### Issue 4: Login Fails

**Check JWT configuration:**
- Verify `jwt.secret` in application.properties
- Check token expiration (default: 30 minutes)

**Check database:**
```sql
USE minetsacco;
SELECT * FROM users LIMIT 5;
```

**Check backend logs:**
```powershell
Get-Content "C:\minetsacco-deploy\backend\logs\error.log" -Tail 100
```

---

## Rollback Plan

### If deployment fails:

1. **Stop services:**
   ```powershell
   nssm stop MinetSaccoBackend
   Stop-Website -Name "MinetSacco"
   ```

2. **Restore previous backend:**
   ```powershell
   Copy-Item "C:\minetsacco-deploy\backend\backup\*.jar" -Destination "C:\minetsacco-deploy\backend\" -Force
   ```

3. **Restore previous frontend:**
   ```powershell
   Remove-Item "C:\inetpub\minetsacco\*" -Recurse -Force
   Copy-Item "C:\minetsacco-deploy\frontend-backup\*" -Destination "C:\inetpub\minetsacco\" -Recurse -Force
   ```

4. **Restart services:**
   ```powershell
   nssm start MinetSaccoBackend
   Start-Website -Name "MinetSacco"
   ```

---

## Maintenance Commands

### Update Backend

```powershell
# Stop service
nssm stop MinetSaccoBackend

# Backup current JAR
Copy-Item "C:\minetsacco-deploy\backend\minet-sacco-backend-0.0.1-SNAPSHOT.jar" -Destination "C:\minetsacco-deploy\backend\backup\minet-sacco-backend-$(Get-Date -Format 'yyyyMMdd-HHmmss').jar"

# Copy new JAR
Copy-Item "path\to\new\minet-sacco-backend-0.0.1-SNAPSHOT.jar" -Destination "C:\minetsacco-deploy\backend\" -Force

# Start service
nssm start MinetSaccoBackend
```

### Update Frontend

```powershell
# Stop website
Stop-Website -Name "MinetSacco"

# Backup current frontend
Copy-Item "C:\inetpub\minetsacco" -Destination "C:\minetsacco-deploy\frontend-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')" -Recurse

# Deploy new frontend
Remove-Item "C:\inetpub\minetsacco\*" -Exclude "web.config" -Recurse -Force
Copy-Item "path\to\new\dist\*" -Destination "C:\inetpub\minetsacco\" -Recurse -Force

# Start website
Start-Website -Name "MinetSacco"
```

### Restart Services

```powershell
# Restart backend
nssm restart MinetSaccoBackend

# Restart IIS
iisreset

# Or restart specific site
Restart-WebAppPool -Name "MinetSaccoPool"
Stop-Website -Name "MinetSacco"
Start-Website -Name "MinetSacco"
```

---

## Access Information

### URLs

- **Frontend:** http://10.39.60.15:8090
- **Backend API:** http://10.39.60.15:9090/api
- **Health Check:** http://10.39.60.15:9090/actuator/health (if enabled)

### Default Ports

- **Backend:** 9090
- **Frontend:** 8090
- **MySQL:** 3306

### Configuration Locations

- **Backend Config:** `C:\minetsacco-deploy\backend\application.properties` (if extracted)
- **Frontend Config:** Built into JS bundle (api.ts)
- **IIS Config:** `C:\inetpub\minetsacco\web.config`
- **Service Config:** `HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\MinetSaccoBackend`

---

## Security Considerations

### Production Checklist

- [ ] Change JWT secret to a strong random value
- [ ] Use HTTPS instead of HTTP (requires SSL certificate)
- [ ] Change default database password
- [ ] Enable Spring Security CSRF protection
- [ ] Set secure session cookies
- [ ] Configure rate limiting
- [ ] Enable audit logging
- [ ] Regular database backups
- [ ] Monitor for security vulnerabilities
- [ ] Update dependencies regularly

### SSL/TLS Setup (Optional but Recommended)

**For IIS:**
1. Obtain SSL certificate
2. Install certificate in IIS
3. Add HTTPS binding on port 443
4. Update frontend to use HTTPS

**For Backend:**
Add to `application.properties`:
```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=your-password
server.ssl.key-store-type=PKCS12
```

---

## Support & Contact

**For deployment issues:**
1. Check logs first (backend and IIS)
2. Verify database connection
3. Test each component separately
4. Review this guide's troubleshooting section

**Critical files for support:**
- Backend logs: `C:\minetsacco-deploy\backend\logs\`
- IIS logs: `C:\inetpub\logs\LogFiles\`
- Application config: `application.properties`
- Database schema: `minetsacco` database

---

## Deployment Completion Checklist

- [ ] MySQL database `minetsacco` created
- [ ] Database user `minetsacco` created with correct permissions
- [ ] Backend JAR copied to server
- [ ] Backend running as Windows service
- [ ] Backend accessible at http://10.39.60.15:9090
- [ ] Frontend files copied to IIS directory
- [ ] IIS website configured and running
- [ ] Frontend accessible at http://10.39.60.15:8090
- [ ] Firewall rules configured
- [ ] Login tested successfully
- [ ] Treasurer features tested (edit/delete loans)
- [ ] Logs verified and accessible
- [ ] Backup plan documented

---

**Deployment Date:** _____________  
**Deployed By:** _____________  
**Server:** 10.39.60.15  
**Backend Version:** 0.0.1-SNAPSHOT  
**Frontend Version:** Latest (July 28, 2026)

**Status:** ✅ Ready for Production
