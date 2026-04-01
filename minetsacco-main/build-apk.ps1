# Quick APK Build Script for Windows PowerShell
# Usage: .\build-apk.ps1

Write-Host "🔨 Minet SACCO APK Builder" -ForegroundColor Cyan
Write-Host "=========================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build web app
Write-Host "📦 Step 1: Building web app..." -ForegroundColor Yellow
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Web build failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Web build complete" -ForegroundColor Green
Write-Host ""

# Step 2: Sync to Android
Write-Host "🔄 Step 2: Syncing to Android..." -ForegroundColor Yellow
npx cap sync android
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Sync failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Sync complete" -ForegroundColor Green
Write-Host ""

# Step 3: Build APK
Write-Host "🏗️  Step 3: Building APK..." -ForegroundColor Yellow
Write-Host "This may take 5-10 minutes on first build..." -ForegroundColor Gray
Write-Host "Downloading Gradle (one-time, ~200MB)..." -ForegroundColor Gray
Push-Location android
.\gradlew.bat assembleRelease
$buildResult = $LASTEXITCODE
Pop-Location

if ($buildResult -ne 0) {
    Write-Host "❌ APK build failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ APK build complete" -ForegroundColor Green
Write-Host ""

# Step 4: Show APK location
$apkPath = "android\app\build\outputs\apk\release\app-release.apk"
if (Test-Path $apkPath) {
    Write-Host "✅ APK ready!" -ForegroundColor Green
    Write-Host "📍 Location: $apkPath" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Yellow
    Write-Host "1. Transfer APK to phone"
    Write-Host "2. Enable 'Unknown Sources' in Settings"
    Write-Host "3. Install the APK"
    Write-Host "4. Launch the app"
} else {
    Write-Host "❌ APK not found at expected location" -ForegroundColor Red
    exit 1
}
