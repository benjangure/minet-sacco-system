# APK Build Issue Report - Minet SACCO Member Portal

## Executive Summary
We are attempting to build a native Android APK for the Minet SACCO member portal using Capacitor. The build process fails due to Java version incompatibility with Gradle. We need guidance on resolving this Java/Gradle compatibility issue.

---

## What We're Trying to Achieve

### Goal
Create a downloadable Android APK that members can install on their phones like a Play Store app. When installed:
- App shows a splash screen (red background with MS logo) for 3 seconds
- Loads the member portal web app
- Displays login page
- Works offline for critical pages (dashboard, loans, account statement)
- Feels like a native app, not a web shortcut

### Why This Approach
- No need to build separate native Android app
- Reuse existing React web app
- Single codebase for web and mobile
- Easy to update (rebuild APK, members reinstall)
- No app store approval needed

---

## What We've Done

### 1. Set Up Capacitor Framework
- Installed Capacitor CLI and core packages
- Created `capacitor.config.ts` with:
  - App ID: `com.minetsacco.memberportal`
  - App name: `Minet SACCO`
  - Splash screen configuration (red background, 3 second duration)
  - Web directory: `dist` (built React app)

### 2. Added Android Platform
- Ran `npx cap add android` to generate Android project
- Created proper Gradle project structure
- Synced built web app to Android assets

### 3. Built Web App
- Ran `npm run build` successfully
- Generated optimized production build in `dist/` folder
- All React components compile without errors

### 4. Configured Gradle
- Set up gradle wrapper with multiple versions attempted:
  - Gradle 7.6.4 (failed)
  - Gradle 7.5.1 (failed)
  - Gradle 8.5 (failed)

### 5. Fixed IP Configuration
- Updated `.env` file with correct local IP: `192.168.0.195:8080`
- Member portal now connects to backend successfully

---

## The Problem

### Error Message
```
BUG! exception in phase 'semantic analysis' in source unit '_BuildScript_'
Unsupported class file major version 69
```

### Root Cause
- **Java Version Installed**: Java 25 LTS (latest)
- **Java Bytecode Version**: 69 (Java 25)
- **Gradle Compatibility**: Gradle 8.5 supports up to Java 21
- **Incompatibility**: Java 25 bytecode (version 69) cannot be processed by Gradle 8.5

### Why It Happens
Gradle compiles build scripts using the installed Java version. When Java 25 is used, it generates bytecode version 69, which Gradle 8.5 doesn't understand. Even newer Gradle versions have limited Java 25 support.

---

## What We've Tried

### Attempt 1: Gradle 7.6.4
- **Result**: Failed with same Java version error
- **Reason**: Too old for Java 25

### Attempt 2: Gradle 7.5.1
- **Result**: Failed with same Java version error
- **Reason**: Supports only up to Java 18

### Attempt 3: Gradle 8.5
- **Result**: Failed with same Java version error
- **Reason**: Supports only up to Java 21

### Attempt 4: Clear Gradle Cache
- Deleted `~/.gradle/caches` and `~/.gradle/wrapper/dists`
- **Result**: Gradle re-downloaded but same error persists
- **Reason**: Issue is Java version, not cache

### Attempt 5: Multiple Gradle Versions
- Tried 3 different Gradle versions
- **Result**: All failed with same error
- **Reason**: Problem is Java 25, not Gradle version

---

## System Information

| Item | Value |
|------|-------|
| OS | Windows 10 (Build 26200.8037) |
| Java Version | 25.0.1 LTS |
| Java Bytecode | Version 69 |
| Gradle Attempted | 7.5.1, 7.6.4, 8.5 |
| Node.js | v24.11.0 |
| React | Latest (Vite build) |
| Capacitor | Latest |
| Android SDK | Installed |

---

## Possible Solutions

