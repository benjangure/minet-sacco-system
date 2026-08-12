# ========================================
# Minet SACCO - Quick Build for Play Store
# ========================================
# 
# One-command builder that creates both APK and AAB
# Choose what you need for your use case
#

param(
    [Parameter(Mandatory=$false)]
    [ValidateSet("apk", "aab", "both")]
    [string]$BuildType = "both"
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "╔════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   Minet SACCO - Play Store Builder    ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Display build selection
Write-Host "📦 Build Configuration:" -ForegroundColor Yellow
switch ($BuildType) {
    "apk" {
        Write-Host "   • Building: APK only (for testing/distribution)" -ForegroundColor White
    }
    "aab" {
        Write-Host "   • Building: AAB only (for Play Store)" -ForegroundColor White
    }
    "both" {
        Write-Host "   • Building: Both APK and AAB" -ForegroundColor White
    }
}
Write-Host ""

# Build web app once
Write-Host "🌐 Building web application..." -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Web build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Web build completed!" -ForegroundColor Green
Write-Host ""

# Sync with Capacitor once
Write-Host "🔄 Syncing with Capacitor..." -ForegroundColor Yellow
npx cap sync android

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Sync failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Sync completed!" -ForegroundColor Green
Write-Host ""

# Build based on selection
if ($BuildType -eq "apk" -or $BuildType -eq "both") {
    Write-Host "🏗️  Building signed APK..." -ForegroundColor Yellow
    Push-Location android
    .\gradlew.bat assembleRelease
    $apkResult = $LASTEXITCODE
    Pop-Location
    
    if ($apkResult -eq 0) {
        Copy-Item "android\app\build\outputs\apk\release\app-release.apk" -Destination "minetsacco-release-v1.1.apk" -Force
        Write-Host "✅ APK ready: minetsacco-release-v1.1.apk" -ForegroundColor Green
    } else {
        Write-Host "❌ APK build failed!" -ForegroundColor Red
    }
    Write-Host ""
}

if ($BuildType -eq "aab" -or $BuildType -eq "both") {
    Write-Host "🏗️  Building signed AAB..." -ForegroundColor Yellow
    Push-Location android
    .\gradlew.bat bundleRelease
    $aabResult = $LASTEXITCODE
    Pop-Location
    
    if ($aabResult -eq 0) {
        Copy-Item "android\app\build\outputs\bundle\release\app-release.aab" -Destination "minetsacco-playstore-v1.1.aab" -Force
        Write-Host "✅ AAB ready: minetsacco-playstore-v1.1.aab" -ForegroundColor Green
    } else {
        Write-Host "❌ AAB build failed!" -ForegroundColor Red
    }
    Write-Host ""
}

Write-Host "════════════════════════════════════════" -ForegroundColor Green
Write-Host "✨ Build Process Completed!" -ForegroundColor Green
Write-Host "════════════════════════════════════════" -ForegroundColor Green
Write-Host ""

# Display results
Write-Host "📦 Build Artifacts:" -ForegroundColor Cyan
if ($BuildType -eq "apk" -or $BuildType -eq "both") {
    if (Test-Path "minetsacco-release-v1.1.apk") {
        $size = [math]::Round((Get-Item "minetsacco-release-v1.1.apk").Length / 1MB, 2)
        Write-Host "   ✓ minetsacco-release-v1.1.apk ($size MB)" -ForegroundColor Green
    }
}
if ($BuildType -eq "aab" -or $BuildType -eq "both") {
    if (Test-Path "minetsacco-playstore-v1.1.aab") {
        $size = [math]::Round((Get-Item "minetsacco-playstore-v1.1.aab").Length / 1MB, 2)
        Write-Host "   ✓ minetsacco-playstore-v1.1.aab ($size MB)" -ForegroundColor Green
    }
}
Write-Host ""

Write-Host "🎯 Next Steps:" -ForegroundColor Cyan
Write-Host "   • APK: Test on device or distribute internally" -ForegroundColor White
Write-Host "   • AAB: Upload to Google Play Console" -ForegroundColor White
Write-Host "   • Assets: playstore-assets/ directory" -ForegroundColor White
Write-Host ""

Write-Host "📚 Documentation:" -ForegroundColor Cyan
Write-Host "   • Build details: See build-release-apk.ps1 or build-release-aab.ps1" -ForegroundColor White
Write-Host "   • Play Store: See GOOGLE_PLAY_STORE_DEPLOYMENT_GUIDE.md" -ForegroundColor White
Write-Host ""
