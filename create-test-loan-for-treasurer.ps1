# ============================================================================
# TEST LOAN CREATION SCRIPT - Complete Workflow to Treasurer
# ============================================================================
# This script creates a test loan that goes through all steps:
# 1. Apply for loan (Member)
# 2. Guarantors accept the loan
# 3. Loan reaches PENDING_TREASURER status
# 4. Treasurer gets notification
# ============================================================================

$API_BASE = "http://localhost:9090/api"
$ADMIN_USERNAME = "admin"
$ADMIN_PASSWORD = "admin123"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TEST LOAN CREATION - FULL WORKFLOW" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ============================================================================
# STEP 1: Login as Admin to get JWT Token
# ============================================================================
Write-Host "[1/8] Logging in as admin..." -ForegroundColor Yellow

$loginBody = @{
    username = $ADMIN_USERNAME
    password = $ADMIN_PASSWORD
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$API_BASE/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
    $JWT_TOKEN = $loginResponse.token
    Write-Host "Success! Login successful!" -ForegroundColor Green
    Write-Host "  Token: $($JWT_TOKEN.Substring(0, 20))..." -ForegroundColor Gray
}
catch {
    Write-Host "Error! Login failed: $_" -ForegroundColor Red
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $JWT_TOKEN"
    "Content-Type" = "application/json"
}

Write-Host ""

# ============================================================================
# STEP 2: Find or Verify Test Members
# ============================================================================
Write-Host "[2/8] Finding test members..." -ForegroundColor Yellow

