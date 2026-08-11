# Local Development Guide

This guide shows you how to run the Minet SACCO System on your local machine (localhost) for development and testing.

---

## Environment Configuration

The system now supports **two profiles**:

| Profile | Database | User | Password | Use Case |
|---------|----------|------|----------|----------|
| **dev** | tminet | tminet | 0a0b0c0D. | Local development/testing |
| **prod** | minetsacco | minetsacco | 0a0b0c0D. | Production server (10.39.60.15) |

---

## Step 1: Setup Local MySQL Database

### Option A: Run SQL Script (Recommended)

**Open MySQL as root:**
```powershell
mysql -u root -p
```

**Run the setup script:**
```sql
source C:/Users/Lenovo/Desktop/minet-sacco/minet-sacco-system/backend/setup-dev-database.sql
```

Or paste the commands directly:
```sql
CREATE DATABASE IF NOT EXISTS tminet 
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP USER IF EXISTS 'tminet'@'localhost';
CREATE USER 'tminet'@'localhost' IDENTIFIED BY '0a0b0c0D.';

GRANT ALL PRIVILEGES ON tminet.* TO 'tminet'@'localhost';
FLUSH PRIVILEGES;
```

### Option B: Command Line

```powershell
mysql -u root -p < backend\setup-dev-database.sql
```

### Verify Setup

**Test connection:**
```powershell
mysql -u tminet -p0a0b0c0D. tminet
```

**Expected output:**
```
Welcome to the MySQL monitor...
mysql> 
```

**Check database:**
```sql
SHOW DATABASES;
USE tminet;
SHOW TABLES;
```

---

## Step 2: Run Backend (Localhost)

### Method 1: Using Maven (Development)

**Navigate to backend directory:**
```powershell
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend
```

**Run with DEV profile (default):**
```powershell
./mvnw.cmd spring-boot:run
```

The backend will automatically use the **dev** profile and connect to the **tminet** database.

**Or explicitly specify dev profile:**
```powershell
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
```

**Expected output:**
```
...
Started MinetSaccoBackendApplication in X.XXX seconds
...
Tomcat started on port(s): 9090 (http)
```

### Method 2: Using JAR File

**Build the JAR:**
```powershell
./mvnw.cmd clean package -DskipTests
```

**Run with dev profile:**
```powershell
java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Method 3: From IDE (IntelliJ/Eclipse)

**IntelliJ IDEA:**
1. Open `MinetSaccoBackendApplication.java`
2. Click ▶ Run
3. Or edit Run Configuration:
   - VM Options: `-Dspring.profiles.active=dev`
   - Or Environment Variables: `SPRING_PROFILES_ACTIVE=dev`

**Eclipse/STS:**
1. Right-click project → Run As → Spring Boot App
2. Run Configurations → Arguments → Program Arguments:
   ```
   --spring.profiles.active=dev
   ```

### Verify Backend is Running

**Test API:**
```powershell
curl http://localhost:9090/api/members
```

**Or open in browser:**
```
http://localhost:9090/api/members
```

**Expected:** JSON response or 401 Unauthorized (API is running)

---

## Step 3: Run Frontend (Localhost)

**Navigate to frontend directory:**
```powershell
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\minetsacco-main
```

**Install dependencies (first time only):**
```powershell
npm install
```

**Run development server:**
```powershell
npm run dev
```

**Expected output:**
```
VITE v8.0.1  ready in XXX ms

➜  Local:   http://localhost:5173/
➜  Network: use --host to expose
```

**Open in browser:**
```
http://localhost:5173
```

The frontend is already configured to connect to `http://localhost:9090/api` in development mode.

---

## Configuration Details

### Backend Configuration

**Dev Profile (application-dev.properties):**
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/tminet
spring.datasource.username=tminet
spring.datasource.password=0a0b0c0D.

# Server
server.port=9090

# Logging (Verbose for development)
logging.level.com.minet.sacco=DEBUG
spring.jpa.show-sql=true

# M-Pesa (Sandbox with localhost callbacks)
mpesa.callback-url=http://localhost:9090/api/mpesa/callback/stk
```

**Prod Profile (application-prod.properties):**
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/minetsacco
spring.datasource.username=minetsacco
spring.datasource.password=0a0b0c0D.

# Server
server.port=9090

# Logging (Less verbose)
logging.level.com.minet.sacco=INFO
spring.jpa.show-sql=false

# M-Pesa (Production with server callbacks)
mpesa.callback-url=http://10.39.60.15:9090/api/mpesa/callback/stk
```

