# Minet SACCO - Deployment and Configuration Guide

## Overview

This guide explains how to deploy and configure the Minet SACCO system for different environments and networks. The system consists of:

- **Backend**: Spring Boot API running on port 8080
- **Frontend (Web)**: React app running on port 3000
  - Staff Portal: `localhost:3000` and `localhost:3000/login`
  - Member Portal: `localhost:3000/member`
- **Mobile App (APK)**: Member portal for Android devices

---

## Part 1: Initial Setup (Same WiFi Network)

### Prerequisites

- Java 21 installed
- Node.js 18+ installed
- MySQL database running
- Android device on the same WiFi network as your development machine

### Step 1: Start the Backend

```bash
cd backend
mvn clean package -DskipTests
java -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar
```

Backend will run on: `http://localhost:8080`

### Step 2: Start the Frontend (Web)

```bash
cd minetsacco-main
npm install
npm run dev
```

Frontend will run on: `http://localhost:3000`

### Step 3: Access the Application

**Staff Portal:**
- URL: `http://localhost:3000/login`
- Default credentials: Check `backend/src/main/resources/db/migration/V2__Insert_initial_data.sql`

**Member Portal (Web):**
- URL: `http://localhost:3000/member`

**Member Portal (Mobile APK):**
- Install the APK on your Android device
- Ensure device is on the same WiFi network
- App will default to member login page
- Configure backend URL in Settings if needed

---

## Part 2: Deploying to Different WiFi Networks

### Problem

When you move to a different WiFi network (e.g., for presentations), the hardcoded IP address in the APK won't work. You need to:

1. Find your new computer's IP address on the new network
2. Update the backend URL in the mobile app
3. Optionally rebuild the APK with the new IP

### Solution A: Using Settings Page (Recommended - No Rebuild Needed)

**On the Mobile Device:**

1. Open the Minet SACCO app
2. Log in as a member
3. Go to **Settings** (gear icon in dashboard)
4. Enter the new backend URL:
   - Format: `http://NEW_IP_ADDRESS:8080`
   - Example: `http://192.168.1.50:8080`
5. Tap **Save**
6. App will reload with the new URL

**Finding Your Computer's IP Address:**

**Windows:**
```powershell
ipconfig
# Look for "IPv4 Address" under your WiFi adapter
# Example: 192.168.1.50
```

**Mac/Linux:**
```bash
ifconfig
# Look for "inet" address under your WiFi interface
# Example: 192.168.1.50
```

### Solution B: Rebuild APK with New IP (Optional)

If you want the APK to have the new IP hardcoded:

**Step 1: Update the default URL**

Edit `minetsacco-main/src/config/api.ts`:

```typescript
const DEFAULT_BACKEND_URL = 'http://192.168.1.50:8080'; // Change this IP
```

**Step 2: Rebuild the APK**

```bash
cd minetsacco-main
npm run build
npx cap sync android
cd android
./gradlew.bat clean assembleDebug
```

**Step 3: Install the new APK**

```bash
# APK location: android/app/build/outputs/apk/debug/app-debug.apk
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Part 3: Network Configuration Checklist

### Before Deploying to a New Network

- [ ] Backend server is running on the new network
- [ ] Backend is accessible from your phone's IP
- [ ] Phone and computer are on the same WiFi network
- [ ] Firewall allows port 8080 traffic
- [ ] Database is accessible to the backend

### Testing Backend Connectivity

From your phone's browser, try accessing:
```
http://YOUR_COMPUTER_IP:8080/api/auth/login
```

You should see a response (even if it's an error). If you get "Connection refused" or timeout, the backend is not accessible.

---

## Part 4: File Structure and Configuration Files

### Key Files to Modify for Different Networks

| File | Purpose | Change When |
|------|---------|-------------|
| `minetsacco-main/src/config/api.ts` | Default backend URL | Rebuilding APK with new IP |
| `minetsacco-main/capacitor.config.ts` | Capacitor settings | Rarely needed (already set to allow all domains) |
| `backend/src/main/resources/application.properties` | Backend config | Changing database or port |

### Configuration Hierarchy

1. **Hardcoded Default** (in `src/config/api.ts`)
   - Used on first app launch
   - Default: `http://192.168.0.195:8080`

2. **LocalStorage** (saved in app)
   - Used after user configures in Settings
   - Persists across app restarts
   - Can be changed anytime in Settings page

