# Minet SACCO Deployment Guide

## System Configuration

### Ports
- **Frontend (IIS):** Port 8090 - `http://localhost:8090`
- **Backend (Java):** Port 9090 - `http://localhost:9090`

### Database
- **Name:** `minetsacco`
- **User:** `minetsacco`
- **Password:** `0a0b0c0D.`
- **Host:** `localhost`
- **Port:** 3306 (MySQL default)

---

## Changes Made for Treasurer Loan Edit/Delete

### 1. Backend Changes (✅ Complete)

**File:** `backend/src/main/java/com/minet/sacco/controller/LoanController.java`
- Added `DELETE /{loanId}` endpoint (ROLE_TREASURER only)
- Added `PUT /{loanId}/update-financials` endpoint (ROLE_TREASURER only)

**File:** `backend/src/main/java/com/minet/sacco/service/LoanService.java`
- Added `deleteLoan()` method with cleanup logic
- Added `updateLoanFinancials()` method

### 2. Frontend Changes (✅ Complete)

**File:** `minetsacco-main/src/pages/Loans.tsx`
- Added Edit/Delete buttons (treasurer role only)
- Enhanced delete error notification UI (multi-line, 10sec duration, helpful alternatives)

**File:** `minetsacco-main/.env`
- Changed `VITE_API_URL` from port 9090 to port 9090

**File:** `minetsacco-main/src/config/api.ts`
- Default backend URL set to `http://localhost:9090`

---

## Local Build Instructions

### Build Backend

```powershell
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend
mvn clean package -DskipTests
```

**Output:** `backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar`

### Build Frontend

```powershell
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\minetsacco-main

# Clean old build
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue

# Build
npm run build
```

**Output:** `minetsacco-main\dist\` folder with all files

---

## Server Deployment

### Prerequisites on Server
- MySQL 8.0.46 running
- Java 21 installed
- IIS installed and configured
- Database `minetsacco` created
- MySQL user `minetsacco` with password `0a0b0c0D.`

### Server Paths
- **Backend JAR:** `C:\minetsacco-deploy\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar`
- **Frontend (IIS):** `C:\inetpub\minetsacco\`

---

## Deployment Steps

### Step 1: Deploy Backend

1. **Copy JAR to server:**
   - From: `C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar`
   - To: `C:\minetsacco-deploy\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar`

2. **Start backend manually:**

```powershell
cd C:\minetsacco-deploy\backend
java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.datasource.username=minetsacco --spring.datasource.password=0a0b0c0D. --spring.datasource.url=jdbc:mysql://localhost:3306/minetsacco?createDatabaseIfNotExist=true
```

3. **Verify backend is running:**
   - Check console for "Started MinetSaccoBackendApplication"
   - Test: `http://localhost:9090/api/auth/login` should respond (not 503)

### Step 2: Deploy Frontend

1. **On server, stop IIS:**

```powershell
iisreset /stop
```

2. **Delete old frontend files:**

```powershell
Remove-Item -Recurse -Force "C:\inetpub\minetsacco\*"
```

3. **Copy new frontend from local machine:**
   - From: `C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\minetsacco-main\dist\*`
   - To: `C:\inetpub\minetsacco\`
   - **Copy ALL files and folders inside `dist\`**

4. **Start IIS:**

```powershell
iisreset /start
```

5. **Verify files copied:**

```powershell
Get-ChildItem "C:\inetpub\minetsacco\assets" | Select-Object Name, LastWriteTime
```

Should show today's date/time.

---

## Testing

### 1. Open Browser (Incognito/Private Mode)
Navigate to: `http://localhost:8090`

### 2. Login as Treasurer
- **Username:** `treasurer`
- **Password:** `password`

### 3. Navigate to Loans Page
You should see:
- ✏️ Edit button for each loan
- 🗑️ Delete button for each loan

### 4. Test Delete Functionality
Try deleting a loan with repayments - you should see a multi-line error notification:
```
Cannot delete loan: Loan has X repayment(s)

To proceed, you must first:
• Delete all loan repayments
• Or mark the loan as fully paid
• Or use the archive feature instead
```

### 5. Verify API Calls
Open Developer Tools (F12) → Network tab
- All API calls should go to `http://localhost:9090/api/...`
- **NOT** `http://localhost:9090/api/...`

---

## Troubleshooting

### Issue: Frontend still calling port 9090

**Cause:** Wrong files deployed or browser cache

**Solution:**
1. On server, verify files in `C:\inetpub\minetsacco\assets\index-*.js` contain `localhost:9090`:
   ```powershell
   Select-String -Path "C:\inetpub\minetsacco\assets\index-*.js" -Pattern "localhost:9090" | Select-Object -First 1
   ```

2. If it shows 9090, delete ALL files and re-copy from local build
3. Use incognito/private mode for testing
4. Clear localStorage in browser console:
   ```javascript
   localStorage.clear();
   location.reload();
   ```

### Issue: Backend database connection error

**Error:** `Access denied for user 'minetsacco'@'localhost' to database 'sacco_db'`

**Solution:** Use correct database name and credentials:
```powershell
java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.datasource.username=minetsacco --spring.datasource.password=0a0b0c0D. --spring.datasource.url=jdbc:mysql://localhost:3306/minetsacco?createDatabaseIfNotExist=true
```

### Issue: 404 error when refreshing pages

**Cause:** Missing or incorrect `web.config` in IIS directory

**Solution:** Ensure `web.config` exists in `C:\inetpub\minetsacco\web.config` with SPA routing rules

---

## Permanent Backend Service (Optional)

To run backend automatically on server startup:

1. **Create batch script:** `C:\minetsacco-deploy\backend\start-backend.bat`
   ```batch
   @echo off
   cd C:\minetsacco-deploy\backend
   java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar --spring.datasource.username=minetsacco --spring.datasource.password=0a0b0c0D. --spring.datasource.url=jdbc:mysql://localhost:3306/minetsacco?createDatabaseIfNotExist=true
   ```

2. **Update Task Scheduler** "MinetSaccoBackend" task to run this script

---

## Success Criteria

✅ Backend runs on port 9090 without errors
✅ Frontend loads on `http://localhost:8090`
✅ Login works (treasurer/password)
✅ Dashboard loads without CORS errors
✅ Loans page shows data
✅ Edit/Delete buttons visible (treasurer role only)
✅ Delete error shows helpful multi-line notification
✅ All API calls go to port 9090 (check Network tab)

---

## Modified Files Summary

### Backend
- `backend/src/main/java/com/minet/sacco/controller/LoanController.java`
- `backend/src/main/java/com/minet/sacco/service/LoanService.java`
- `backend/src/main/resources/application.properties` (port 9090)

### Frontend
- `minetsacco-main/src/pages/Loans.tsx` (Edit/Delete buttons + error UI)
- `minetsacco-main/src/config/api.ts` (port 9090)
- `minetsacco-main/.env` (VITE_API_URL port 9090)
- `minetsacco-main/public/web.config` (SPA routing for IIS)

---

## Contact
For issues during deployment, check:
1. Backend console for errors
2. Browser Developer Tools → Console for frontend errors
3. Browser Developer Tools → Network tab for API calls
