# ========================================
# Deploy Multiple Next of Kin Feature
# ========================================

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  DEPLOYING NEXT OF KIN FEATURE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Build Backend
Write-Host "Step 1: Building Backend..." -ForegroundColor Yellow
cd backend
.\mvnw.cmd clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Backend build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Backend built successfully" -ForegroundColor Green
cd ..

# Step 2: Build Frontend
Write-Host ""
Write-Host "Step 2: Building Frontend..." -ForegroundColor Yellow
cd minetsacco-main
npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Frontend build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Frontend built successfully" -ForegroundColor Green
cd ..

# Step 3: Verify Database Migration
Write-Host ""
Write-Host "Step 3: Verifying Database Migration..." -ForegroundColor Yellow
$migrationExists = Test-Path "backend\src\main\resources\db\migration\V1000__Add_multiple_next_of_kin.sql"
if ($migrationExists) {
    Write-Host "✓ Flyway migration file exists" -ForegroundColor Green
} else {
    Write-Host "✗ Migration file missing!" -ForegroundColor Red
    exit 1
}

# Step 4: Test Database Connection
Write-Host ""
Write-Host "Step 4: Testing Database Connection..." -ForegroundColor Yellow
mysql -u minetsacco -p"0a0b0c0D." minetsacco -e "SELECT 1;" 2>&1 | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Database connection successful" -ForegroundColor Green
} else {
    Write-Host "✗ Database connection failed!" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  BUILD COMPLETE - READY FOR DEPLOYMENT" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Files Ready:" -ForegroundColor Cyan
Write-Host "  Backend JAR: backend\target\minet-sacco-backend-*.jar" -ForegroundColor White
Write-Host "  Frontend:    minetsacco-main\dist\" -ForegroundColor White
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Stop the running backend service" -ForegroundColor White
Write-Host "2. Copy backend JAR to production server" -ForegroundColor White
Write-Host "3. Copy frontend dist folder to web server" -ForegroundColor White
Write-Host "4. Restart services" -ForegroundColor White
Write-Host "5. Flyway will auto-run migration V1000 on startup" -ForegroundColor White
Write-Host ""