try {
    $members = Invoke-RestMethod -Uri "$API_BASE/members" -Method Get -Headers $headers
    
    if ($members.Count -lt 3) {
        Write-Host "Error! Not enough members in database (need at least 3)" -ForegroundColor Red
        Write-Host "  Found: $($members.Count) members" -ForegroundColor Red
        exit 1
    }
    
    # Select loan applicant and 2 guarantors
    $loanApplicant = $members[0]
    $guarantor1 = $members[1]
    $guarantor2 = $members[2]
    
    Write-Host "Success! Test members found!" -ForegroundColor Green
    Write-Host "  Loan Applicant: $($loanApplicant.firstName) $($loanApplicant.lastName) (ID: $($loanApplicant.id))" -ForegroundColor Gray
    Write-Host "  Guarantor 1: $($guarantor1.firstName) $($guarantor1.lastName) (ID: $($guarantor1.id))" -ForegroundColor Gray
    Write-Host "  Guarantor 2: $($guarantor2.firstName) $($guarantor2.lastName) (ID: $($guarantor2.id))" -ForegroundColor Gray
}
catch {
    Write-Host "Error! Failed to fetch members: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# ============================================================================
# STEP 3: Get Available Loan Products
# ============================================================================
Write-Host "[3/8] Fetching loan products..." -ForegroundColor Yellow

try {
    $loanProducts = Invoke-RestMethod -Uri "$API_BASE/loan-products" -Method Get -Headers $headers
    
    if ($loanProducts.Count -eq 0) {
        Write-Host "Error! No loan products available" -ForegroundColor Red
        exit 1
    }
    
    $selectedProduct = $loanProducts[0]
    Write-Host "Success! Loan product selected!" -ForegroundColor Green
    Write-Host "  Product: $($selectedProduct.name)" -ForegroundColor Gray
    Write-Host "  Interest Rate: $($selectedProduct.interestRate)%" -ForegroundColor Gray
    Write-Host "  Max Amount: $($selectedProduct.maxAmount)" -ForegroundColor Gray
}
catch {
    Write-Host "Error! Failed to fetch loan products: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# ============================================================================
# STEP 4: Check Member Account Balances
# ============================================================================
Write-Host "[4/8] Checking account balances..." -ForegroundColor Yellow

try {
    $applicantAccounts = Invoke-RestMethod -Uri "$API_BASE/accounts/member/$($loanApplicant.id)" -Method Get -Headers $headers
    $savingsAccount = $applicantAccounts | Where-Object { $_.accountType -eq "SAVINGS" } | Select-Object -First 1
    
    if ($savingsAccount) {
        Write-Host "Success! Account balances retrieved!" -ForegroundColor Green
        Write-Host "  Savings Balance: KES $($savingsAccount.balance)" -ForegroundColor Gray
    }
    else {
        Write-Host "Warning! No savings account found for applicant" -ForegroundColor Yellow
    }
}
catch {
    Write-Host "Warning! Could not fetch account balances: $_" -ForegroundColor Yellow
}

Write-Host ""

# ============================================================================
# STEP 5: Apply for Loan
# ============================================================================
Write-Host "[5/8] Applying for loan..." -ForegroundColor Yellow

$loanAmount = 50000
$termMonths = 12
$guarantorAmount = [math]::Round($loanAmount / 2, 2)

$loanApplicationBody = @{
    memberId = $loanApplicant.id
    loanProductId = $selectedProduct.id
    amount = $loanAmount
    termMonths = $termMonths
    purpose = "Test loan for treasurer notification workflow"
    guarantors = @(
        @{
            guarantorMemberId = $guarantor1.id
            pledgedAmount = $guarantorAmount
        },
        @{
            guarantorMemberId = $guarantor2.id
            pledgedAmount = $guarantorAmount
        }
    )
} | ConvertTo-Json -Depth 10

try {
    $loanResponse = Invoke-RestMethod -Uri "$API_BASE/loans/apply" -Method Post -Headers $headers -Body $loanApplicationBody
    $loanId = $loanResponse.id
    Write-Host "Success! Loan application created!" -ForegroundColor Green
    Write-Host "  Loan ID: $loanId" -ForegroundColor Gray
    Write-Host "  Amount: KES $loanAmount" -ForegroundColor Gray
    Write-Host "  Term: $termMonths months" -ForegroundColor Gray
    Write-Host "  Status: $($loanResponse.status)" -ForegroundColor Gray
}
catch {
    Write-Host "Error! Loan application failed: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# ============================================================================
# STEP 6: Get Guarantor Records
# ============================================================================
Write-Host "[6/8] Fetching guarantor records..." -ForegroundColor Yellow

Start-Sleep -Seconds 2

try {
    $loanDetails = Invoke-RestMethod -Uri "$API_BASE/loans/$loanId" -Method Get -Headers $headers
    $guarantors = $loanDetails.guarantors
    
    if ($guarantors.Count -eq 0) {
        Write-Host "Error! No guarantors found for loan" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Success! Guarantor records retrieved!" -ForegroundColor Green
    foreach ($g in $guarantors) {
        Write-Host "  Guarantor ID: $($g.id), Status: $($g.status), Pledged: KES $($g.pledgedAmount)" -ForegroundColor Gray
    }
}
catch {
    Write-Host "Error! Failed to fetch guarantor records: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# ============================================================================
# STEP 7: Guarantors Accept the Loan
# ============================================================================
Write-Host "[7/8] Processing guarantor approvals..." -ForegroundColor Yellow

$allApproved = $true

foreach ($guarantor in $guarantors) {
    try {
        $approvalBody = @{
            guarantorId = $guarantor.id
            accepted = $true
            comments = "Test approval - automated workflow"
        } | ConvertTo-Json
        
        $approvalResponse = Invoke-RestMethod -Uri "$API_BASE/loans/guarantors/respond" -Method Post -Headers $headers -Body $approvalBody
        Write-Host "  Success! Guarantor $($guarantor.id) accepted the loan" -ForegroundColor Green
        Start-Sleep -Seconds 1
    }
    catch {
        Write-Host "  Error! Guarantor $($guarantor.id) approval failed: $_" -ForegroundColor Red
        $allApproved = $false
    }
}

if ($allApproved) {
    Write-Host "Success! All guarantors approved!" -ForegroundColor Green
}
else {
    Write-Host "Warning! Some guarantor approvals failed" -ForegroundColor Yellow
}

Write-Host ""

# ============================================================================
# STEP 8: Verify Loan Status - Should be PENDING_TREASURER
# ============================================================================
Write-Host "[8/8] Verifying loan status..." -ForegroundColor Yellow

Start-Sleep -Seconds 2

try {
    $finalLoanDetails = Invoke-RestMethod -Uri "$API_BASE/loans/$loanId" -Method Get -Headers $headers
    
    Write-Host "Success! Loan status retrieved!" -ForegroundColor Green
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  FINAL LOAN STATUS" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  Loan ID: $loanId" -ForegroundColor White
    Write-Host "  Loan Number: $($finalLoanDetails.loanNumber)" -ForegroundColor White
    
    $statusColor = if ($finalLoanDetails.status -eq "PENDING_TREASURER") { "Green" } else { "Yellow" }
    Write-Host "  Status: $($finalLoanDetails.status)" -ForegroundColor $statusColor
    
    Write-Host "  Applicant: $($loanApplicant.firstName) $($loanApplicant.lastName)" -ForegroundColor White
    Write-Host "  Amount: KES $($finalLoanDetails.amount)" -ForegroundColor White
    Write-Host "  Monthly Repayment: KES $($finalLoanDetails.monthlyRepayment)" -ForegroundColor White
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    if ($finalLoanDetails.status -eq "PENDING_TREASURER") {
        Write-Host "SUCCESS! The loan is now PENDING_TREASURER status." -ForegroundColor Green
        Write-Host "The treasurer should receive a notification!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Next Steps:" -ForegroundColor Yellow
        Write-Host "1. Login as treasurer to see the notification" -ForegroundColor White
        Write-Host "2. Check the loan approval queue" -ForegroundColor White
        Write-Host "3. Approve and disburse the loan if needed" -ForegroundColor White
    }
    else {
        Write-Host "WARNING: Loan status is $($finalLoanDetails.status)" -ForegroundColor Yellow
        Write-Host "Expected status: PENDING_TREASURER" -ForegroundColor Yellow
        Write-Host ""
        Write-Host "Possible reasons:" -ForegroundColor Yellow
        Write-Host "- Additional approval steps required (Loan Officer, Credit Committee)" -ForegroundColor White
        Write-Host "- Workflow configuration may require manual intervention" -ForegroundColor White
        Write-Host "- Check system logs for more details" -ForegroundColor White
    }
}
catch {
    Write-Host "Error! Failed to fetch final loan status: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Script completed!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