### Frontend Configuration

**Dev Mode (api.ts):**
```typescript
// Automatically uses localhost:9090 when running on localhost
export const getBackendUrl = (): string => {
  if (!Capacitor.isNativePlatform()) {
    return import.meta.env.VITE_API_URL || 'http://localhost:9090';
  }
  // ...
}
```

**Production Mode:**
```typescript
// Automatically uses server IP when accessed from 10.39.60.15
return 'http://10.39.60.15:9090';
```

---

## Switching Between Environments

### Switch to Development (Local)

**Backend:**
```powershell
# Using Maven
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev

# Using JAR
java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

**Database:** tminet  
**Backend URL:** http://localhost:9090

### Switch to Production (Server)

**Backend:**
```powershell
# Using Maven
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod

# Using JAR
java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

**Database:** minetsacco  
**Backend URL:** http://localhost:9090 (but expects minetsacco database)

---

## Database Schema & Data

### Initial Schema Setup

The system uses Flyway migrations located in:
```
backend/src/main/resources/db/migration/
```

**To apply migrations:**
1. Enable Flyway in `application-dev.properties`:
   ```properties
   spring.flyway.enabled=true
   ```

2. Restart backend - migrations will run automatically

3. Disable Flyway after first run:
   ```properties
   spring.flyway.enabled=false
   ```

### Copy Data from Production (Optional)

If you want to test with production data:

**On production server:**
```powershell
mysqldump -u minetsacco -p0a0b0c0D. minetsacco > minetsacco_backup.sql
```

**On your local machine:**
```powershell
mysql -u tminet -p0a0b0c0D. tminet < minetsacco_backup.sql
```

⚠️ **Warning:** This will overwrite your local data!

---

## Testing Treasurer Features Locally

### 1. Create Test User

**Login to MySQL:**
```sql
mysql -u tminet -p0a0b0c0D. tminet
```

**Create treasurer user:**
```sql
INSERT INTO users (username, password, first_name, last_name, email, role, enabled, created_at)
VALUES ('treasurer', '$2a$10$dummyHashedPassword', 'Test', 'Treasurer', 'treasurer@test.com', 'TREASURER', 1, NOW());
```

### 2. Create Test Member & Loan

```sql
-- Create test member
INSERT INTO members (member_number, first_name, last_name, email, phone, status, created_at)
VALUES ('TEST001', 'John', 'Doe', 'john@test.com', '0712345678', 'ACTIVE', NOW());

-- Create test loan
INSERT INTO loans (member_id, loan_number, amount, interest_rate, term_months, 
                   total_interest, total_repayable, monthly_repayment, 
                   outstanding_balance, status, application_date, created_at)
SELECT id, 'LN-TEST-001', 100000, 12, 12, 12000, 112000, 9333.33, 112000, 'APPROVED', NOW(), NOW()
FROM members WHERE member_number = 'TEST001';
```

### 3. Test Edit Loan

**API Call:**
```powershell
curl -X PUT "http://localhost:9090/api/loans/1/update-financials?principal=50000&outstandingBalance=50000&reason=Testing" `
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 4. Test Delete Loan

**API Call:**
```powershell
curl -X DELETE "http://localhost:9090/api/loans/1" `
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 5. Check Audit Logs

```sql
SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 10;
```

---

## Common Development Tasks

### Reset Database

```powershell
mysql -u tminet -p0a0b0c0D. -e "DROP DATABASE tminet; CREATE DATABASE tminet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

Then restart backend to reapply schema.

### Clear Cache

```powershell
./mvnw.cmd clean
```

### View Logs

Backend logs are printed to console. To save to file:

```powershell
./mvnw.cmd spring-boot:run > backend.log 2>&1
```

### Hot Reload

**Backend:** Use Spring Boot DevTools (restart on code changes)

**Frontend:** Vite automatically reloads on file changes

---

## Troubleshooting

### Issue: "Access denied for user 'tminet'@'localhost'"

**Solution:**
```sql
-- As root
mysql -u root -p

-- Verify user exists
SELECT User, Host FROM mysql.user WHERE User='tminet';

