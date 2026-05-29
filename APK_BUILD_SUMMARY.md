# APK Build & Rebuild Guide

## Overview

This is the complete reference guide for building and rebuilding the Minet SACCO APK (member portal mobile application). The APK is exclusively for members and should not include staff portal features.

---

## Prerequisites

- Node.js and npm installed
- Android SDK and Gradle configured
- Java Development Kit (JDK) installed
- Latest code from repository

---

## Quick Build (3 Steps)

### Step 1: Build Frontend
```bash
cd minetsacco-main
npm run build
```
Compiles React/TypeScript into production assets in `dist/` folder.

### Step 2: Sync with Android
```bash
npx cap sync android
```
Copies built assets to Android project and updates Capacitor plugins.

### Step 3: Build APK
```bash
cd android
.\gradlew.bat assembleDebug
```
Generates the debug APK file.

**APK Location:** `minetsacco-main/android/app/build/outputs/apk/debug/app-debug.apk`

---

## One-Liner Build Command

```bash
npm run build && npx cap sync android && cd android && .\gradlew.bat assembleDebug
```

---

## Network Configuration

### Critical File: `network_security_config.xml`
**Path:** `minetsacco-main/android/app/src/main/res/xml/network_security_config.xml`

This file controls which IPs the APK can connect to. Current configuration allows:
- `192.168.0.x` range
- `192.168.1.x` range
- `192.168.100.x` range (includes `192.168.100.54`)
- `10.0.0.x` range
- `localhost` and `127.0.0.1`
- `base-config` section allows cleartext traffic to any IP

**Update this file only if:** Adding a new IP range not already listed.

### Backend URL Configuration
**File:** `minetsacco-main/src/config/api.ts`

Default backend URL:
```typescript
DEFAULT_NATIVE_BACKEND_URL: 'http://192.168.100.54:8080'
```

**Update when:** Backend IP changes. Then rebuild APK.

### Runtime Configuration (No Rebuild Needed)
After installing APK on device, configure backend URL via app Settings page:
- Open app → Settings
- Enter backend URL: `http://<new-ip>:8080`
- No rebuild required

---

## Handling IP Changes

**Option 1: Update Config & Rebuild (Permanent)**
1. Edit `minetsacco-main/src/config/api.ts` with new IP
2. Run full build process (3 steps above)
3. Install new APK

**Option 2: Use Settings Page (Quick)**
1. Open installed APK
2. Go to Settings
3. Enter new backend URL
4. No rebuild needed

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Gradle build fails | Run `.\gradlew.bat clean` then retry |
| APK won't connect to backend | Verify backend running: `curl http://<ip>:8080/api/auth/health` |
| Frontend build fails | Clear node_modules, run `npm install`, retry `npm run build` |
| APK can't reach new IP | Check IP is in `network_security_config.xml` or use Settings page |

---

## Important Notes

- **APK = Member Portal Only:** Do not add staff features to APK
- **Staff Portal:** Web-based at `localhost:8080` (separate from APK)
- **HTTP Only:** APK uses HTTP (not HTTPS) for local network communication
- **Build Time:** 2-5 minutes depending on system
- **No Rebuild for IP Changes:** Use Settings page instead (faster)

---

## Build History

- **v1.0** (May 1, 2026): Network security configuration fixed
  - Added `192.168.100.54` to allowed IPs
  - Added `base-config` for flexible IP handling
  - APK now connects successfully to backend
