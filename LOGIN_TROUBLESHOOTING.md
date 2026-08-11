# Login Troubleshooting Guide

## Default Credentials

### Staff Login
```
Username: treasurer
Password: password
```

**All default users have the same password: `password`**

Available accounts:
- `admin` / `password` → ADMIN role
- `treasurer` / `password` → TREASURER role
- `loan_officer` / `password` → LOAN_OFFICER role
- `credit_committee` / `password` → CREDIT_COMMITTEE role

## Common Login Errors

### 1. "500 Internal Server Error"

**Symptom**: Error message says "An unexpected error occurred: Incorrect username or password"

**Possible Causes**:
1. Backend not running
2. Database connection failed
3. BCrypt configuration issue
4. User doesn't exist in database

**Solutions**:

#### A. Check Backend is Running
```powershell
# Check if backend is listening on port 9090
curl http://localhost:9090/api/health
```

**Expected**: 401 Unauthorized (means backend is up)
**Problem**: Connection refused or timeout (means backend is down)

**Fix**: Start backend
```powershell
cd backend
mvn spring-boot:run
```

#### B. Check Database Connection
Look at backend console logs for errors like:
- `Communications link failure`
- `Access denied for user`
- `Unknown database`

**Fix database connection**:
1. Open `backend/src/main/resources/application.properties`
2. Verify:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/tminet
   spring.datasource.username=tminet
   spring.datasource.password=0a0b0c0D.
   ```
3. Test database connection:
   ```powershell
   mysql -u tminet -p0a0b0c0D. tminet -e "SELECT 1;"
   ```

#### C. Verify User Exists
```powershell
# Check if treasurer user exists
mysql -u tminet "-p0a0b0c0D." tminet -e "SELECT username, role, enabled FROM users WHERE username='treasurer';"
```

**Expected output**:
```
username    role        enabled
treasurer   TREASURER   1
```

**If user doesn't exist**, run migrations:
```powershell
cd backend
mvn flyway:migrate
```

#### D. Check Backend Logs
Look at the backend console for specific errors:
- `BCryptPasswordEncoder` errors
- `AuthenticationException`
- Stack traces

### 2. "401 Unauthorized" (Expected for Wrong Password)

**Symptom**: Login fails with "Incorrect username or password"

**Solution**: Verify you're using the correct password: **`password`** (not "admin123" or other variants)

### 3. "Network Error" or "Cannot connect"

**Symptom**: Frontend shows "Network Error" or connection timeout

**Cause**: Backend not running or wrong URL

**Solutions**:

1. **Check backend is running**:
   ```powershell
   # Look for Java process
   Get-Process | Where-Object {$_.ProcessName -like "*java*"}
   ```

2. **Check API URL in frontend**:
   - Open `minetsacco-main/src/config/api.ts`
   - Verify: `export const API_BASE_URL = "http://localhost:9090/api";`

3. **Start backend**:
   ```powershell
   cd backend
   mvn spring-boot:run
   ```

4. **Wait for backend to fully start** (look for "Started SaccoApplication" in logs)

### 4. CORS Errors

**Symptom**: Browser console shows CORS policy error

**Cause**: Backend CORS configuration doesn't allow frontend origin

**Solution**:
1. Check `backend/src/main/java/com/minet/sacco/config/CorsConfig.java`
2. Verify `http://localhost:3000` is in allowed origins
3. Restart backend after changes

### 5. "User account is disabled"

**Symptom**: Login fails with "User account is disabled"

**Cause**: User's `enabled` field is 0 in database

**Solution**:
```sql
UPDATE users SET enabled = 1 WHERE username = 'treasurer';
```

## Testing Login Flow

### Step-by-Step Test

1. **Backend Health Check**:
   ```powershell
   curl http://localhost:9090/api/health
   ```
   Expected: 401 Unauthorized (good!) or authentication prompt

2. **Database Check**:
   ```powershell
   mysql -u tminet "-p0a0b0c0D." tminet -e "SELECT username, enabled FROM users WHERE username='treasurer';"
   ```
   Expected: treasurer user with enabled=1

3. **Frontend Check**:
   - Open http://localhost:3000
   - Check browser console (F12) for errors
   - No CORS errors should appear

4. **Login Attempt**:
   - Username: `treasurer`
   - Password: `password`
   - Click Login
   - Check browser Network tab (F12) for API call
   - Look at `/api/auth/login` request

5. **Check Response**:
   - Status 200: Success → should redirect to dashboard
   - Status 401: Wrong password
   - Status 500: Backend error (check backend logs)

## Manual Password Reset (If Needed)

If you need to reset the treasurer password to a known value:

### Option 1: Using BCrypt Generator
1. Generate BCrypt hash for "password":
   ```
   $2a$10$N9qo8uLOickgx2nYAUpLUeRxr7D3s6x3NYxvl2SzLTYuL3Mm9cYOa
   ```

2. Update database:
   ```sql
   UPDATE users 
   SET password = '$2a$10$N9qo8uLOickgx2nYAUpLUeRxr7D3s6x3NYxvl2SzLTYuL3Mm9cYOa' 
   WHERE username = 'treasurer';
   ```

### Option 2: Re-run Migration
```powershell
cd backend
mvn flyway:clean
mvn flyway:migrate
```
**⚠️ WARNING**: This will delete ALL data and reset to initial state!

## Debugging Checklist

- [ ] Backend running (Java process active)
- [ ] Backend accessible (curl returns 401)
- [ ] Database accessible (mysql command works)
- [ ] User exists in database (SELECT query returns row)
- [ ] User is enabled (enabled=1)
- [ ] Frontend running (npm dev server active)
- [ ] API URL correct in frontend config
- [ ] No CORS errors in browser console
- [ ] Network tab shows `/api/auth/login` POST request
- [ ] Request body contains username and password

## Still Having Issues?

1. **Check backend console logs** - Look for exception stack traces
2. **Check browser console** - Look for JavaScript errors or failed requests
3. **Check Network tab** - Look at actual request/response data
4. **Restart everything**:
   ```powershell
   # Stop backend (Ctrl+C)
   # Stop frontend (Ctrl+C)
   
   # Start backend
   cd backend
   mvn spring-boot:run
   
   # In new terminal, start frontend
   cd minetsacco-main
   npm run dev
   ```

## Quick Commands Reference

```powershell
# Start backend
cd backend
mvn spring-boot:run

# Start frontend
cd minetsacco-main
npm run dev

# Check MySQL users
mysql -u tminet "-p0a0b0c0D." tminet -e "SELECT * FROM users;"

# Test backend health
curl http://localhost:9090/api/health

# View backend logs
# Look at console where mvn spring-boot:run is running
```

## Default Credentials Summary

| Username          | Password   | Role            |
|-------------------|------------|-----------------|
| admin             | password   | ADMIN           |
| treasurer         | password   | TREASURER       |
| loan_officer      | password   | LOAN_OFFICER    |
| credit_committee  | password   | CREDIT_COMMITTEE|

**All passwords are**: `password` (lowercase, no special characters)

---

**Document Version**: 1.0  
**Last Updated**: 2026-07-28
