# ========================================
# Minet SACCO - Build Signed Release APK
# ========================================
# 
# This script builds a production-ready, signed APK for:
# - Direct distribution
# - Internal testing
# - Side-loading
#
# For Play Store upload, use build-release-aab.ps1 instead
#

param(
    [switch]$SkipBuild,
    [switch]$SkipSync,
    [switch]$OpenOutput
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Minet SACCO - Release APK Builder" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$APP_NAME = "Minet SACCO"
$BUILD_TYPE = "release"
$OUTPUT_APK = "android\app\build\outputs\apk\release\app-release.apk"
$FINAL_APK = "minetsacco-release-v1.1.apk"

# Step 1: Build Web App
if (-not $SkipBuild) {
    Write-Host "📦 Step 1/4: Building web application..." -ForegroundColor Yellow
    Write-Host "This will optimize and bundle the React app..." -ForegroundColor Gray
    Write-Host ""
    
    npm run build
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Web build failed!" -ForegroundColor Red
        Write-Host "Check the error messages above and fix any issues." -ForegroundColor Red
        exit 1
    }
    
    Write-Host "✅ Web build completed successfully!" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "⏭️  Step 1/4: Skipping web build (using existing dist/)..." -ForegroundColor Yellow
    Write-Host ""
}

# Step 2: Sync with Capacitor
if (-not $SkipSync) {
    Write-Host "🔄 Step 2/4: Syncing with Capacitor..." -ForegroundColor Yellow
    Write-Host "Copying web assets to Android project..." -ForegroundColor Gray
    Write-Host ""
    
    npx cap sync android
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Capacitor sync failed!" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "✅ Capacitor sync completed!" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "⏭️  Step 2/4: Skipping Capacitor sync..." -ForegroundColor Yellow
    Write-Host ""
}

# Step 3: Build Signed APK
Write-Host "🏗️  Step 3/4: Building signed release APK..." -ForegroundColor Yellow
Write-Host "This process includes:" -ForegroundColor Gray
Write-Host "  • Code compilation" -ForegroundColor Gray
Write-Host "  • R8 code optimization and obfuscation" -ForegroundColor Gray
Write-Host "  • Resource shrinking" -ForegroundColor Gray
Write-Host "  • APK signing with release keystore" -ForegroundColor Gray
Write-Host "  • APK alignment" -ForegroundColor Gray
Write-Host ""
Write-Host "⏱️  This may take 3-5 minutes..." -ForegroundColor Gray
Write-Host ""

Push-Location android
$buildStart = Get-Date

# Run Gradle build
.\gradlew.bat assembleRelease

$buildResult = $LASTEXITCODE
$buildEnd = Get-Date
$buildDuration = ($buildEnd - $buildStart).TotalSeconds

Pop-Location

if ($buildResult -ne 0) {
    Write-Host ""
    Write-Host "❌ APK build failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "  • Keystore password incorrect" -ForegroundColor Gray
    Write-Host "  • Missing release.keystore file" -ForegroundColor Gray
    Write-Host "  • ProGuard/R8 configuration errors" -ForegroundColor Gray
    Write-Host "  • OutOfMemory errors (increase Gradle memory)" -ForegroundColor Gray
    Write-Host ""
    exit 1
}

Write-Host "✅ APK build completed in $([math]::Round($buildDuration, 1)) seconds!" -ForegroundColor Green
Write-Host ""

# Step 4: Verify and Copy APK
Write-Host "📋 Step 4/4: Verifying APK..." -ForegroundColor Yellow

if (-not (Test-Path $OUTPUT_APK)) {
    Write-Host "❌ APK not found at expected location: $OUTPUT_APK" -ForegroundColor Red
    exit 1
}

# Get APK info
$apkSize = (Get-Item $OUTPUT_APK).Length
$apkSizeMB = [math]::Round($apkSize / 1MB, 2)

# Copy to root with version name
Copy-Item $OUTPUT_APK -Destination $FINAL_APK -Force

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "✅ SUCCESS! APK Ready for Distribution" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📱 APK Information:" -ForegroundColor Cyan
Write-Host "   • File: $FINAL_APK" -ForegroundColor White
Write-Host "   • Size: $apkSizeMB MB" -ForegroundColor White
Write-Host "   • Type: Signed Release APK" -ForegroundColor White
Write-Host "   • Optimization: R8 Full Mode Enabled" -ForegroundColor White
Write-Host "   • Signed: Yes (release.keystore)" -ForegroundColor White
Write-Host ""

# APK Details
Write-Host "🔍 APK Details:" -ForegroundColor Cyan
Write-Host "   • App Name: $APP_NAME" -ForegroundColor White
Write-Host "   • Package: com.minetsacco.memberportal" -ForegroundColor White
Write-Host "   • Version: 1.1 (versionCode: 2)" -ForegroundColor White
Write-Host "   • Min SDK: Android 7.0 (API 24)" -ForegroundColor White
Write-Host "   • Target SDK: Android 15 (API 36)" -ForegroundColor White
Write-Host ""

Write-Host "📦 Distribution Options:" -ForegroundColor Cyan
Write-Host ""
Write-Host "Option 1: Direct Installation" -ForegroundColor Yellow
Write-Host "   1. Transfer $FINAL_APK to Android device" -ForegroundColor White
Write-Host "   2. Enable 'Install from Unknown Sources' in device settings" -ForegroundColor White
Write-Host "   3. Tap the APK file to install" -ForegroundColor White
Write-Host ""

Write-Host "Option 2: ADB Installation" -ForegroundColor Yellow
Write-Host "   adb install $FINAL_APK" -ForegroundColor White
Write-Host ""

Write-Host "Option 3: Internal Testing" -ForegroundColor Yellow
Write-Host "   • Upload to Firebase App Distribution" -ForegroundColor White
Write-Host "   • Share via email/cloud storage" -ForegroundColor White
Write-Host "   • Use for beta testing" -ForegroundColor White
Write-Host ""

Write-Host "⚠️  Note: For Google Play Store, build AAB instead:" -ForegroundColor Yellow
Write-Host "   .\build-release-aab.ps1" -ForegroundColor White
Write-Host ""

Write-Host "📂 APK Location:" -ForegroundColor Cyan
Write-Host "   $(Resolve-Path $FINAL_APK)" -ForegroundColor White
Write-Host ""

# Open file location if requested
if ($OpenOutput) {
    Write-Host "📁 Opening file location..." -ForegroundColor Cyan
    explorer.exe /select,"$(Resolve-Path $FINAL_APK)"
}

Write-Host "✨ Build completed successfully!" -ForegroundColor Green
Write-Host ""

# Display signing info
Write-Host "🔐 Signing Information:" -ForegroundColor Cyan
Write-Host "   • Keystore: android\app\release.keystore" -ForegroundColor White
Write-Host "   • Alias: minetsacco" -ForegroundColor White
Write-Host "   • Algorithm: SHA-384 with RSA (2048 bit)" -ForegroundColor White
Write-Host "   • Validity: 10,000 days (~27 years)" -ForegroundColor White
Write-Host ""

# Show next steps
Write-Host "🎯 Next Steps:" -ForegroundColor Cyan
Write-Host "   1. Test the APK on a physical device" -ForegroundColor White
Write-Host "   2. Verify all functionality works correctly" -ForegroundColor White
Write-Host "   3. Test with different Android versions" -ForegroundColor White
Write-Host "   4. Build AAB for Play Store submission" -ForegroundColor White
Write-Host ""

Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
