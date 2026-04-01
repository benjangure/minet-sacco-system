# Building APK with Capacitor - Minet SACCO Member Portal

## What's Been Set Up

Capacitor has been configured to wrap your React web app into a native Android APK. When members install the APK:
1. Splash screen appears (red background with MS logo)
2. App loads the member portal
3. Login page displays
4. Works exactly like a Play Store app

## Prerequisites

Before building the APK, ensure you have:

- ✅ Java JDK installed (already verified)
- ✅ Android SDK installed
- ✅ Android Studio (recommended for easier setup)
- ✅ Gradle (comes with Android Studio)

## Step 1: Install Android Studio

1. Download from: https://developer.android.com/studio
2. Install Android Studio
3. During installation, select:
   - Android SDK
   - Android SDK Platform
   - Android Virtual Device (optional, for testing)

## Step 2: Set Up Android SDK Path

After installing Android Studio, set the ANDROID_SDK_ROOT environment variable:

**Windows (PowerShell):**
```powershell
$env:ANDROID_SDK_ROOT = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"
```

Or set it permanently in System Environment Variables:
- Search "Environment Variables"
- Click "Edit the system environment variables"
- Click "Environment Variables"
- Add new variable:
  - Name: `ANDROID_SDK_ROOT`
  - Value: `C:\Users\YourUsername\AppData\Local\Android\Sdk`

## Step 3: Build the APK

### Option A: Using Gradle (Command Line)

```bash
cd minetsacco-main/android
./gradlew assembleRelease
```

The APK will be generated at:
```
android/app/build/outputs/apk/release/app-release.apk
```

### Option B: Using Android Studio (Easier)

1. Open Android Studio
2. File → Open → Select `minetsacco-main/android` folder
3. Wait for Gradle sync to complete
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. APK will be generated in `android/app/build/outputs/apk/release/`

## Step 4: Sign the APK (Required for Distribution)

For Play Store or distribution, you need to sign the APK with a keystore.

### Create a Keystore (First Time Only)

```bash
keytool -genkey -v -keystore minet-sacco.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias minet-sacco
```

You'll be prompted for:
- Keystore password (remember this!)
- Key password (can be same as keystore)
- Your name, organization, etc.

### Sign the APK

```bash
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore minet-sacco.keystore android/app/build/outputs/apk/release/app-release-unsigned.apk minet-sacco
```

### Align the APK (Final Step)

```bash
zipalign -v 4 android/app/build/outputs/apk/release/app-release-unsigned.apk app-release.apk
```

The final signed APK is: `app-release.apk`

## Step 5: Test the APK

### On Android Phone

1. Enable "Unknown Sources" in Settings → Security
2. Transfer `app-release.apk` to phone
3. Open file manager and tap the APK
4. Tap "Install"
5. App appears on home screen
6. Tap to launch - splash screen appears, then login page

### On Android Emulator

1. Open Android Studio
2. Tools → Device Manager → Create Virtual Device
3. Run the emulator
4. Drag and drop APK onto emulator, or:
   ```bash
   adb install app-release.apk
   ```

## Updating the APK

When you make changes to the app:

1. Rebuild the web app:
   ```bash
   npm run build
   ```

2. Sync to Android:
   ```bash
   npx cap sync android
   ```

3. Rebuild the APK:
   ```bash
   cd android
   ./gradlew assembleRelease
   ```

## Troubleshooting

### "ANDROID_SDK_ROOT not set"
- Set the environment variable (see Step 2)
- Restart terminal/PowerShell after setting

### "Gradle not found"
- Install Android Studio (includes Gradle)
- Or install Gradle separately from gradle.org

### "Build fails with Java error"
- Ensure Java JDK is installed (not just JRE)
- Check Java version: `java -version`

### "APK won't install on phone"
- Ensure phone has "Unknown Sources" enabled
- Try uninstalling previous version first
- Check phone storage space

## File Locations

```
minetsacco-main/
├── android/                          ← Android project
│   ├── app/
│   │   ├── build/
│   │   │   └── outputs/apk/release/  ← APK location
│   │   └── src/
│   │       └── main/
│   │           └── assets/public/    ← Web app files
│   └── gradlew                        ← Gradle wrapper
├── capacitor.config.ts               ← Capacitor config
├── dist/                             ← Built web app
└── package.json
```

## Next Steps

1. Install Android Studio
2. Set ANDROID_SDK_ROOT environment variable
3. Run `./gradlew assembleRelease` in the android folder
4. Sign the APK (for distribution)
5. Test on phone or emulator
6. Share APK with members

## Distribution

To distribute the APK:

1. **Direct Download**: Host on your server, members download and install
2. **Play Store**: Upload signed APK to Google Play Console
3. **GitHub Releases**: Upload to GitHub for easy sharing
4. **QR Code**: Generate QR code linking to APK download

## Support

For issues:
- Check Android Studio logs
- Verify ANDROID_SDK_ROOT is set correctly
- Ensure Java JDK (not JRE) is installed
- Check Capacitor documentation: https://capacitorjs.com/docs/android
