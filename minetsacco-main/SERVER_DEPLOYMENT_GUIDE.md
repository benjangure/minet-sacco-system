# Server Deployment Guide - Minet SACCO (Windows Server)

This guide walks you through deploying the Minet SACCO system to a Windows Server. You'll deploy three components: Database, Backend, and Frontend.

**Server Details:**
- IP: 10.39.60.15
- Username: wakaea
- Access: Remote Desktop (RDP)

**Security Notes:**
- Use strong passwords for all services (12+ characters, mixed case, numbers, symbols)
- Change default credentials immediately
- Enable Windows Firewall and restrict ports
- Keep all software updated
- Create regular database backups

---

## Phase 1: Database Setup (MySQL)

### Step 1: Download MySQL
1. Open browser on the server
2. Visit: https://dev.mysql.com/downloads/mysql/
3. Download **MySQL Community Server** (latest version)
4. Choose **Windows (x86, 64-bit) MSI Installer**

### Step 2: Install MySQL
1. Run the installer
2. Choose **Setup Type**: "Developer Default"
3. Follow the wizard
4. **MySQL Server Configuration:**
   - Port: `3306`
   - Windows Service Name: `MySQL80`
5. **MySQL Server User Configuration:**
   - Username: `root`
   - Password: Create a **strong password** (min 12 characters, mix of upper/lower/numbers/symbols)
   - Save this password securely

### Step 3: Verify MySQL Installation
Open Command Prompt and run:
```cmd
mysql -u root -p
```
Enter the root password. If you see `mysql>` prompt, MySQL is working.

### Step 4: Create Database and User
In the MySQL prompt, run:
```sql
CREATE DATABASE minetsacco;
CREATE USER 'sacco_user'@'localhost' IDENTIFIED BY 'YourStrongPassword123!';
GRANT ALL PRIVILEGES ON minetsacco.* TO 'sacco_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

**Important:** Replace `YourStrongPassword123!` with a strong password. Save it securely.

### Step 5: Verify Database
```cmd
mysql -u sacco_user -p minetsacco
```
Enter the password. If you see `mysql>` prompt, database is ready.

---

## Phase 2: Install Java

### Step 1: Download Java
1. Visit: https://www.oracle.com/java/technologies/downloads/
2. Download **Java 11 LTS** or **Java 17 LTS**
3. Choose **Windows x64 Installer**

### Step 2: Install Java
1. Run the installer
2. Accept defaults
3. Note the installation path (usually `C:\Program Files\Java\jdk-11.x.x`)

### Step 3: Verify Installation
Open Command Prompt and run:
```cmd
java -version
```

---

## Phase 3: Install Maven

### Step 1: Download Maven
1. Visit: https://maven.apache.org/download.cgi
2. Download **Binary zip archive** (apache-maven-3.x.x-bin.zip)

### Step 2: Extract Maven
1. Extract to: `C:\apache-maven-3.x.x`

### Step 3: Add Maven to PATH
1. Right-click **This PC** → **Properties**
2. Click **Advanced system settings**
3. Click **Environment Variables**
4. Under **System variables**, click **New**
   - Variable name: `MAVEN_HOME`
   - Variable value: `C:\apache-maven-3.x.x`
5. Find **Path** variable, click **Edit**
6. Click **New** and add: `%MAVEN_HOME%\bin`
7. Click **OK** three times

### Step 4: Verify Maven
Open **new** Command Prompt and run:
```cmd
mvn --version
```

---

## Phase 4: Transfer Code Files

### Option A: Network Share (Recommended)
1. On your **local machine**, right-click `minetsacco-main` folder
2. Select **Properties** → **Sharing** → **Share**
3. Share with your user
4. On the **server**, open **File Explorer**
5. Go to: `\\your-local-computer-ip\minetsacco-main`
6. Copy entire folder to: `C:\minetsacco`

### Option B: USB Drive
1. Copy `minetsacco-main` to USB
2. Plug USB into server
3. Copy to `C:\minetsacco`

---

## Phase 5: Configure and Build Backend

### Step 1: Navigate to Backend
```cmd
cd C:\minetsacco\backend
```

### Step 2: Edit Configuration
1. Open: `src\main\resources\application.properties`
2. Update with your database credentials:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/minetsacco
spring.datasource.username=sacco_user
spring.datasource.password=YourStrongPassword123!
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

server.port=9090
server.servlet.context-path=/api

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

### Step 3: Build Backend
```cmd
mvn clean package -DskipTests
```

This creates: `target\minetsacco-0.0.1-SNAPSHOT.jar`

### Step 4: Test Backend
```cmd
java -jar target\minetsacco-0.0.1-SNAPSHOT.jar
```

Wait for startup, then open browser and visit:
```
http://localhost:9090/api/auth/health
```

You should see: `{"status":"UP"}`

Press `Ctrl+C` to stop.

---

## Phase 6: Install Node.js

### Step 1: Download Node.js
1. Visit: https://nodejs.org/
2. Download **LTS version** (Windows Installer)

### Step 2: Install Node.js
1. Run installer
2. Accept defaults
3. Complete installation

### Step 3: Verify Installation
Open **new** Command Prompt and run:
```cmd
node --version
npm --version
```

---

## Phase 7: Configure and Build Frontend

### Step 1: Navigate to Frontend
```cmd
cd C:\minetsacco\minetsacco-main
```

### Step 2: Edit API Configuration
1. Open: `src\config\api.ts`
2. Update backend URL:
```typescript
const API_BASE_URL = "http://10.39.60.15:9090/api";
```

### Step 3: Install Dependencies
```cmd
npm install
```

### Step 4: Build Frontend
```cmd
npm run build
```

This creates: `dist\` folder

---

## Phase 8: Setup IIS (Internet Information Services)

### Step 1: Install IIS
1. Open **Server Manager**
2. Click **Add Roles and Features**
3. Select **Web Server (IIS)**
4. Complete installation
5. Restart if prompted

### Step 2: Create IIS Website
1. Open **Internet Information Services (IIS) Manager**
2. Right-click **Sites** → **Add Website**
3. Fill in:
   - Site name: `MinetSacco`
   - Physical path: `C:\minetsacco\minetsacco-main\dist`
   - Binding: `http`, Port: `80`
4. Click **OK**

### Step 3: Configure URL Rewrite (for React Router)
1. Select your website
2. Double-click **URL Rewrite**
3. Click **Add Rule** → **New Blank Rule**
4. Name: `React Router`
5. Pattern: `.*`
6. Action: Rewrite to `index.html`
7. Click **OK**

### Step 4: Setup API Proxy
1. Select your website
2. Double-click **Application Request Routing Cache**
3. Click **Server Proxy Settings**
4. Check **Enable proxy**
5. Click **OK**

Then create rewrite rule:
1. Double-click **URL Rewrite**
2. Click **Add Rule** → **Reverse Proxy**
3. Inbound pattern: `^api/(.*)`
4. Rewrite URL: `http://localhost:9090/api/{R:1}`
5. Click **OK**

