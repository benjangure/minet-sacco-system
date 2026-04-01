# APK Current State and Issues - Documentation

## What's Currently Implemented

The APK is built using **Capacitor**, which wraps a React web application as a native Android app. The setup includes:

- **Frontend**: React + TypeScript web app (member portal)
- **Wrapper**: Capacitor framework to package web app as APK
- **Configuration**: 
  - `capacitor.config.ts` - Main Capacitor configuration
  - `android/app/src/main/assets/capacitor.config.json` - Android-specific config
  - `minetsacco-main/.env` - Environment variables with API URL

## Current Issues

### 1. **Login Not Working - "Failed to fetch" Error**
**Symptom**: When entering valid credentials (member1/password), the app shows "Failed to fetch" error

**Root Cause**: 
- The app is configured to load local files from the Android assets directory
- However, the API calls to the backend are failing
- The backend is running at `192.168.0.195:8080` on your local network
- The phone is on the same WiFi network but cannot reach the backend

**What's been tried**:
- Changed `androidScheme` from `https` to `http`
- Added `allowNavigation` for the backend IP
- Updated `.env` with correct API URL: `VITE_API_URL=http://192.168.0.195:8080/api`
- Rebuilt APK multiple times with `npx cap sync android` and `gradlew clean assembleDebug`

**Why it's still not working**:
- Capacitor's local server configuration may be interfering with external API calls
- There may be CORS issues between the local app and backend
- Network connectivity between phone and backend needs verification

### 2. **Splash Screen Not Displaying**
**Symptom**: App opens directly to login page without showing splash screen

**Configuration**: 
- Splash screen is configured in `capacitor.config.ts` with red background (#ef4444)
- Duration set to 3 seconds
- Spinner enabled

**Why it's not working**:
- The `@capacitor/splash-screen` plugin may not be properly initialized
- The plugin initialization code may be missing from `src/main.tsx` or `src/App.tsx`

### 3. **App Icon**
**Status**: ✅ Working
- Red "M" icon is properly configured
- Uses adaptive icon system for Android

## What Was Removed

To simplify the login page:
- ❌ Removed "Are you staff? Login here" link
- ❌ Removed "Download APK" button
- ❌ Removed "Access on Mobile" section

The login page now shows only:
- Minet SACCO branding with red M icon
- Phone Number or Employee ID field
- National ID (password) field
- Login button
- Info cards about features

## APK Location

**File**: `minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk`

**Size**: ~4.4 MB

**Installation**: Transfer to Samsung A14 and tap to install (allow unknown sources)

## Next Steps to Fix Login

### Option 1: Verify Network Connectivity
1. On your phone, open a browser and try: `http://192.168.0.195:8080/api/auth/member/login`
2. If it shows an error page, the phone can reach the backend
3. If it times out, there's a network issue

### Option 2: Check Capacitor Logs
1. Connect phone to computer via USB
2. Run: `adb logcat | grep -i capacitor`
3. Look for any error messages about loading or API calls

### Option 3: Simplify the Approach
Instead of using Capacitor's complex configuration, consider:
- Using a simple WebView wrapper that just loads the web portal
- This would be more straightforward and easier to debug
- Would eliminate Capacitor's local server complexity

## Backend Configuration

**Backend URL**: `http://192.168.0.195:8080`

**API Endpoint**: `http://192.168.0.195:8080/api/auth/member/login`

**Test Credentials**: 
- Username: `member1`
- Password: `password`

**CORS**: Configured in `backend/src/main/java/com/minet/sacco/config/CorsConfig.java`

## Files Modified

- `minetsacco-main/src/pages/MemberLogin.tsx` - Removed staff link and APK download
- `minetsacco-main/capacitor.config.ts` - Changed to `http` scheme with `allowNavigation`
- `minetsacco-main/android/app/src/main/assets/capacitor.config.json` - Synced config

## Recommendation

The current Capacitor setup is adding complexity without solving the core issue. For a simpler, more reliable solution, consider:

1. **Using a native WebView wrapper** - Simpler to debug and configure
2. **Testing with a development server** - Run `npm run dev` on your computer and access from phone browser to isolate issues
3. **Getting help from Capacitor community** - The issue may be specific to Capacitor's local server handling

The backend and frontend code are working correctly (verified on web). The issue is specifically with how the APK is loading and communicating with the backend.
