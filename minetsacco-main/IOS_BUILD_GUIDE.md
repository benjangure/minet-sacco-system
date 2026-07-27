# iOS Build Guide for Minet SACCO App

This guide explains how to build the Minet SACCO mobile application for iOS devices using Capacitor.

## Prerequisites

### Required Software
1. **Xcode** (latest version) - Download from Mac App Store
2. **iOS Simulator** - Included with Xcode
3. **Apple Developer Account** (for device testing and App Store distribution)
4. **macOS** - iOS development requires a Mac
5. **Node.js** and **npm** - Already installed in your project

### Apple Developer Requirements
- For physical device testing: Apple Developer Program membership ($99/year)
- For App Store distribution: Apple Developer Program membership required
- For simulator testing only: No paid membership required

## Setup Instructions

### 1. Install Dependencies
```bash
npm install --legacy-peer-deps
```

### 2. Build the Web App
```bash
npm run build
```

### 3. Sync with iOS Platform
```bash
npm run ios:sync
```

## Build Commands

### Development Builds

#### Open in Xcode
```bash
npm run ios:open
```
This opens the iOS project in Xcode for configuration and debugging.

#### Run on iOS Simulator
```bash
npm run ios:run
```
This builds and runs the app on the iOS Simulator.

#### Build for Testing
```bash
npm run ios:build
```
This creates a release build for testing or distribution.

## Manual Xcode Build Process

If you prefer using Xcode directly:

1. **Open the project:**
   ```bash
   npx cap open ios
   ```

2. **In Xcode:**
   - Select your target device (simulator or physical device)
   - Press `Cmd+R` to build and run
   - Or use Product → Archive for distribution builds

## Configuration

### App Bundle ID
The app uses Bundle ID: `com.minetsacco.memberportal`

### App Name
- Display Name: "Minet SACCO"
- Scheme: "MinetSACCO"

### Capabilities
The app includes these Capacitor plugins:
- @capacitor-community/file-opener
- @capacitor/filesystem  
- @capacitor/splash-screen

## Distribution

### For App Store Submission
1. Ensure you have a paid Apple Developer account
2. Configure certificates and provisioning profiles in Xcode
3. Use Product → Archive in Xcode
4. Upload to App Store Connect

### For Ad-Hoc Distribution
1. Create distribution certificate
2. Register device UDIDs
3. Create provisioning profile
4. Build and distribute IPA file

### For Testing Flight
1. Build using App Store distribution method
2. Upload to App Store Connect
3. Add testers in TestFlight section

## Troubleshooting

### Common Issues

#### Build Failures
- Clean build folder: `Cmd+Shift+K` in Xcode
- Clear derived data: Xcode → Preferences → Locations → Derived Data
- Run `npx cap sync ios` to ensure latest web assets

#### Simulator Issues
- Reset Simulator: Simulator → Reset Content and Settings
- Ensure Simulator iOS version matches deployment target

#### Code Signing Issues
- Verify Apple Developer account status
- Check bundle identifier matches provisioning profile
- Ensure certificates are valid and installed

### Getting Help
- Capacitor documentation: https://capacitorjs.com/docs
- Apple Developer documentation: https://developer.apple.com/documentation/
- Xcode help menu

## Cross-Platform Development

### Android vs iOS Commands
```bash
# Android
npm run apk:sync
npm run apk:build

# iOS  
npm run ios:sync
npm run ios:build
npm run ios:run
```

### Shared Codebase
Both platforms share the same React web application code in the `src/` directory. Platform-specific configurations are in:
- `android/` - Android native project
- `ios/` - iOS native project
- `capacitor.config.ts` - Cross-platform configuration

## Next Steps

1. Test the app thoroughly on both simulator and physical devices
2. Configure proper app icons and splash screens for iOS
3. Set up proper code signing for distribution
4. Consider adding iOS-specific features like Face ID/Touch ID authentication
5. Test on various iOS device sizes and versions
