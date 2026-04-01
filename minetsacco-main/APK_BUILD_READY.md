# APK Build - Ready to Go

## Status: ✅ All Configuration Complete

Your system is now ready to build the Android APK. Java 17 is installed and gradle.properties has been updated.

## Build Steps

### Step 1: Open Command Prompt
```
Press Windows Key + R
Type: cmd
Press Enter
```

### Step 2: Navigate to Android Directory
```
cd C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main\minetsacco-main\android
```

### Step 3: Stop Gradle Daemon & Clean
```
gradlew.bat --stop
gradlew.bat clean
```

### Step 4: Build Debug APK
```
gradlew.bat assembleDebug
```

This will take 5-10 minutes on first build. You'll see:
- Downloading Gradle 8.5
- Compiling Android resources
- Building APK
- Final message: "BUILD SUCCESSFUL"

### Step 5: Find Your APK
Once build completes, your APK is at:
```
C:\Users\Elitebook\OneDrive\Desktop\minetsacco-main\minetsacco-main\android\app\build\outputs\apk\debug\app-debug.apk
```

## Testing on Phone

### Option 1: USB Transfer
1. Connect phone to computer via USB
2. Copy `app-debug.apk` to phone
3. On phone: Settings → Security → Enable "Install from unknown sources"
4. Open file manager, find APK, tap to install

### Option 2: Email/Cloud
1. Email the APK to yourself
2. Download on phone
3. Enable "Install from unknown sources"
4. Tap APK to install

### Option 3: Same WiFi (Recommended for Testing)
1. Both phone and computer on same WiFi (192.168.0.x)
2. Use file sharing or AirDroid to transfer APK
3. Install on phone

## What to Test

After installing:
1. Splash screen appears (red background, "MS" logo, "MINET SACCO")
2. Login page loads
3. Enter member credentials
4. Dashboard displays
5. Try M-Pesa deposit/withdrawal
6. Check recent transactions (should only show successful ones)

## Troubleshooting

**Build fails with "Unsupported class file major version 69"**
- Java 25 is still being used
- Verify gradle.properties has correct Java 17 path
- Run: `gradlew.bat --stop` then try again

**APK not found after build**
- Check build output for "BUILD SUCCESSFUL"
- Navigate to exact path: `android/app/build/outputs/apk/debug/`
- If folder doesn't exist, build failed - check error messages

**APK won't install on phone**
- Enable "Install from unknown sources" in Settings
- Try different transfer method
- Check phone storage has space

## Next Steps After Testing

Once APK works on phone:
1. Test all member portal features
2. Verify M-Pesa transactions work
3. Check offline functionality (PWA features)
4. When ready for release, build release APK: `gradlew.bat assembleRelease`

---

**Configuration Files Updated:**
- ✅ `minetsacco-main/android/gradle.properties` - Java 17 path set
- ✅ `minetsacco-main/android/gradle/wrapper/gradle-wrapper.properties` - Gradle 8.5 configured
- ✅ `minetsacco-main/android/app/build.gradle` - Java 17 compatibility set
- ✅ `minetsacco-main/.env` - API URL configured for local network
