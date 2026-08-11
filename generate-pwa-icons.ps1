Write-Host "=== Generating PWA Icons from Minet Logo ===" -ForegroundColor Cyan
Write-Host ""

$sourceLogo = "minetsacco-main/public/Minet-Logo1.png"
$outputDir = "minetsacco-main/public"

# Check if source logo exists
if (-not (Test-Path $sourceLogo)) {
    Write-Host "❌ Source logo not found: $sourceLogo" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Found source logo: $sourceLogo" -ForegroundColor Green
Write-Host ""

# Check if ImageMagick or similar tool is available
$hasConvert = Get-Command convert -ErrorAction SilentlyContinue
$hasMagick = Get-Command magick -ErrorAction SilentlyContinue

if ($hasMagick) {
    Write-Host "✅ Found ImageMagick (magick command)" -ForegroundColor Green
    $convertCmd = "magick"
} elseif ($hasConvert) {
    Write-Host "✅ Found ImageMagick (convert command)" -ForegroundColor Green
    $convertCmd = "convert"
} else {
    Write-Host "⚠️  ImageMagick not found. Icon generation skipped." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To generate icons, install ImageMagick:" -ForegroundColor Yellow
    Write-Host "  Download: https://imagemagick.org/script/download.php#windows" -ForegroundColor Gray
    Write-Host "  Or use: winget install ImageMagick.ImageMagick" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Manual alternative:" -ForegroundColor Yellow
    Write-Host "  1. Open Minet-Logo1.png in an image editor" -ForegroundColor Gray
    Write-Host "  2. Resize and save as:" -ForegroundColor Gray
    Write-Host "     - icon-192.png (192x192)" -ForegroundColor Gray
    Write-Host "     - icon-512.png (512x512)" -ForegroundColor Gray
    Write-Host "     - splash-1080x1920.png (1080x1920)" -ForegroundColor Gray
    Write-Host "     - splash-1125x2436.png (1125x2436)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "For now, copying original logo as fallback icons..." -ForegroundColor Yellow
    
    # Copy original logo as fallback
    Copy-Item $sourceLogo "$outputDir/icon-192.png" -Force
    Copy-Item $sourceLogo "$outputDir/icon-512.png" -Force
    
    Write-Host "✅ Fallback icons created (using original logo)" -ForegroundColor Green
    exit 0
}

Write-Host ""
Write-Host "Generating icons..." -ForegroundColor Yellow

# Generate 192x192 icon
& $convertCmd $sourceLogo -resize 192x192 -background white -gravity center -extent 192x192 "$outputDir/icon-192.png"
if ($?) {
    Write-Host "✅ Generated icon-192.png" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to generate icon-192.png" -ForegroundColor Red
}

# Generate 512x512 icon
& $convertCmd $sourceLogo -resize 512x512 -background white -gravity center -extent 512x512 "$outputDir/icon-512.png"
if ($?) {
    Write-Host "✅ Generated icon-512.png" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to generate icon-512.png" -ForegroundColor Red
}

# Generate splash screens (optional, using logo centered on white background)
& $convertCmd -size 1080x1920 xc:white $sourceLogo -resize 600x600 -gravity center -composite "$outputDir/splash-1080x1920.png"
if ($?) {
    Write-Host "✅ Generated splash-1080x1920.png" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to generate splash-1080x1920.png" -ForegroundColor Red
}

& $convertCmd -size 1125x2436 xc:white $sourceLogo -resize 600x600 -gravity center -composite "$outputDir/splash-1125x2436.png"
if ($?) {
    Write-Host "✅ Generated splash-1125x2436.png" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to generate splash-1125x2436.png" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Icon Generation Complete ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Generated files:" -ForegroundColor Green
Get-ChildItem "$outputDir/icon-*.png", "$outputDir/splash-*.png" 2>$null | ForEach-Object {
    $sizeKB = [math]::Round($_.Length / 1KB, 2)
    Write-Host "  $($_.Name) - $sizeKB KB" -ForegroundColor Gray
}