### Solution 1: Downgrade Java to 17 LTS (Recommended)
- Install Java 17 LTS (stable, widely supported)
- Set JAVA_HOME environment variable to Java 17
- Gradle 8.5 fully supports Java 17
- **Pros**: Guaranteed to work, stable version
- **Cons**: Need to uninstall/downgrade Java

### Solution 2: Downgrade Java to 21 LTS
- Install Java 21 LTS (newer, still supported)
- Set JAVA_HOME environment variable to Java 21
- Gradle 8.5 supports Java 21
- **Pros**: More recent than 17, still stable
- **Cons**: Still requires Java downgrade

### Solution 3: Use Java 25 with Gradle 9.0+
- Wait for Gradle 9.0 or later with Java 25 support
- **Pros**: Keeps latest Java
- **Cons**: Gradle 9.0 not yet released, unknown timeline

### Solution 4: Use Docker/Container
- Build APK inside Docker container with Java 17
- **Pros**: No local Java changes needed
- **Cons**: Requires Docker installation and knowledge

### Solution 5: Use EAS Build (Expo/Capacitor Cloud)
- Use cloud-based build service
- **Pros**: No local setup needed
- **Cons**: Requires account, may have costs

---

## Recommended Next Steps

1. **Install Java 17 LTS**
   - Download from: https://www.oracle.com/java/technologies/downloads/#java17
   - Or use: `choco install openjdk17` (if using Chocolatey)

2. **Set JAVA_HOME Environment Variable**
   - Windows: Set `JAVA_HOME=C:\Program Files\Java\jdk-17.x.x`
   - Verify: `echo %JAVA_HOME%` in CMD

3. **Verify Java Installation**
   - Run: `java -version`
   - Should show: `java version "17.x.x"`

4. **Retry APK Build**
   ```cmd
   cd minetsacco-main/android
   gradlew.bat --stop
   gradlew.bat assembleRelease
   ```

5. **Expected Result**
   - Build should complete in 5-10 minutes
   - APK generated at: `app/build/outputs/apk/release/app-release.apk`

---

## Files and Configuration

### Key Files
- `minetsacco-main/capacitor.config.ts` - Capacitor configuration
- `minetsacco-main/android/gradle/wrapper/gradle-wrapper.properties` - Gradle version
- `minetsacco-main/.env` - API URL configuration
- `minetsacco-main/android/app/build.gradle` - Android build config

### Build Command
```cmd
cd minetsacco-main/android
gradlew.bat assembleRelease
```

### Expected APK Location
```
minetsacco-main/android/app/build/outputs/apk/release/app-release.apk
```

---

## Questions for External Advice

1. Is downgrading to Java 17 the standard approach for Capacitor/Gradle builds?
2. Are there any known workarounds for Java 25 with Gradle?
3. Should we consider alternative build tools (EAS Build, Expo, etc.)?
4. Is there a way to force Gradle to use a specific Java version without changing JAVA_HOME?
5. What's the recommended Java version for production Capacitor builds?

---

## Timeline

- **Day 1**: Set up Capacitor, added Android platform
- **Day 2**: Multiple Gradle version attempts (7.5.1, 7.6.4, 8.5)
- **Day 3**: Cache clearing, environment troubleshooting
- **Current**: Identified Java 25 incompatibility as root cause

---

## Success Criteria

Once resolved:
- ✅ APK builds successfully without errors
- ✅ APK installs on Android phone
- ✅ Splash screen appears on launch
- ✅ Member portal loads and functions
- ✅ Login works with backend
- ✅ Offline mode works for critical pages

---

## Contact/Support Needed

We need guidance on:
1. Best practice for resolving Java/Gradle compatibility
2. Whether Java 17 downgrade is the right approach
3. Alternative solutions if downgrade isn't feasible
4. Any Capacitor-specific build configurations we might be missing

---

**Status**: Blocked on Java/Gradle compatibility issue
**Priority**: High (needed for next week's presentation)
**Estimated Resolution Time**: 30 minutes once Java is resolved
