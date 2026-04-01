# APK Rebuild Instructions - Mobile Download Fixes

## Quick Start (Recommended)

### Step 1: Open PowerShell in the minetsacco-main directory
```powershell
cd C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main
```

### Step 2: Run the build script
```powershell
.\build-apk.ps1
```

This will automatically:
1. Build the web app with all latest changes
2. Sync to Android
3. Build the APK
4. Show you the APK location

**Estimated time**: 5-10 minutes (first build may take longer due to Gradle download)

---

## Manual Step-by-Step (If script fails)

### Step 1: Build the web app
```powershell
cd C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main
npm run build
```
Wait for completion. You should see: `✓ built in XXXms`

### Step 2: Sync to Android
```powershell
npx cap sync android
```
This copies the built web app to the Android project.

### Step 3: Build the APK
```powershell
cd android
.\gradlew.bat assembleRelease
cd ..
```

This creates the APK file. Wait for: `BUILD SUCCESSFUL`

---

## After Build Completes

### APK Location
```
C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main\android\app\build\outputs\apk\release\app-release.apk
```

### Install on Phone

1. **Connect phone to PC** via USB cable
2. **Transfer APK to phone**:
   ```powershell
   # Copy to phone's Downloads folder
   adb push "android\app\build\outputs\apk\release\app-release.apk" /sdcard/Download/
   ```
   
   OR manually copy via file explorer

3. **On phone**:
   - Open Files app
   - Navigate to Downloads
   - Tap the APK file
   - Tap "Install"
   - If prompted about unknown sources, tap "Install anyway"

4. **Launch the app** from home screen

---

## What's New in This Build

✅ **Mobile Downloads Fixed**
- Deposit receipts now open in browser tab on mobile
- KYC documents now open in browser tab on mobile
- Desktop downloads still work normally

✅ **Audit Trail Improvements**
- No more duplicate logs
- Added eye icon to view full details
- All actions now logged (deposits, loans, members, KYC docs)

✅ **All Previous Features**
- Member portal login
- Deposit requests
- Loan applications
- KYC document uploads
- Home button navigation
- CORS configuration for dev server

---

## Troubleshooting

### If npm run build fails
```powershell
# Clear cache and try again
npm cache clean --force
npm install
npm run build
```

### If Gradle build fails
```powershell
cd android
.\gradlew.bat clean
.\gradlew.bat assembleRelease
cd ..
```

### If adb push fails
- Install Android SDK Platform Tools
- Or manually copy APK via USB file transfer

### If APK won't install
- Uninstall old version first
- Enable "Unknown Sources" in Settings > Security
- Try installing again

---

## Testing the New APK

### On Samsung A14 Phone

1. **Login** with member credentials
2. **Test Deposit Receipt Download**:
   - Go to Savings section
   - Find a deposit request
   - Click download receipt
   - ✅ Should open in browser tab (not download)
   - Can view, save, or share from browser

3. **Test KYC Document Download**:
   - Go to Documents section
   - Click view/download document
   - ✅ Should open in browser tab (not download)
   - Can view, save, or share from browser

4. **Test Audit Trail** (if you have admin access):
   - Go to Audit Trail
   - Should see all actions (no duplicates)
   - Click eye icon to view full details

---

## Version Info

- **Frontend**: React + TypeScript
- **Mobile Framework**: Capacitor
- **Android Target**: API 24+
- **APK Type**: Release (optimized, smaller size)

---

## Next Steps After Installation

1. Test all download features on the phone
2. Verify audit trail shows correct logs
3. Test on WiFi with dev server at `http://192.168.0.195:3000/member`
4. Report any issues

---

## Questions?

If the build fails at any step, check:
1. Node.js is installed: `node --version`
2. npm is installed: `npm --version`
3. Java is installed: `java -version`
4. Android SDK is installed and ANDROID_HOME is set
5. Gradle has internet connection

All dependencies should be installed from previous builds.
