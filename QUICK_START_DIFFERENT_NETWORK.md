# Quick Start: Using Minet SACCO on Different WiFi Networks

## TL;DR - For Presentations

### Before You Leave
1. Rebuild APK with latest code
2. Test everything works on your current network

### At the Venue
1. Connect laptop and phone to venue WiFi
2. Find your laptop's IP: `ipconfig` (Windows) or `ifconfig` (Mac/Linux)
3. Start backend: `java -jar backend/target/minet-sacco-backend-0.0.1-SNAPSHOT.jar`
4. On phone: Open Minet SACCO app → Settings → Enter new backend URL → Save
5. Demo!

---

## Step-by-Step Guide

### Step 1: Find Your Computer's IP Address

**Windows:**
```powershell
ipconfig
```
Look for "IPv4 Address" under your WiFi adapter. Example: `192.168.1.50`

**Mac/Linux:**
```bash
ifconfig
```
Look for "inet" address. Example: `192.168.1.50`

### Step 2: Start the Backend

```bash
cd backend
java -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar
```

Backend will run on: `http://YOUR_IP:8080`

### Step 3: Update Mobile App

**On your Android phone:**

1. Open the Minet SACCO app
2. Log in as a member (if not already logged in)
3. Tap the **Settings** icon (gear icon in dashboard)
4. Enter the backend URL:
   - Format: `http://YOUR_IP:8080`
   - Example: `http://192.168.1.50:8080`
5. Tap **Save**
6. App will reload with the new URL

### Step 4: Demo

- **Staff Portal (Web):** `http://localhost:3000/login`
- **Member Portal (Phone):** App will use the configured URL

---

## What Changed in This Update

### APK Now Defaults to Member Portal
- Previously: APK opened to staff login page
- Now: APK opens directly to member login page (`/member`)
- Staff can still access web portal at `http://localhost:3000/login`

### Configurable Backend URL
- **Before:** Had to rebuild APK for each network
- **Now:** Change URL in Settings without rebuilding
- URL is saved in phone's local storage
- Persists across app restarts

### Files Modified
- `minetsacco-main/src/App.tsx` - Default route changed to `/member`
- `minetsacco-main/src/config/api.ts` - New configuration system
- `minetsacco-main/src/pages/MemberSettings.tsx` - New settings page
- `minetsacco-main/capacitor.config.ts` - Allow all domains

---

## Troubleshooting

### "Connection refused" or "Can't reach backend"

**Check:**
1. Backend is running: `http://YOUR_IP:8080/api/auth/login` in browser
2. Phone is on same WiFi network
3. Firewall allows port 8080

**Fix:**
1. Verify IP address with `ipconfig`
2. Update URL in app Settings
3. Disable firewall or add port 8080 exception

### "Settings page not showing"

**Fix:**
1. Reinstall the APK
2. Clear app cache: Settings > Apps > Minet SACCO > Storage > Clear Cache

### "Backend URL not saving"

**Fix:**
1. Ensure URL starts with `http://` or `https://`
2. Ensure URL format is correct: `http://IP:PORT`
3. Try again

---

## File Locations

| Item | Location |
|------|----------|
| APK | `minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk` |
| Backend JAR | `backend/target/minet-sacco-backend-0.0.1-SNAPSHOT.jar` |
| Config File | `minetsacco-main/src/config/api.ts` |
| Settings Page | `minetsacco-main/src/pages/MemberSettings.tsx` |

---

## Default Values

| Item | Value |
|------|-------|
| Default Backend URL | `http://192.168.0.195:8080` |
| Backend Port | 8080 |
| Web Frontend Port | 3000 |
| Staff Portal | `http://localhost:3000/login` |
| Member Portal (Web) | `http://localhost:3000/member` |
| Member Portal (Mobile) | APK app |

---

## Complete Workflow for Presentations

```bash
# 1. Before leaving (at home/office)
cd minetsacco-main
npm run build
npx cap sync android
cd android
./gradlew.bat clean assembleDebug
# Test on phone - verify all features work

# 2. At the venue
# Connect to WiFi
# Find IP: ipconfig
# Start backend
cd backend
java -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar

# 3. On phone
# Open app → Settings → Enter http://YOUR_IP:8080 → Save
# Demo the features

# 4. Access portals
# Staff: http://localhost:3000/login (on laptop)
# Member: App on phone (uses configured URL)
```

---

## Key Points to Remember

✅ **Same Network:** Works out of the box  
✅ **Different Network:** Use Settings page (no rebuild needed)  
✅ **Hardcoded IP:** Edit `src/config/api.ts` if you want to rebuild  
✅ **Mobile First:** APK defaults to member portal  
✅ **Flexible:** Supports any backend URL via Settings  

---

## Support

For detailed information, see: `DEPLOYMENT_AND_CONFIGURATION_GUIDE.md`