---

## Part 5: Troubleshooting

### Issue: "Failed to connect to backend"

**Solution:**
1. Verify backend is running: `http://YOUR_IP:8080/api/auth/login`
2. Check phone is on same WiFi network
3. Check firewall allows port 8080
4. Update backend URL in app Settings

### Issue: "Connection refused"

**Solution:**
1. Backend service crashed - restart it
2. Wrong IP address - verify with `ipconfig` (Windows) or `ifconfig` (Mac/Linux)
3. Phone on different network - connect to correct WiFi

### Issue: "Timeout"

**Solution:**
1. Firewall blocking port 8080 - disable or add exception
2. Backend not listening on all interfaces - check `application.properties`
3. Network connectivity issue - test with `ping YOUR_IP`

### Issue: "Settings page not appearing"

**Solution:**
1. Rebuild the APK with latest code
2. Clear app cache: Settings > Apps > Minet SACCO > Storage > Clear Cache
3. Reinstall the APK

---

## Part 6: Quick Reference

### Default Credentials

Check `backend/src/main/resources/db/migration/V2__Insert_initial_data.sql` for:
- Staff login credentials
- Member login credentials
- Test data

### API Endpoints

All endpoints are prefixed with `/api`:

**Member Portal:**
- `POST /member/login` - Member login
- `GET /member/dashboard` - Dashboard data
- `GET /member/loans` - Member's loans
- `POST /member/request-loan-repayment` - Submit repayment request
- `GET /member/notifications` - Get notifications

**Staff Portal:**
- `POST /auth/login` - Staff login
- `GET /teller/loan-repayments/pending` - Pending repayment requests
- `POST /teller/loan-repayments/{id}/approve` - Approve repayment
- `POST /teller/loan-repayments/{id}/reject` - Reject repayment

### Database

- **Name:** `sacco_db`
- **User:** `root`
- **Password:** Check `.env` file
- **Port:** 3306 (default MySQL)

---

## Part 7: Deployment Workflow for Presentations

### Before the Presentation

1. **Test on your network:**
   ```bash
   # Start backend
   cd backend
   mvn clean package -DskipTests
   java -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar
   
   # Start frontend
   cd minetsacco-main
   npm run dev
   ```

2. **Test on mobile:**
   - Install APK on device
   - Verify all features work
   - Test loan repayment workflow

### At the Presentation Venue

1. **Connect to venue WiFi:**
   - Phone and laptop on same network

2. **Find your new IP:**
   ```powershell
   ipconfig
   ```

3. **Start backend:**
   ```bash
   cd backend
   java -jar target/minet-sacco-backend-0.0.1-SNAPSHOT.jar
   ```

4. **Update mobile app:**
   - Open Settings in app
   - Enter new backend URL: `http://NEW_IP:8080`
   - Save and reload

5. **Demo:**
   - Access staff portal: `http://localhost:3000/login`
   - Access member portal on phone: App will use configured URL

### Troubleshooting During Presentation

- **Backend not responding:** Restart backend service
- **Mobile app can't connect:** Check Settings > Backend URL
- **Firewall blocking:** Disable firewall or add port 8080 exception
- **Wrong IP:** Verify with `ipconfig` again

---

## Part 8: Summary

### Key Points

✅ **Same Network:** Works out of the box with default IP  
✅ **Different Network:** Use Settings page to update URL (no rebuild needed)  
✅ **Hardcoded IP:** Edit `src/config/api.ts` and rebuild if preferred  
✅ **Flexible:** App allows any backend URL via Settings  
✅ **Mobile First:** APK defaults to member portal (`/member`)  

### Files Modified in This Update

- `minetsacco-main/src/App.tsx` - Default route changed to `/member`
- `minetsacco-main/src/config/api.ts` - New configuration file
- `minetsacco-main/src/pages/MemberSettings.tsx` - New settings page
- `minetsacco-main/capacitor.config.ts` - Allow all domains

### Next Steps

1. Rebuild the APK with these changes
2. Test on mobile device
3. Verify Settings page works
4. Test changing backend URL in Settings
5. Deploy to GitHub

---

## Support

For issues or questions, refer to:
- Backend logs: `backend/logs/`
- Frontend console: Browser DevTools (F12)
- Mobile app logs: Android Studio Logcat
