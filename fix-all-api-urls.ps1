# Fix All Hardcoded API URLs Script

Write-Host "Fixing all hardcoded API URLs..." -ForegroundColor Cyan

$files = @(
    "minetsacco-main\src\components\DocumentUpload.tsx",
    "minetsacco-main\src\components\KycDocumentUpload.tsx",
    "minetsacco-main\src\pages\AuditReports.tsx",
    "minetsacco-main\src\pages\BulkProcessing.tsx",
    "minetsacco-main\src\pages\CustomerSupportPortal.tsx",
    "minetsacco-main\src\pages\FundConfiguration.tsx",
    "minetsacco-main\src\pages\GuarantorApprovals.tsx",
    "minetsacco-main\src\pages\KycApproval.tsx",
    "minetsacco-main\src\pages\KycDocumentUpload.tsx",
    "minetsacco-main\src\pages\KycUploadTracking.tsx",
    "minetsacco-main\src\pages\LoanEligibilityRules.tsx",
    "minetsacco-main\src\pages\LoanProducts.tsx",
    "minetsacco-main\src\pages\LoanRepaymentRecording.tsx",
    "minetsacco-main\src\pages\Members.tsx",
    "minetsacco-main\src\pages\MemberTransactionHistory.tsx",
    "minetsacco-main\src\pages\ProfitLossReport.tsx",
    "minetsacco-main\src\pages\Reports.tsx",
    "minetsacco-main\src\pages\Savings.tsx",
    "minetsacco-main\src\pages\Settings.tsx",
    "minetsacco-main\src\pages\TellerMemberContext.tsx",
    "minetsacco-main\src\pages\UserManagement.tsx",
    "minetsacco-main\src\pages\ViewMemberDocuments.tsx"
)

$fixedCount = 0

foreach ($file in $files) {
    $filePath = Join-Path $PSScriptRoot $file
    
    if (Test-Path $filePath) {
        Write-Host "Processing: $file" -ForegroundColor Yellow
        
        $content = Get-Content -Path $filePath -Raw
        
        # Check if needs fixing
        if ($content -match 'const API_BASE_URL = "http://localhost:9090/api";') {
            # Check if import already exists
            $hasImport = $content -match "import.*getApiBaseUrl.*from.*config/api"
            
            # Add import if needed
            if (-not $hasImport) {
                $content = $content -replace '(import[^\n]+\n)(\s*\n)', "`$1import { getApiBaseUrl } from '@/config/api';`n`$2"
            }
            
            # Replace hardcoded URL
            $content = $content -replace 'const API_BASE_URL = "http://localhost:9090/api";', 'const API_BASE_URL = getApiBaseUrl();'
            
            # Save file
            Set-Content -Path $filePath -Value $content -NoNewline
            
            Write-Host "  Fixed!" -ForegroundColor Green
            $fixedCount++
        } else {
            Write-Host "  Skipped (already fixed)" -ForegroundColor Gray
        }
    }
}

Write-Host ""
Write-Host "Fixed $fixedCount files" -ForegroundColor Green
Write-Host "Next: npm run build" -ForegroundColor Yellow
