# Fix Network Access Script for Minet SACCO
# Run this script on the server to fix the CORS and network access issues

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "Minet SACCO - Fix Network Access" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Stop IIS
Write-Host "Step 1: Stopping IIS..." -ForegroundColor Yellow
iisreset /stop
Start-Sleep -Seconds 2

# Step 2: Clear old frontend files
Write-Host "Step 2: Clearing old frontend files from IIS..." -ForegroundColor Yellow
Get-ChildItem "C:\inetpub\minetsacco" -Recurse | Remove-Item -Force -Recurse -ErrorAction SilentlyContinue
Write-Host "  Cleared C:\inetpub\minetsacco" -ForegroundColor Green

# Step 3: Copy new dist files
Write-Host "Step 3: Copying NEW frontend build from local machine..." -ForegroundColor Yellow
Write-Host "  SOURCE: C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\minetsacco-main\dist\*" -ForegroundColor Cyan
Write-Host "  DESTINATION: C:\inetpub\minetsacco\" -ForegroundColor Cyan
Write-Host ""
Write-Host "  YOU NEED TO MANUALLY COPY THE FILES!" -ForegroundColor Red
Write-Host "  1. On your LOCAL machine, build the frontend:" -ForegroundColor Yellow
Write-Host "     cd C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\minetsacco-main" -ForegroundColor Gray
Write-Host "     npm run build" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. Copy the dist folder contents to the server:" -ForegroundColor Yellow
Write-Host "     FROM: C:\Users\Lenovo\Desktop\minet-sacco\minet-sacco-system\minetsacco-main\dist\*" -ForegroundColor Gray
Write-Host "     TO: C:\inetpub\minetsacco\" -ForegroundColor Gray
Write-Host ""
Read-Host "Press Enter AFTER you have copied the files to continue"

# Step 4: Verify files copied
Write-Host "Step 4: Verifying files..." -ForegroundColor Yellow
$assetFiles = Get-ChildItem "C:\inetpub\minetsacco\assets" -ErrorAction SilentlyContinue
if ($assetFiles) {
    Write-Host "  Found $($assetFiles.Count) files in assets folder" -ForegroundColor Green
    Write-Host "  Latest file:" -ForegroundColor Cyan
    $assetFiles | Sort-Object LastWriteTime -Descending | Select-Object -First 1 | Format-Table Name, LastWriteTime
} else {
    Write-Host "  WARNING: No files found in C:\inetpub\minetsacco\assets!" -ForegroundColor Red
}

# Step 5: Check if backend is running
Write-Host "Step 5: Checking backend status..." -ForegroundColor Yellow
$javaProcess = Get-Process -Name "java" -ErrorAction SilentlyContinue
if ($javaProcess) {
    Write-Host "  Backend is RUNNING (PID: $($javaProcess.Id))" -ForegroundColor Green
} else {
    Write-Host "  Backend is NOT running!" -ForegroundColor Red
    Write-Host "  Start it with:" -ForegroundColor Yellow
    Write-Host "  cd C:\minetsacco-deploy\backend" -ForegroundColor Gray
    Write-Host "  java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar" -ForegroundColor Gray
}

# Step 6: Start IIS
Write-Host "Step 6: Starting IIS..." -ForegroundColor Yellow
iisreset /start
Start-Sleep -Seconds 3

Write-Host ""
Write-Host "=====================================" -ForegroundColor Green
Write-Host "NEXT STEPS:" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host "1. On your DEVICE browser (not server):" -ForegroundColor Yellow
Write-Host "   - Open browser DevTools (F12)" -ForegroundColor Gray
Write-Host "   - Go to Application > Storage > Local Storage" -ForegroundColor Gray
Write-Host "   - Find 'http://10.39.60.15:8090'" -ForegroundColor Gray
Write-Host "   - DELETE the 'backendUrl' key (if it exists)" -ForegroundColor Gray
Write-Host "   - Clear all storage, then close DevTools" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Hard refresh the page:" -ForegroundColor Yellow
Write-Host "   - Ctrl+Shift+R (Chrome/Edge)" -ForegroundColor Gray
Write-Host "   - Ctrl+F5 (Firefox)" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Test login at: http://10.39.60.15:8090" -ForegroundColor Yellow
Write-Host ""
Write-Host "4. Open DevTools Console and verify:" -ForegroundColor Yellow
Write-Host "   - API calls should go to http://10.39.60.15:9090/api/*" -ForegroundColor Gray
Write-Host "   - NOT http://localhost:9090/api/*" -ForegroundColor Gray
Write-Host ""