-- Recreate user
DROP USER IF EXISTS 'tminet'@'localhost';
CREATE USER 'tminet'@'localhost' IDENTIFIED BY '0a0b0c0D.';
GRANT ALL PRIVILEGES ON tminet.* TO 'tminet'@'localhost';
FLUSH PRIVILEGES;
```

### Issue: "Unknown database 'tminet'"

**Solution:**
```sql
CREATE DATABASE tminet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Or enable `createDatabaseIfNotExist=true` in connection URL (already enabled).

### Issue: "Port 9090 already in use"

**Find process:**
```powershell
netstat -ano | findstr :9090
```

**Kill process:**
```powershell
taskkill /PID <process_id> /F
```

### Issue: Frontend shows "Network Error"

**Check:**
1. Backend is running on port 9090
2. Test: `curl http://localhost:9090/api/members`
3. Check browser console for CORS errors
4. Verify `api.ts` configuration

### Issue: "Table doesn't exist"

**Solution:**
1. Enable Flyway: `spring.flyway.enabled=true`
2. Restart backend
3. Migrations will create tables
4. Disable Flyway: `spring.flyway.enabled=false`

---

## Development Workflow

### Typical Development Session

1. **Start MySQL** (if not running)
   ```powershell
   # Check if running
   Get-Service MySQL80
   
   # Start if needed
   Start-Service MySQL80
   ```

2. **Start Backend**
   ```powershell
   cd backend
   ./mvnw.cmd spring-boot:run
   ```

3. **Start Frontend** (new terminal)
   ```powershell
   cd minetsacco-main
   npm run dev
   ```

4. **Open Browser**
   ```
   http://localhost:5173
   ```

5. **Make Changes**
   - Backend: Save file → Spring Boot restarts automatically
   - Frontend: Save file → Vite reloads automatically

6. **Test Changes**
   - Use browser for UI testing
   - Use Postman/curl for API testing
   - Check console logs for errors

### Before Committing Code

1. **Test with dev profile:**
   ```powershell
   ./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
   ```

2. **Build successfully:**
   ```powershell
   ./mvnw.cmd clean package -DskipTests
   ```

3. **Test with prod profile:**
   ```powershell
   java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
   ```
   (Make sure minetsacco database exists for this test)

---

## Environment Variables (Optional)

Instead of profiles, you can also use environment variables:

**Windows PowerShell:**
```powershell
$env:SPRING_PROFILES_ACTIVE="dev"
./mvnw.cmd spring-boot:run
```

**Windows CMD:**
```cmd
set SPRING_PROFILES_ACTIVE=dev
mvnw.cmd spring-boot:run
```

---

## Quick Reference

### URLs

| Service | Development | Production |
|---------|------------|------------|
| Backend API | http://localhost:9090/api | http://10.39.60.15:9090/api |
| Frontend | http://localhost:5173 | http://10.39.60.15:8090 |
| MySQL | localhost:3306 | localhost:3306 (on server) |

### Database Credentials

| Environment | Database | User | Password |
|-------------|----------|------|----------|
| Development | tminet | tminet | 0a0b0c0D. |
| Production | minetsacco | minetsacco | 0a0b0c0D. |

### Commands Cheat Sheet

```powershell
# Backend
./mvnw.cmd spring-boot:run                          # Run with default (dev) profile
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev   # Run dev explicitly
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod  # Run prod
./mvnw.cmd clean package -DskipTests                # Build JAR

# Frontend
npm install                                          # Install dependencies
npm run dev                                          # Development server
npm run build                                        # Build for production

# MySQL
mysql -u tminet -p0a0b0c0D. tminet                  # Connect to dev database
mysql -u minetsacco -p0a0b0c0D. minetsacco          # Connect to prod database

# Check running processes
Get-Process java                                     # Find Java processes
netstat -ano | findstr :9090                        # Check port 9090
```

---

## Next Steps

1. ✅ Setup local database (tminet)
2. ✅ Run backend with dev profile
3. ✅ Run frontend dev server
4. ✅ Login and test features
5. ✅ Test treasurer loan management
6. ✅ Check audit logs
7. ✅ Make your changes
8. ✅ Test thoroughly
9. ✅ Build for production
10. ✅ Deploy to server with prod profile

---

**Happy Coding! 🚀**

For production deployment, see **DEPLOYMENT_GUIDE.md**  
For treasurer features, see **TREASURER_LOAN_MANAGEMENT.md**
