# APK Login Error - "Failed to fetch"

## Problem
When entering valid credentials on the APK, you get error: **"Failed to fetch"**

This means the phone cannot connect to the backend server.

## Root Cause

The APK is configured to connect to: `http://192.168.0.195:8080/api`

But one of these is true:
1. ❌ Backend server is NOT running
2. ❌ Phone is NOT on the same WiFi network
3. ❌ IP address `192.168.0.195` is WRONG for your backend

## Solution

### Step 1: Verify Backend is Running

**On your computer:**
```bash
# Check if backend is running
curl http://192.168.0.195:8080/api/auth/member/login
```

If you get a response, backend is running. If not, start it:
```bash
cd backend
java -jar target/minet-sacco-backend-1.0.0.jar
```

### Step 2: Verify Phone is on Same WiFi

**On your Samsung A14:**
1. Go to Settings → WiFi
2. Check connected WiFi network
3. Should be same network as your computer

### Step 3: Verify IP Address

**Find your computer's IP address:**

**Windows:**
```bash
ipconfig
```
Look for "IPv4 Address" under your WiFi adapter (usually starts with 192.168.x.x)

**If IP is different from 192.168.0.195:**

You need to rebuild the APK with the correct IP.

## Rebuild APK with Correct IP

### Step 1: Update .env file

**File:** `minetsacco-main/.env`

Change:
```
VITE_API_URL="http://192.168.0.195:8080/api"
```

To your actual IP (example):
```
VITE_API_URL="http://192.168.1.100:8080/api"
```

### Step 2: Rebuild Web App
```bash
cd minetsacco-main
npm run build
```

### Step 3: Rebuild APK
```bash
cd android
.\gradlew.bat clean assembleDebug
```

### Step 4: Transfer New APK
- Uninstall old APK from phone
- Transfer new APK
- Install new APK
- Try login again

## Quick Checklist

Before trying login again:

- [ ] Backend is running (check with curl or visit http://192.168.0.195:8080 in browser)
- [ ] Phone is on same WiFi as computer
- [ ] IP address in .env matches your computer's IP
- [ ] APK was rebuilt after changing IP
- [ ] Old APK was uninstalled before installing new one

## Test Connection from Phone

**On your Samsung A14:**

1. Open browser
2. Go to: `http://192.168.0.195:8080/api/auth/member/login`
3. Should show error page (not "Cannot reach server")
4. If you see error page, connection works
5. If you see "Cannot reach server", IP is wrong

## Common IP Addresses

| Network | IP Range |
|---------|----------|
| Home WiFi | 192.168.0.x or 192.168.1.x |
| Office WiFi | 10.0.0.x or 172.16.x.x |
| Mobile Hotspot | 192.168.43.x |

## If Still Not Working

1. **Check firewall**
   - Windows Firewall might be blocking port 8080
   - Go to Windows Defender Firewall → Allow app through firewall
   - Add Java to allowed apps

2. **Check backend logs**
   - Look for errors in backend console
   - Check if backend is listening on correct port

3. **Try localhost on computer**
   - Open browser on computer
   - Go to `http://localhost:8080/api/auth/member/login`
   - Should work if backend is running

4. **Restart everything**
   - Stop backend
   - Stop APK
   - Restart backend
   - Restart APK
   - Try login again

## Correct Login Flow

Once connection is working:

1. Open APK
2. Enter username: `member1`
3. Enter password: `password`
4. Click "Sign In"
5. Should see dashboard (not error)

## After Fixing

Once login works:
- Test deposit request
- Test M-Pesa integration
- Test all features

---

**Need help?** Check the troubleshooting section above or verify each step in the checklist.
