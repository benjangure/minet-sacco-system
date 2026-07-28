# Deploy Frontend to IIS Server
# This script copies the built frontend to the IIS web folder

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Deploy Frontend to IIS Server" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

$SOURCE_DIR = "C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\minetsacco-main\dist"
$DEST_DIR = "C:\inetpub\minetsacco"

# Step 1: Verify source files exist
Write-Host "Step 1: Verifying source files..." -ForegroundColor Yellow
if (-not (Test-Path $SOURCE_DIR)) {
    Write-Host "  ERROR: Source directory not found: $SOURCE_DIR" -ForegroundColor Red
    Write-Host "  Please run 'npm run build' first!" -ForegroundColor Red
    exit 1
}

$sourceFiles = Get-ChildItem -Path $SOURCE_DIR -Recurse -File
Write-Host "  Found $($sourceFiles.Count) files in source directory" -ForegroundColor Green

# Step 2: Stop IIS
Write-Host ""
Write-Host "Step 2: Stopping IIS..." -ForegroundColor Yellow
iisreset /stop
Start-Sleep -Seconds 2

# Step 3: Clear old files
Write-Host ""
Write-Host "Step 3: Clearing old files from IIS..." -ForegroundColor Yellow
if (Test-Path $DEST_DIR) {
    Get-ChildItem -Path $DEST_DIR -Recurse | Remove-Item -Force -Recurse -ErrorAction SilentlyContinue
    Write-Host "  Cleared: $DEST_DIR" -ForegroundColor Green
} else {
    Write-Host "  Creating destination directory: $DEST_DIR" -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $DEST_DIR -Force | Out-Null
}

# Step 4: Copy new files
Write-Host ""
Write-Host "Step 4: Copying new frontend files..." -ForegroundColor Yellow
Copy-Item -Path "$SOURCE_DIR\*" -Destination $DEST_DIR -Recurse -Force
Write-Host "  Copied all files to: $DEST_DIR" -ForegroundColor Green

# Step 5: Verify deployment
Write-Host ""
Write-Host "Step 5: Verifying deployment..." -ForegroundColor Yellow
$deployedFiles = Get-ChildItem -Path $DEST_DIR -Recurse -File
Write-Host "  Total files deployed: $($deployedFiles.Count)" -ForegroundColor Green

# Check for index.html
$indexFile = Get-Item "$DEST_DIR\index.html" -ErrorAction SilentlyContinue
if ($indexFile) {
    Write-Host "  ✓ index.html found (Modified: $($indexFile.LastWriteTime))" -ForegroundColor Green
} else {
    Write-Host "  ✗ index.html NOT found!" -ForegroundColor Red
}

# Check for assets folder
$assetsFolder = Get-ChildItem -Path "$DEST_DIR\assets" -ErrorAction SilentlyContinue
if ($assetsFolder) {
    Write-Host "  ✓ assets folder found with $($assetsFolder.Count) files" -ForegroundColor Green
    Write-Host "  Latest JS file:" -ForegroundColor Cyan
    $assetsFolder | Where-Object { $_.Extension -eq ".js" } | Sort-Object LastWriteTime -Descending | Select-Object -First 1 | Format-Table Name, LastWriteTime -AutoSize
} else {
    Write-Host "  ✗ assets folder NOT found!" -ForegroundColor Red
}

# Step 6: Start IIS
Write-Host ""
Write-Host "Step 6: Starting IIS..." -ForegroundColor Yellow
iisreset /start
Start-Sleep -Seconds 3
Write-Host "  IIS started" -ForegroundColor Green

Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. On your device, clear browser cache (Ctrl+Shift+Del)" -ForegroundColor Gray
Write-Host "2. Go to: http://10.39.60.15:8090" -ForegroundColor Gray
Write-Host "3. Open DevTools (F12) and check Console" -ForegroundColor Gray
Write-Host "4. Verify API calls go to: http://10.39.60.15:9090/api/*" -ForegroundColor Gray
Write-Host "   (NOT localhost:9090)" -ForegroundColor Gray
Write-Host ""
