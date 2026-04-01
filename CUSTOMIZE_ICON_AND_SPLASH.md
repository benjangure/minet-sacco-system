# Customize App Icon and Splash Screen

## Current Status

✅ **Splash Screen:** Already configured and enabled
- Duration: 3 seconds
- Background color: Red (#ef4444)
- Shows spinner while loading

❌ **App Icon:** Generic blue X icon (needs customization)

## Icon Customization

### Current Icon
The current icon is a generic blue X on white background. You want to change it to match the Minet SACCO theme (red with "M" logo).

### Icon Locations

All icon files are in:
```
minetsacco-main/android/app/src/main/res/
```

Specific locations:
- **Foreground:** `mipmap-*/ic_launcher_foreground.png`
- **Background:** `values/ic_launcher_background.xml`
- **Round icon:** `mipmap-*/ic_launcher_round.png`
- **Regular icon:** `mipmap-*/ic_launcher.png`

### Icon Sizes Required

| Folder | Size | DPI |
|--------|------|-----|
| mipmap-mdpi | 48x48 | 1x |
| mipmap-hdpi | 72x72 | 1.5x |
| mipmap-xhdpi | 96x96 | 2x |
| mipmap-xxhdpi | 144x144 | 3x |
| mipmap-xxxhdpi | 192x192 | 4x |

### How to Create Custom Icon

**Option 1: Use Android Studio (Easiest)**
1. Open Android Studio
2. Right-click `minetsacco-main/android/app/src/main/res`
3. Select "New" → "Image Asset"
4. Choose "Icon Type" → "Launcher Icons (Adaptive and Legacy)"
5. Upload your icon image
6. Android Studio will generate all sizes automatically

**Option 2: Use Online Tool**
1. Go to: https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
2. Upload your icon image
3. Download the generated icons
4. Extract and copy to `mipmap-*` folders

**Option 3: Manual Creation**
1. Create icon in Photoshop/GIMP at 192x192 pixels
2. Export as PNG
3. Resize to each required size
4. Copy to corresponding mipmap folders

### Recommended Icon Design

For Minet SACCO theme:
- **Background:** Red (#ef4444 or similar)
- **Foreground:** White "M" letter or money symbol
- **Style:** Simple, clean, professional
- **Shape:** Square with rounded corners

### Icon Files to Replace

**Foreground icons (the main icon):**
```
minetsacco-main/android/app/src/main/res/mipmap-mdpi/ic_launcher_foreground.png
minetsacco-main/android/app/src/main/res/mipmap-hdpi/ic_launcher_foreground.png
minetsacco-main/android/app/src/main/res/mipmap-xhdpi/ic_launcher_foreground.png
minetsacco-main/android/app/src/main/res/mipmap-xxhdpi/ic_launcher_foreground.png
minetsacco-main/android/app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png
```

**Background color (XML file):**
```
minetsacco-main/android/app/src/main/res/values/ic_launcher_background.xml
```

**Round icons:**
```
minetsacco-main/android/app/src/main/res/mipmap-*/ic_launcher_round.png
```

**Legacy icons:**
```
minetsacco-main/android/app/src/main/res/mipmap-*/ic_launcher.png
```

### Update Background Color

**File:** `minetsacco-main/android/app/src/main/res/values/ic_launcher_background.xml`

Current:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#FFFFFF</color>
</resources>
```

Change to red:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#ef4444</color>
</resources>
```

## Splash Screen Customization

### Current Splash Screen

✅ Already configured in `capacitor.config.ts`:
```typescript
SplashScreen: {
  launchShowDuration: 3000,      // Shows for 3 seconds
  launchAutoHide: true,           // Auto-hides when app loads
  backgroundColor: '#ef4444',     // Red background
  showSpinner: true,              // Shows loading spinner
  spinnerColor: '#ffffff'         // White spinner
}
```

### Splash Screen Images

Located in:
```
minetsacco-main/android/app/src/main/res/drawable-*/splash.png
```

Sizes:
- `drawable-port-mdpi/splash.png` - 320x426
- `drawable-port-hdpi/splash.png` - 480x640
- `drawable-port-xhdpi/splash.png` - 720x960
- `drawable-port-xxhdpi/splash.png` - 1080x1440
- `drawable-port-xxxhdpi/splash.png` - 1440x1920
- `drawable-land-*` - Landscape versions

### Customize Splash Screen Image

**Option 1: Use Capacitor CLI**
```bash
cd minetsacco-main
npx cap plugin:generate
```

**Option 2: Manual Update**
1. Create splash screen image (1080x1920 pixels for portrait)
2. Export as PNG
3. Resize to each required size
4. Replace files in `drawable-port-*` and `drawable-land-*` folders

### Recommended Splash Screen Design

- **Background:** Red (#ef4444)
- **Logo:** Minet SACCO logo or "M" in white
- **Text:** "Minet SACCO" in white
- **Tagline:** "Member Portal" in white
- **Style:** Clean, professional, matches login page

### Update Splash Screen Configuration

**File:** `minetsacco-main/capacitor.config.ts`

Current:
```typescript
SplashScreen: {
  launchShowDuration: 3000,
  launchAutoHide: true,
  backgroundColor: '#ef4444',
  showSpinner: true,
  spinnerColor: '#ffffff'
}
```

Options to customize:
```typescript
SplashScreen: {
  launchShowDuration: 3000,      // Duration in milliseconds
  launchAutoHide: true,           // Auto-hide when app ready
  backgroundColor: '#ef4444',     // Background color
  showSpinner: true,              // Show loading spinner
  spinnerColor: '#ffffff',        // Spinner color
  fadeOutDuration: 500            // Fade out animation
}
```

## Step-by-Step Implementation

### Step 1: Create Custom Icon

1. Design icon (192x192 pixels minimum)
2. Use Android Asset Studio or similar tool
3. Generate all required sizes
4. Save PNG files

### Step 2: Replace Icon Files

1. Copy generated icon files to:
   ```
   minetsacco-main/android/app/src/main/res/mipmap-*/
   ```

2. Update background color in:
   ```
   minetsacco-main/android/app/src/main/res/values/ic_launcher_background.xml
   ```

### Step 3: Create Custom Splash Screen

1. Design splash screen (1080x1920 pixels)
2. Export as PNG
3. Resize to all required sizes
4. Copy to:
   ```
   minetsacco-main/android/app/src/main/res/drawable-port-*/
   minetsacco-main/android/app/src/main/res/drawable-land-*/
   ```

### Step 4: Rebuild APK

```bash
cd minetsacco-main
npm run build
cd android
.\gradlew.bat clean assembleDebug
```

### Step 5: Test on Device

1. Uninstall old APK
2. Install new APK
3. Check icon appears correctly
4. Check splash screen shows for 3 seconds
5. Check app loads after splash

## Troubleshooting

### Icon not updating
- Clear Android build cache: `.\gradlew.bat clean`
- Uninstall old APK completely
- Rebuild and reinstall

### Splash screen not showing
- Check `capacitor.config.ts` has SplashScreen plugin configured
- Verify splash.png files exist in drawable folders
- Check `launchShowDuration` is > 0

### Icon looks blurry
- Ensure image is high resolution (at least 192x192)
- Use PNG format (not JPG)
- Check all size variants are correct

## Files to Modify

```
minetsacco-main/
├── capacitor.config.ts (splash screen config)
└── android/app/src/main/res/
    ├── values/ic_launcher_background.xml (background color)
    ├── mipmap-mdpi/
    │   ├── ic_launcher_foreground.png
    │   ├── ic_launcher_round.png
    │   └── ic_launcher.png
    ├── mipmap-hdpi/
    │   ├── ic_launcher_foreground.png
    │   ├── ic_launcher_round.png
    │   └── ic_launcher.png
    ├── mipmap-xhdpi/
    │   ├── ic_launcher_foreground.png
    │   ├── ic_launcher_round.png
    │   └── ic_launcher.png
    ├── mipmap-xxhdpi/
    │   ├── ic_launcher_foreground.png
    │   ├── ic_launcher_round.png
    │   └── ic_launcher.png
    ├── mipmap-xxxhdpi/
    │   ├── ic_launcher_foreground.png
    │   ├── ic_launcher_round.png
    │   └── ic_launcher.png
    ├── drawable-port-mdpi/splash.png
    ├── drawable-port-hdpi/splash.png
    ├── drawable-port-xhdpi/splash.png
    ├── drawable-port-xxhdpi/splash.png
    ├── drawable-port-xxxhdpi/splash.png
    ├── drawable-land-mdpi/splash.png
    ├── drawable-land-hdpi/splash.png
    ├── drawable-land-xhdpi/splash.png
    ├── drawable-land-xxhdpi/splash.png
    └── drawable-land-xxxhdpi/splash.png
```

## Quick Summary

| Item | Current | Action |
|------|---------|--------|
| Icon | Generic blue X | Replace with red M logo |
| Icon background | White | Change to red (#ef4444) |
| Splash screen | Exists | Update image to match theme |
| Splash duration | 3 seconds | Keep as is |
| Splash spinner | White | Keep as is |

---

**Next Steps:**
1. Design custom icon and splash screen
2. Generate all required sizes
3. Replace files in Android project
4. Rebuild APK
5. Test on device
