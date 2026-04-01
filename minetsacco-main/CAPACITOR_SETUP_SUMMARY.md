# Capacitor Setup Complete - APK Ready to Build

## What's Done

✅ Capacitor installed and configured
✅ Android platform added
✅ Web app built and synced to Android
✅ Splash screen configured (red background, 3 second duration)
✅ Build scripts added to package.json

## Quick Start - Build APK

### Option 1: Using PowerShell Script (Easiest)
```powershell
.\build-apk.ps1
```

### Option 2: Manual Steps
```bash
# 1. Build web app
npm run build

# 2. Sync to Android
npx cap sync android

# 3. Build APK
cd android
./gradlew assembleRelease
cd ..
```

### Option 3: Using npm scripts
```bash
npm run apk:sync    # Build web + sync
npm run apk:build   # Build APK
```

## What You Need

Before building, install:
1. **Android Studio** - https://developer.android.com/studio
2. **Set ANDROID_SDK_ROOT** environment variable (see APK_BUILD_GUIDE.md)

## APK Location

After building, the APK is at:
```
minetsacco-main/android/app/build/outputs/apk/release/app-release.apk
```

## Testing

1. Transfer APK to phone
2. Enable "Unknown Sources" in Settings → Security
3. Install the APK
4. App appears on home screen
5. Tap to launch - splash screen appears, then login page

## Signing for Distribution

For Play Store or sharing, sign the APK:

```bash
# Create keystore (first time only)
keytool -genkey -v -keystore minet-sacco.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias minet-sacco

# Sign APK
jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore minet-sacco.keystore android/app/build/outputs/apk/release/app-release-unsigned.apk minet-sacco

# Align APK
zipalign -v 4 android/app/build/outputs/apk/release/app-release-unsigned.apk app-release.apk
```

## Files

- `capacitor.config.ts` - Capacitor configuration
- `APK_BUILD_GUIDE.md` - Detailed build instructions
- `build-apk.ps1` - PowerShell build script
- `android/` - Android project (Gradle-based)

## Next Steps

1. Install Android Studio
2. Set ANDROID_SDK_ROOT environment variable
3. Run `.\build-apk.ps1` or `npm run apk:sync && npm run apk:build`
4. Test APK on phone
5. Sign APK for distribution
6. Share with members

## Troubleshooting

See `APK_BUILD_GUIDE.md` for detailed troubleshooting.

Common issues:
- ANDROID_SDK_ROOT not set → Set environment variable
- Gradle not found → Install Android Studio
- Build fails → Check Java version (need JDK, not JRE)