---

## Phase 9: Run Backend as Windows Service

### Step 1: Create Batch File
Create: `C:\minetsacco\run-backend.bat`

```batch
@echo off
cd C:\minetsacco\backend
java -jar target\minetsacco-0.0.1-SNAPSHOT.jar
pause
```

### Step 2: Install as Service (Using NSSM)
1. Download NSSM: https://nssm.cc/download
2. Extract to: `C:\nssm`
3. Open Command Prompt as **Administrator**
4. Run:
```cmd
C:\nssm\nssm.exe install MinetSaccoBackend "C:\minetsacco\run-backend.bat"
C:\nssm\nssm.exe start MinetSaccoBackend
```

### Step 3: Verify Service
Open **Services** (services.msc) and look for **MinetSaccoBackend** - it should be running.

---

## Phase 10: Test Everything

### Test Backend
Open browser and visit:
```
http://10.39.60.15:9090/api/auth/health
```

### Test Frontend
Open browser and visit:
```
http://10.39.60.15
```

You should see the Minet SACCO login page.

### Test Login
Try logging in with:
- Username: `EMP001`
- Password: `12345678`

---

## Troubleshooting

### Backend won't start
```cmd
# Check if port 9090 is in use
netstat -ano | findstr :9090

# If in use, identify the process and stop it
tasklist /FI "PID eq <process_id>"
```

### Frontend shows blank page
1. Check IIS logs: `C:\inetpub\logs\LogFiles`
2. Rebuild frontend: `npm run build`
3. Restart IIS: `iisreset`

### Database connection failed
```cmd
# Test MySQL connection
mysql -u sacco_user -p minetsacco
# Enter your password
```

### Can't access from another computer
1. Open **Windows Firewall with Advanced Security**
2. Create inbound rules for ports: `80`, `9090`
3. Restrict to specific IP addresses if possible

---

## Security Checklist

- [ ] Changed all default passwords to strong passwords (12+ characters, mixed case, numbers, symbols)
- [ ] Enabled Windows Firewall
- [ ] Restricted database access to localhost only
- [ ] Restricted IIS to necessary ports only
- [ ] Disabled unnecessary Windows services
- [ ] Enabled Windows updates
- [ ] Created regular database backups
- [ ] Logged all access attempts
- [ ] Tested backup and restore procedures

---

## Quick Reference

**Access Points:**
- Frontend: `http://10.39.60.15`
- Backend API: `http://10.39.60.15:9090/api`
- Database: `localhost:3306`

**Key Folders:**
- Backend: `C:\minetsacco\backend`
- Frontend: `C:\minetsacco\minetsacco-main`
- IIS Website: `C:\minetsacco\minetsacco-main\dist`

**Important Commands:**
```cmd
# Start backend manually
cd C:\minetsacco\backend
java -jar target\minetsacco-0.0.1-SNAPSHOT.jar

# Restart IIS
iisreset

# View services
services.msc

# Access MySQL
mysql -u root -p
```

---

## Summary

Your Minet SACCO system is now deployed with:
- ✓ MySQL database running securely on port 3306
- ✓ Java backend running on port 9090
- ✓ IIS frontend running on port 80
- ✓ All three components communicating
- ✓ Security best practices implemented

**System is ready for production use.**
