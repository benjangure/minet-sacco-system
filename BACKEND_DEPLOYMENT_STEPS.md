# Backend Deployment to Production (10.39.60.15)

## ✅ Backend Rebuilt Successfully
- **Profile:** Production (minetsacco database)
- **JAR Location:** `C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar`
- **JAR Size:** ~104 MB
- **Configuration:** Embedded production profile with correct database credentials

---

## 🚀 Deployment Steps

### Step 1: Copy JAR to Production Server

**Option A: Network Share (if accessible)**
```powershell
# Copy JAR via network share
Copy-Item "C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target\minet-sacco-backend-0.0.1-SNAPSHOT.jar" -Destination "\\10.39.60.15\c$\minetsacco-deploy\backend\target\" -Force
```

**Option B: Manual Copy**
1. Open File Explorer
2. Navigate to: `C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\backend\target\`
3. Copy file: `minet-sacco-backend-0.0.1-SNAPSHOT.jar`
4. Paste to production server at: `C:\minetsacco-deploy\backend\target\`

---

### Step 2: On Production Server (10.39.60.15)

Open PowerShell as Administrator and run:

```powershell
# Navigate to NSSM directory
cd C:\Users\WakaeA\Downloads\nssm-2.24\win64

# Stop the service
.\nssm.exe stop MinetSaccoBackend

# Verify it's stopped
Get-Service -Name "MinetSaccoBackend"

# Wait a moment
Start-Sleep -Seconds 5

# Start the service
.\nssm.exe start MinetSaccoBackend

# Wait for startup (20 seconds)
Start-Sleep -Seconds 20

# Check if port 9090 is listening
netstat -ano | findstr :9090
```

**Expected Output:**
```
TCP    0.0.0.0:9090           0.0.0.0:0              LISTENING       12345
```

---

### Step 3: Verify Backend is Running

```powershell
# Check service status
Get-Service -Name "MinetSaccoBackend"
# Should show: Running

# Test the backend health endpoint
Invoke-WebRequest -Uri "http://localhost:9090/actuator/health" -UseBasicParsing
# Should return: {"status":"UP"}

# Check logs
Get-Content "C:\minetsacco-deploy\backend\logs\stdout.log" -Tail 30
```

---

### Step 4: Test from Frontend

Open browser and navigate to:
```
http://10.39.60.15:9090
```

Login to member portal and verify everything works.

---

## 🔍 Troubleshooting

### If Service Won't Start

Check the logs:
```powershell
Get-Content "C:\minetsacco-deploy\backend\logs\stdout.log" -Tail 100
Get-Content "C:\minetsacco-deploy\backend\logs\stderr.log" -Tail 100
```

### If Port 9090 Not Listening

Test manual startup:
```powershell
cd C:\minetsacco-deploy\backend\target
& "C:\Program Files\Java\jdk-17\bin\java.exe" -jar minet-sacco-backend-0.0.1-SNAPSHOT.jar
```

Watch the output for errors. Press `Ctrl+C` to stop.

### Common Issues

1. **Database Connection Error**
   - Verify MariaDB is running: `Get-Service -Name "MariaDB"`
   - Check database credentials in production

2. **Port Already in Use**
   - Find what's using port 9090: `netstat -ano | findstr :9090`
   - Stop the conflicting process

3. **Java Not Found**
   - Verify Java installation: `java -version`
   - Should show: Java 17 or higher

---

## 📋 Configuration Summary

**Production Profile Active:** Yes (default)

**Database Connection:**
- URL: `jdbc:mysql://localhost:3306/minetsacco`
- Username: `minetsacco`
- Password: `0a0b0c0D.`
- Driver: `com.mysql.cj.jdbc.Driver`

**Flyway:**
- Enabled: `true`
- Baseline on migrate: `true`
- Out of order: `true`

**Server:**
- Port: `9090`
- Address: `0.0.0.0` (listens on all interfaces)

**Push Notifications:**
- VAPID keys embedded
- Ready for PWA notifications

---

## ✅ Success Checklist

- [ ] JAR copied to production server
- [ ] Service stopped
- [ ] Service started
- [ ] Port 9090 listening
- [ ] Health endpoint responds
- [ ] Frontend can connect
- [ ] Login works
- [ ] No errors in logs

---

## 🎯 Next Steps After Backend is Running

1. **Test PWA Features:**
   - Open member portal on mobile
   - Click "Add to Home Screen"
   - Enable notifications
   - Test push notifications from admin panel

2. **Monitor Logs:**
   - Check for any startup warnings
   - Verify database migrations ran successfully
   - Monitor for connection pool issues

3. **Performance Check:**
   - Test loan calculations
   - Verify reports generation
   - Check guarantor functionality

---

## 📞 Need Help?

If the backend still won't start:
1. Share the full logs from `stdout.log`
2. Share output of manual JAR run
3. Share any error messages from Windows Event Viewer
