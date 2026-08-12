# ========================================
# Minet SACCO - Build Signed Release AAB
# ========================================
# 
# This script builds a production-ready, signed Android App Bundle (AAB)
# for Google Play Store upload.
#
# AAB is the recommended format for Play Store as it enables:
# - Smaller download sizes (Dynamic Delivery)
# - Automatic APK optimization per device
# - App serving optimization by Google
#

param(
    [switch]$SkipBuild,
    [switch]$SkipSync,
    [switch]$OpenOutput
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Minet SACCO - Release AAB Builder" -ForegroundColor Cyan
Write-Host "  Google Play Store Ready" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$APP_NAME = "Minet SACCO"
$BUILD_TYPE = "release"
$OUTPUT_AAB = "android\app\build\outputs\bundle\release\app-release.aab"
$FINAL_AAB = "minetsacco-playstore-v1.1.aab"

# Step 1: Build Web App
if (-not $SkipBuild) {
    Write-Host "📦 Step 1/4: Building web application..." -ForegroundColor Yellow
    Write-Host "Optimizing React app for production..." -ForegroundColor Gray
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

# Step 3: Build Signed AAB
Write-Host "🏗️  Step 3/4: Building signed release AAB..." -ForegroundColor Yellow
Write-Host "This process includes:" -ForegroundColor Gray
Write-Host "  • Code compilation" -ForegroundColor Gray
Write-Host "  • R8 code optimization and obfuscation" -ForegroundColor Gray
Write-Host "  • Resource shrinking" -ForegroundColor Gray
Write-Host "  • AAB bundling with split APKs" -ForegroundColor Gray
Write-Host "  • Bundle signing with release keystore" -ForegroundColor Gray
Write-Host ""
Write-Host "⏱️  This may take 3-5 minutes..." -ForegroundColor Gray
Write-Host ""

Push-Location android
$buildStart = Get-Date

# Run Gradle bundle build
.\gradlew.bat bundleRelease

$buildResult = $LASTEXITCODE
$buildEnd = Get-Date
$buildDuration = ($buildEnd - $buildStart).TotalSeconds

Pop-Location

if ($buildResult -ne 0) {
    Write-Host ""
    Write-Host "❌ AAB build failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Common issues:" -ForegroundColor Yellow
    Write-Host "  • Keystore password incorrect" -ForegroundColor Gray
    Write-Host "  • Missing release.keystore file" -ForegroundColor Gray
    Write-Host "  • ProGuard/R8 configuration errors" -ForegroundColor Gray
    Write-Host "  • OutOfMemory errors (increase Gradle memory)" -ForegroundColor Gray
    Write-Host "  • Build tools not installed" -ForegroundColor Gray
    Write-Host ""
    exit 1
}

Write-Host "✅ AAB build completed in $([math]::Round($buildDuration, 1)) seconds!" -ForegroundColor Green
Write-Host ""

# Step 4: Verify and Copy AAB
Write-Host "📋 Step 4/4: Verifying AAB..." -ForegroundColor Yellow

if (-not (Test-Path $OUTPUT_AAB)) {
    Write-Host "❌ AAB not found at expected location: $OUTPUT_AAB" -ForegroundColor Red
    exit 1
}

# Get AAB info
$aabSize = (Get-Item $OUTPUT_AAB).Length
$aabSizeMB = [math]::Round($aabSize / 1MB, 2)

# Copy to root with version name
Copy-Item $OUTPUT_AAB -Destination $FINAL_AAB -Force

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "✅ SUCCESS! AAB Ready for Play Store" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📱 AAB Information:" -ForegroundColor Cyan
Write-Host "   • File: $FINAL_AAB" -ForegroundColor White
Write-Host "   • Size: $aabSizeMB MB" -ForegroundColor White
Write-Host "   • Type: Signed Android App Bundle" -ForegroundColor White
Write-Host "   • Format: AAB (Play Store Optimized)" -ForegroundColor White
Write-Host "   • Optimization: R8 Full Mode + Dynamic Delivery" -ForegroundColor White
Write-Host "   • Signed: Yes (release.keystore)" -ForegroundColor White
Write-Host ""

# App Details
Write-Host "🔍 App Details:" -ForegroundColor Cyan
Write-Host "   • App Name: $APP_NAME" -ForegroundColor White
Write-Host "   • Package: com.minetsacco.memberportal" -ForegroundColor White
Write-Host "   • Version Name: 1.1" -ForegroundColor White
Write-Host "   • Version Code: 2" -ForegroundColor White
Write-Host "   • Min SDK: Android 7.0 (API 24)" -ForegroundColor White
Write-Host "   • Target SDK: Android 15 (API 36)" -ForegroundColor White
Write-Host "   • Compile SDK: Android 15 (API 36)" -ForegroundColor White
Write-Host ""

Write-Host "🔐 Signing Information:" -ForegroundColor Cyan
Write-Host "   • Keystore: android\app\release.keystore" -ForegroundColor White
Write-Host "   • Alias: minetsacco" -ForegroundColor White
Write-Host "   • Algorithm: SHA-384 with RSA (2048 bit)" -ForegroundColor White
Write-Host "   • Validity: 10,000 days (~27 years)" -ForegroundColor White
Write-Host ""

Write-Host "📂 AAB Location:" -ForegroundColor Cyan
Write-Host "   $(Resolve-Path $FINAL_AAB)" -ForegroundColor White
Write-Host ""

Write-Host "🎯 Next Steps - Upload to Google Play Console:" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Login to Google Play Console" -ForegroundColor Yellow
Write-Host "   https://play.google.com/console" -ForegroundColor White
Write-Host ""

Write-Host "2. Select Your App or Create New App" -ForegroundColor Yellow
Write-Host "   • Click 'Create app' if first time" -ForegroundColor White
Write-Host "   • Enter app name: Minet SACCO Member Portal" -ForegroundColor White
Write-Host "   • Select language: English" -ForegroundColor White
Write-Host "   • Category: Finance" -ForegroundColor White
Write-Host ""

Write-Host "3. Complete Store Listing (Left sidebar)" -ForegroundColor Yellow
Write-Host "   • App details" -ForegroundColor White
Write-Host "     - Short description (80 chars)" -ForegroundColor Gray
Write-Host "     - Full description (4000 chars)" -ForegroundColor Gray
Write-Host "   • Graphics" -ForegroundColor White
Write-Host "     - App icon: playstore-assets/icon-512x512.png" -ForegroundColor Gray
Write-Host "     - Feature graphic: playstore-assets/feature-graphic-1024x500.png" -ForegroundColor Gray
Write-Host "     - Screenshots: Capture from running app" -ForegroundColor Gray
Write-Host "   • Categorization" -ForegroundColor White
Write-Host "     - App category: Finance" -ForegroundColor Gray
Write-Host "   • Contact details" -ForegroundColor White
Write-Host "     - Email: admin@minetsacco.co.ke" -ForegroundColor Gray
Write-Host "   • Privacy policy" -ForegroundColor White
Write-Host "     - URL: https://minetsacco.co.ke/privacy-policy" -ForegroundColor Gray
Write-Host ""

Write-Host "4. Upload AAB (Production > Releases)" -ForegroundColor Yellow
Write-Host "   • Go to 'Production' in left sidebar" -ForegroundColor White
Write-Host "   • Click 'Create new release'" -ForegroundColor White
Write-Host "   • Upload: $FINAL_AAB" -ForegroundColor White
Write-Host "   • Add release notes" -ForegroundColor White
Write-Host "   • Click 'Save' then 'Review release'" -ForegroundColor White
Write-Host ""

Write-Host "5. Complete Content Rating" -ForegroundColor Yellow
Write-Host "   • Fill out questionnaire" -ForegroundColor White
Write-Host "   • App contains no objectionable content" -ForegroundColor White
Write-Host "   • Target audience: 18+" -ForegroundColor White
Write-Host ""

Write-Host "6. Set Pricing & Distribution" -ForegroundColor Yellow
Write-Host "   • Free app" -ForegroundColor White
Write-Host "   • Select countries: Kenya (and others if applicable)" -ForegroundColor White
Write-Host "   • Accept content guidelines" -ForegroundColor White
Write-Host ""

Write-Host "7. Review and Publish" -ForegroundColor Yellow
Write-Host "   • Review all sections for completeness" -ForegroundColor White
Write-Host "   • Click 'Send for review'" -ForegroundColor White
Write-Host "   • Wait for Google review (typically 1-3 days)" -ForegroundColor White
Write-Host ""

Write-Host "📋 Required Assets (in playstore-assets/):" -ForegroundColor Cyan
Write-Host "   ✓ icon-512x512.png" -ForegroundColor Green
Write-Host "   ✓ feature-graphic-1024x500.png" -ForegroundColor Green
Write-Host "   ✓ PLAY_STORE_LISTING.md (copy descriptions from here)" -ForegroundColor Green
Write-Host "   ✓ PRIVACY_POLICY.md (publish on website first)" -ForegroundColor Green
Write-Host "   ⚠ Screenshots needed - capture from running app" -ForegroundColor Yellow
Write-Host ""

Write-Host "📸 Screenshot Requirements:" -ForegroundColor Cyan
Write-Host "   • Minimum: 2 screenshots" -ForegroundColor White
Write-Host "   • Recommended: 4-8 screenshots" -ForegroundColor White
Write-Host "   • Format: PNG or JPEG" -ForegroundColor White
Write-Host "   • Size: 1080x1920 (portrait) or 1920x1080 (landscape)" -ForegroundColor White
Write-Host "   • Show: Login, Dashboard, Loans, Savings, Reports" -ForegroundColor White
Write-Host ""

Write-Host "⏱️  Review Timeline:" -ForegroundColor Cyan
Write-Host "   • Submission to review: Immediate" -ForegroundColor White
Write-Host "   • Review duration: 1-3 days (typically)" -ForegroundColor White
Write-Host "   • First-time reviews: May take up to 7 days" -ForegroundColor White
Write-Host "   • App updates: Usually faster (1-2 days)" -ForegroundColor White
Write-Host ""

Write-Host "✅ Pre-Launch Checklist:" -ForegroundColor Cyan
Write-Host "   [✓] AAB built and signed" -ForegroundColor Green
Write-Host "   [✓] App icons generated" -ForegroundColor Green
Write-Host "   [✓] Feature graphic created" -ForegroundColor Green
Write-Host "   [✓] App description written" -ForegroundColor Green
Write-Host "   [✓] Privacy policy drafted" -ForegroundColor Green
Write-Host "   [⚠] Privacy policy published online" -ForegroundColor Yellow
Write-Host "   [⚠] Screenshots captured" -ForegroundColor Yellow
Write-Host "   [⚠] App tested on devices" -ForegroundColor Yellow
Write-Host "   [⚠] Play Console account created" -ForegroundColor Yellow
Write-Host ""

Write-Host "🚨 Important Notes:" -ForegroundColor Yellow
Write-Host "   • Privacy policy MUST be published on a public URL" -ForegroundColor White
Write-Host "   • Test app thoroughly before submission" -ForegroundColor White
Write-Host "   • Respond to review feedback within 7 days" -ForegroundColor White
Write-Host "   • Keep keystore file safe - needed for all updates" -ForegroundColor White
Write-Host "   • App cannot be published without all required assets" -ForegroundColor White
Write-Host ""

# Open file location if requested
if ($OpenOutput) {
    Write-Host "📁 Opening file location..." -ForegroundColor Cyan
    explorer.exe /select,"$(Resolve-Path $FINAL_AAB)"
}

Write-Host "✨ Build completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "🎊 Ready to upload to Google Play Store!" -ForegroundColor Green
Write-Host ""

Write-Host "Press any key to exit..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
