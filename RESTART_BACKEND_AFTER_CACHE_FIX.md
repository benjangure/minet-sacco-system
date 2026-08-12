# Restart Backend After Cache Fix

## Issue Fixed
Added missing cache `loansByStatusList` to CacheConfig.java

## Error Message (Now Fixed)
```
Cannot find cache named 'loansByStatusList'
```

## How to Apply the Fix

### Option 1: Local Development
If running locally:
```powershell
# Stop current backend (Ctrl+C in the terminal where it's running)

# Start again
cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend
java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar
```

### Option 2: Production Server (10.39.60.15)

#### Step 1: Copy new JAR to production
```powershell
.\COPY-JAR-TO-PRODUCTION.ps1
```

#### Step 2: Restart backend service on production server
```powershell
# On production server (10.39.60.15)
cd C:\Users\WakaeA\Downloads\nssm-2.24\win64

# Stop the service
.\nssm.exe stop MinetSaccoBackend

# Wait 5 seconds
Start-Sleep -Seconds 5

# Start the service
.\nssm.exe start MinetSaccoBackend

# Wait 20 seconds for startup
Start-Sleep -Seconds 20

# Verify it's running
netstat -ano | findstr :9090
```

## Verify Fix
After restart, test the loans page:
1. Login as treasurer
2. Go to Loans page
3. Should load without cache errors ✓

## What Was Changed
- **File:** `backend/src/main/java/com/minet/sacco/config/CacheConfig.java`
- **Change:** Added `new ConcurrentMapCache("loansByStatusList")` to cache manager
- **Commit:** dbd5b73

## Tobias Test Loan
Also deleted test loan LN-2026-00166 for Tobias Mugendi (employee 7139)
- Loan deleted ✓
- Tobias remains a member ✓
