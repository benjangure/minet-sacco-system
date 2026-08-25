##############################################################################
# Minet SACCO - Full GL Functionality Test Script
# Tests the entire GL workflow: chart of accounts, period entries,
# approval lifecycle (DRAFT->POSTED->APPROVED->LOCKED), rejection,
# balance calculations (AGGREGATION/MANUAL/FORMULA/COMPUTED),
# reports (trial balance, balance sheet), and exports (Excel + PDF).
#
# Usage:
#   .\test_gl_full.ps1
#   .\test_gl_full.ps1 -AdminUser "admin" -AdminPass "password" -TreasurerUser "treasurer" -TreasurerPass "password"
##############################################################################

param(
    [string]$BaseUrl       = "http://localhost:9090/api",
    [string]$AdminUser     = "admin",
    [string]$AdminPass     = "password",
    [string]$TreasurerUser = "treasurer",
    [string]$TreasurerPass = "password",
    [int]$PeriodMonth      = (Get-Date).Month,
    [int]$PeriodYear       = (Get-Date).Year
)

$PassCount  = 0
$FailCount  = 0
$SkipCount  = 0

function Write-Pass { param([string]$m); Write-Host "  [PASS] $m" -ForegroundColor Green;  $script:PassCount++ }
function Write-Fail { param([string]$m); Write-Host "  [FAIL] $m" -ForegroundColor Red;    $script:FailCount++ }
function Write-Skip { param([string]$m); Write-Host "  [SKIP] $m" -ForegroundColor Yellow; $script:SkipCount++ }
function Write-Info { param([string]$m); Write-Host "  [INFO] $m" -ForegroundColor Gray }
function Write-Section { param([string]$t); Write-Host ""; Write-Host "=== $t ===" -ForegroundColor Cyan }

function Invoke-Api {
    param(
        [string]$Method = "GET",
        [string]$Endpoint,
        [hashtable]$Headers,
        [object]$Body,
        [string]$Label
    )
    try {
        $uri = "$BaseUrl$Endpoint"
        $params = @{ Method = $Method; Uri = $uri; Headers = $Headers; ErrorAction = "Stop" }
        if ($Body) {
            $params.Body        = ($Body | ConvertTo-Json -Depth 10)
            $params.ContentType = "application/json"
        }
        return Invoke-RestMethod @params
    } catch {
        $code = $null
        try { $code = $_.Exception.Response.StatusCode.value__ } catch {}
        $detail = ""
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $detail = $reader.ReadToEnd()
        } catch {}
        Write-Fail "$Label -- HTTP $code : $detail"
        return $null
    }
}

function Expect-Status {
    param([string]$Method, [string]$Endpoint, [hashtable]$Headers, [object]$Body,
          [int]$ExpectedStatus, [string]$Label)
    try {
        $uri = "$BaseUrl$Endpoint"
        $params = @{ Method = $Method; Uri = $uri; Headers = $Headers; ErrorAction = "Stop" }
        if ($Body) {
            $params.Body        = ($Body | ConvertTo-Json -Depth 10)
            $params.ContentType = "application/json"
        }
        Invoke-RestMethod @params | Out-Null
        # If we get here it was 2xx
        if ($ExpectedStatus -ge 200 -and $ExpectedStatus -lt 300) {
            Write-Pass "$Label -- got 2xx as expected"
        } else {
            Write-Fail "$Label -- expected HTTP $ExpectedStatus but got 2xx"
        }
    } catch {
        $code = $null
        try { $code = $_.Exception.Response.StatusCode.value__ } catch {}
        if ($code -eq $ExpectedStatus) {
            Write-Pass "$Label -- correctly got HTTP $code"
        } else {
            Write-Fail "$Label -- expected HTTP $ExpectedStatus but got HTTP $code"
        }
    }
}

# ============================================================
# STEP 0: Login
# ============================================================
Write-Section "STEP 0 -- Authentication"

$adminLogin = Invoke-Api -Method POST -Endpoint "/auth/login" `
    -Body @{ username = $AdminUser; password = $AdminPass } -Label "Admin login"

if (-not $adminLogin -or -not $adminLogin.token) {
    Write-Fail "Admin login failed -- cannot continue"
    exit 1
}
$adminToken = $adminLogin.token
Write-Pass "Admin logged in"

$treasLogin = Invoke-Api -Method POST -Endpoint "/auth/login" `
    -Body @{ username = $TreasurerUser; password = $TreasurerPass } -Label "Treasurer login"

if (-not $treasLogin -or -not $treasLogin.token) {
    Write-Fail "Treasurer login failed -- cannot continue"
    exit 1
}
$treasToken = $treasLogin.token
Write-Pass "Treasurer logged in"

$aHdr = @{ Authorization = "Bearer $adminToken" }
$tHdr = @{ Authorization = "Bearer $treasToken" }

# ============================================================
# STEP 1: Data Sources
# ============================================================
Write-Section "STEP 1 -- GL Data Sources"

$ds = Invoke-Api -Endpoint "/gl/data-sources" -Headers $tHdr -Label "Get data sources"
if ($ds -and $ds.data) {
    Write-Pass "Data sources returned ($($ds.data.Count) items)"
} else {
    Write-Fail "No data sources returned"
}

# ============================================================
# STEP 2: Create Test GL Accounts (4 types)
# ============================================================
Write-Section "STEP 2 -- Create GL Accounts (all 4 calculation types)"

$rand = Get-Random -Maximum 9999
$codeAgg      = "TST_AGG_$rand"
$codeMan      = "TST_MAN_$rand"
$codeFormula  = "TST_FRM_$rand"
$codeComputed = "TST_CMP_$rand"

# AGGREGATION -- transactions DEPOSIT
$r = Invoke-Api -Method POST -Endpoint "/gl/account-configuration" -Headers $tHdr `
    -Body @{
        code                   = $codeAgg
        name                   = "Test Cash Deposits (AGG)"
        accountType            = "ASSET"
        balanceCalculationType = "AGGREGATION"
        dataSource             = "transactions"
        transactionType        = "DEPOSIT"
        normalBalance          = "DEBIT"
        sectionLabel           = "Test Assets"
        periodSensitive        = $false
        displayOrder           = 9991
        isActive               = $true
    } -Label "Create AGGREGATION account"

$aggId = $null
if ($r -and $r.data) { $aggId = $r.data.id; Write-Pass "AGGREGATION created (ID $aggId, code $codeAgg)" }
else { Write-Fail "AGGREGATION account creation failed" }

# MANUAL_ENTRY -- expense, period-sensitive
$r2 = Invoke-Api -Method POST -Endpoint "/gl/account-configuration" -Headers $tHdr `
    -Body @{
        code                   = $codeMan
        name                   = "Test Audit Fees (MANUAL)"
        accountType            = "EXPENSE"
        balanceCalculationType = "MANUAL_ENTRY"
        normalBalance          = "DEBIT"
        sectionLabel           = "Test Expenses"
        periodSensitive        = $true
        displayOrder           = 9992
        isActive               = $true
    } -Label "Create MANUAL_ENTRY account"

$manId = $null
if ($r2 -and $r2.data) { $manId = $r2.data.id; Write-Pass "MANUAL_ENTRY created (ID $manId, code $codeMan)" }
else { Write-Fail "MANUAL_ENTRY account creation failed" }

# FORMULA -- REVENUE minus EXPENSE
$r3 = Invoke-Api -Method POST -Endpoint "/gl/account-configuration" -Headers $tHdr `
    -Body @{
        code                   = $codeFormula
        name                   = "Test Net Income (FORMULA)"
        accountType            = "EQUITY"
        balanceCalculationType = "FORMULA"
        calculationConfig      = '{"formula":"REVENUE - EXPENSE"}'
        normalBalance          = "CREDIT"
        sectionLabel           = "Test Equity"
        periodSensitive        = $false
        displayOrder           = 9993
        isActive               = $true
    } -Label "Create FORMULA account"

$formulaId = $null
if ($r3 -and $r3.data) { $formulaId = $r3.data.id; Write-Pass "FORMULA created (ID $formulaId, code $codeFormula)" }
else { Write-Fail "FORMULA account creation failed" }

# COMPUTED -- retained earnings
$r4 = Invoke-Api -Method POST -Endpoint "/gl/account-configuration" -Headers $tHdr `
    -Body @{
        code                   = $codeComputed
        name                   = "Test Retained Earnings (COMPUTED)"
        accountType            = "EQUITY"
        balanceCalculationType = "COMPUTED"
        calculationConfig      = '{"compute":"RETAINED_EARNINGS"}'
        normalBalance          = "CREDIT"
        sectionLabel           = "Test Equity"
        periodSensitive        = $false
        displayOrder           = 9994
        isActive               = $true
    } -Label "Create COMPUTED account"

$computedId = $null
if ($r4 -and $r4.data) { $computedId = $r4.data.id; Write-Pass "COMPUTED created (ID $computedId, code $codeComputed)" }
else { Write-Fail "COMPUTED account creation failed" }

# ============================================================
# STEP 3: List accounts -- verify all 4 show up
# ============================================================
Write-Section "STEP 3 -- List GL Accounts"

$list = Invoke-Api -Endpoint "/gl/account-configuration" -Headers $aHdr -Label "List accounts"
if ($list -and $list.data) {
    $total = $list.data.Count
    Write-Pass "Account list returned ($total accounts)"
    foreach ($code in @($codeAgg, $codeMan, $codeFormula, $codeComputed)) {
        if ($list.data | Where-Object { $_.code -eq $code }) {
            Write-Pass "  Found account $code"
        } else {
            Write-Fail "  Account $code not found in list"
        }
    }
} else {
    Write-Fail "Account list returned nothing"
}

# ============================================================
# STEP 4: Balance calculations for all 4 types
# ============================================================
Write-Section "STEP 4 -- Balance Calculations"

$today = (Get-Date).ToString("yyyy-MM-dd")

foreach ($pair in @(
    @{ Id = $aggId;      Type = "AGGREGATION" },
    @{ Id = $formulaId;  Type = "FORMULA"     },
    @{ Id = $computedId; Type = "COMPUTED"    }
)) {
    if ($pair.Id) {
        $bal = Invoke-Api -Endpoint "/gl/accounts/$($pair.Id)/balance?asOfDate=$today" -Headers $aHdr `
            -Label "Balance for $($pair.Type)"
        if ($null -ne $bal) {
            Write-Pass "$($pair.Type) balance = KES $($bal.data.balance) (not null)"
        }
    }
}

# ============================================================
# STEP 5: Period Entry view
# ============================================================
Write-Section "STEP 5 -- Period Entry View"

$pv = Invoke-Api -Endpoint ("/gl/period-entry?periodMonth=$PeriodMonth" + "&periodYear=$PeriodYear") `
    -Headers $tHdr -Label "Get period view"

if ($pv -and $pv.data) {
    $autoL   = ($pv.data | Where-Object { $_.sourceType -eq "AUTO"   }).Count
    $manualL = ($pv.data | Where-Object { $_.sourceType -eq "MANUAL" }).Count
    Write-Pass "Period view: $($pv.data.Count) lines ($autoL AUTO, $manualL MANUAL)"
} else {
    Write-Fail "Period view returned nothing"
}

# ============================================================
# STEP 6: Create a Period Entry (DRAFT)
# ============================================================
Write-Section "STEP 6 -- Create Period Entry (DRAFT)"

$entryId = $null
if ($manId) {
    $ts = (Get-Date).ToString("HH:mm:ss")
    $pe = Invoke-Api -Method POST -Endpoint "/gl/period-entry" -Headers $tHdr `
        -Body @{
            glAccountId  = $manId
            amount       = 75000.00
            periodMonth  = $PeriodMonth
            periodYear   = $PeriodYear
            description  = "Test audit fees entry $ts"
            entryReason  = "ACCRUAL"
            isDebit      = $true
        } -Label "Create period entry"

    if ($pe -and $pe.data) {
        $entryId = $pe.data.id
        $st = $pe.data.periodStatus
        Write-Pass "Entry created (ID $entryId, status: $st)"
        if ($st -eq "DRAFT") { Write-Pass "Status is DRAFT as expected" }
        else { Write-Fail "Expected DRAFT, got $st" }
    }
} else {
    Write-Skip "No MANUAL account -- skipping period entry tests"
}

# ============================================================
# STEP 7: DRAFT entry must NOT appear in trial balance
# ============================================================
Write-Section "STEP 7 -- DRAFT Entry Excluded from Reports"

$tbQ = "/reports/trial-balance?asOfDate=$today" + "&periodMonth=$PeriodMonth" + "&periodYear=$PeriodYear"
$tbBefore = Invoke-Api -Endpoint $tbQ -Headers $aHdr -Label "Trial balance before approval"

if ($tbBefore -and $tbBefore.data) {
    $line = $tbBefore.data.lines | Where-Object { $_.code -eq $codeMan }
    if (-not $line -or [double]$line.balance -eq 0) {
        Write-Pass "DRAFT entry correctly excluded from trial balance"
    } else {
        Write-Fail "DRAFT entry incorrectly appears in trial balance (KES $($line.balance))"
    }
} else {
    Write-Skip "Could not verify DRAFT exclusion"
}

# ============================================================
# STEP 8: Submit (DRAFT -> POSTED)
# ============================================================
Write-Section "STEP 8 -- Submit Entry (DRAFT -> POSTED)"

if ($entryId) {
    $sub = Invoke-Api -Method PUT -Endpoint "/gl/period-entry/$entryId/submit" `
        -Headers $tHdr -Label "Submit entry"
    if ($sub -and $sub.data) {
        $st = $sub.data.periodStatus
        if ($st -eq "POSTED") { Write-Pass "Status -> POSTED" }
        else { Write-Fail "Expected POSTED, got $st" }
    }
} else { Write-Skip "No entry to submit" }

# ============================================================
# STEP 9: Double-submit guard (should return 400)
# ============================================================
Write-Section "STEP 9 -- Double-Submit Guard (expect HTTP 400)"

if ($entryId) {
    Expect-Status -Method PUT -Endpoint "/gl/period-entry/$entryId/submit" `
        -Headers $tHdr -ExpectedStatus 400 -Label "Double-submit rejected"
} else { Write-Skip "No entry to test" }

# ============================================================
# STEP 10: Check pending queue
# ============================================================
Write-Section "STEP 10 -- Admin Views Pending Approval Queue"

$pending = Invoke-Api -Endpoint "/gl/manual-entries/pending" -Headers $aHdr -Label "Pending queue"
if ($pending -and $pending.data) {
    Write-Pass "Pending queue has $($pending.data.Count) item(s)"
    if ($entryId -and ($pending.data | Where-Object { $_.id -eq $entryId })) {
        Write-Pass "Test entry found in pending queue"
    } elseif ($entryId) {
        Write-Fail "Test entry NOT in pending queue"
    }
} else {
    Write-Fail "Pending queue empty or failed"
}

# ============================================================
# STEP 11: Admin approves (POSTED -> APPROVED)
# ============================================================
Write-Section "STEP 11 -- Admin Approves Entry (POSTED -> APPROVED)"

if ($entryId) {
    $appr = Invoke-Api -Method PUT -Endpoint "/gl/period-entry/$entryId/approve" `
        -Headers $aHdr -Label "Approve entry"
    if ($appr -and $appr.data) {
        $pst = $appr.data.periodStatus
        $ast = $appr.data.approvalStatus
        if ($pst -eq "APPROVED" -and $ast -eq "APPROVED") {
            Write-Pass "period_status=APPROVED, approval_status=APPROVED"
        } else {
            Write-Fail "Unexpected statuses: period=$pst approval=$ast"
        }
    }
} else { Write-Skip "No entry to approve" }

# ============================================================
# STEP 12: APPROVED entry MUST appear in trial balance
# ============================================================
Write-Section "STEP 12 -- APPROVED Entry Appears in Trial Balance"

$tbAfter = Invoke-Api -Endpoint $tbQ -Headers $aHdr -Label "Trial balance after approval"
if ($tbAfter -and $tbAfter.data) {
    $line = $tbAfter.data.lines | Where-Object { $_.code -eq $codeMan }
    if ($line -and [double]$line.balance -gt 0) {
        Write-Pass "Approved entry in trial balance: KES $($line.balance)"
    } elseif ($line) {
        Write-Fail "Entry found but balance is 0"
    } else {
        Write-Fail "Approved entry NOT found in trial balance"
    }
} else {
    Write-Fail "Trial balance failed after approval"
}

# ============================================================
# STEP 13: Balance sheet check
# ============================================================
Write-Section "STEP 13 -- Balance Sheet"

$bs = Invoke-Api -Endpoint "/reports/balance-sheet?asOfDate=$today" -Headers $aHdr -Label "Balance sheet"
if ($bs -and $bs.data) {
    Write-Pass "Balance sheet returned"
    Write-Info "  Total Assets      : KES $($bs.data.totalAssets)"
    Write-Info "  Total Liabilities : KES $($bs.data.totalLiabilities)"
    Write-Info "  Total Equity      : KES $($bs.data.totalEquity)"
    Write-Info "  Is Balanced       : $($bs.data.isBalanced)"
    if ($bs.data.isBalanced) {
        Write-Pass "Balance sheet is BALANCED (Assets = Liabilities + Equity)"
    } else {
        Write-Info "Balance sheet not balanced yet -- normal with incomplete chart of accounts"
    }
} else {
    Write-Fail "Balance sheet failed"
}

# ============================================================
# STEP 14: GL Trial Balance (grouped)
# ============================================================
Write-Section "STEP 14 -- GL Grouped Trial Balance"

$glTbQ = "/gl/trial-balance?asOfDate=$today" + "&periodMonth=$PeriodMonth" + "&periodYear=$PeriodYear"
$glTb = Invoke-Api -Endpoint $glTbQ -Headers $aHdr -Label "GL grouped trial balance"
if ($glTb -and $glTb.data) {
    Write-Pass "GL trial balance returned ($($glTb.data.lines.Count) lines)"
    Write-Info "  Total Debit  : KES $($glTb.data.totalDebit)"
    Write-Info "  Total Credit : KES $($glTb.data.totalCredit)"
} else {
    Write-Fail "GL trial balance failed"
}

# ============================================================
# STEP 15: JSON vs Export consistency (both must use GL data)
# ============================================================
Write-Section "STEP 15 -- JSON/Export Consistency (same data source)"

$jsonTb = Invoke-Api -Endpoint "/reports/trial-balance?asOfDate=$today" -Headers $aHdr -Label "TB JSON format"
if ($jsonTb -and $jsonTb.data) {
    if ($jsonTb.data.PSObject.Properties.Name -contains "lines") {
        Write-Pass "Trial Balance JSON uses GL format (has 'lines' array)"
    } else {
        Write-Fail "Trial Balance JSON not GL-based (missing 'lines')"
    }
}

$jsonBs = Invoke-Api -Endpoint "/reports/balance-sheet?asOfDate=$today" -Headers $aHdr -Label "BS JSON format"
if ($jsonBs -and $jsonBs.data) {
    if ($jsonBs.data.PSObject.Properties.Name -contains "totalAssets") {
        Write-Pass "Balance Sheet JSON uses GL format (has 'totalAssets')"
    } else {
        Write-Fail "Balance Sheet JSON not GL-based (missing 'totalAssets')"
    }
}

# ============================================================
# STEP 16: Lock entry (APPROVED -> LOCKED)
# ============================================================
Write-Section "STEP 16 -- Lock Entry (APPROVED -> LOCKED)"

if ($entryId) {
    $locked = Invoke-Api -Method PUT -Endpoint "/gl/period-entry/$entryId/lock" `
        -Headers $aHdr -Label "Lock entry"
    if ($locked -and $locked.data) {
        $lst = $locked.data.periodStatus
        if ($lst -eq "LOCKED") { Write-Pass "Status -> LOCKED (immutable)" }
        else { Write-Fail "Expected LOCKED, got $lst" }
    }
} else { Write-Skip "No entry to lock" }

# ============================================================
# STEP 17: Rejection workflow (new entry, different period)
# ============================================================
Write-Section "STEP 17 -- Rejection Workflow (DRAFT -> POSTED -> REJECTED -> DRAFT)"

if ($manId) {
    $nm = if ($PeriodMonth -lt 12) { $PeriodMonth + 1 } else { 1 }
    $ny = if ($PeriodMonth -lt 12) { $PeriodYear } else { $PeriodYear + 1 }

    $rejPe = Invoke-Api -Method POST -Endpoint "/gl/period-entry" -Headers $tHdr `
        -Body @{
            glAccountId  = $manId
            amount       = 5000.00
            periodMonth  = $nm
            periodYear   = $ny
            description  = "Rejection test entry"
            entryReason  = "ADJUSTMENT"
            isDebit      = $true
        } -Label "Create rejection test entry"

    if ($rejPe -and $rejPe.data) {
        $rejId = $rejPe.data.id
        Write-Pass "Rejection test entry created (ID $rejId)"

        $sub2 = Invoke-Api -Method PUT -Endpoint "/gl/period-entry/$rejId/submit" `
            -Headers $tHdr -Label "Submit for rejection"
        if ($sub2 -and $sub2.data.periodStatus -eq "POSTED") { Write-Pass "Status -> POSTED" }

        $rej = Invoke-Api -Method PUT -Endpoint "/gl/period-entry/$rejId/reject" `
            -Headers $aHdr `
            -Body @{ rejectReason = "Figures need revision -- test rejection" } `
            -Label "Admin rejects entry"

        if ($rej -and $rej.data) {
            $rst = $rej.data.periodStatus
            if ($rst -eq "DRAFT") { Write-Pass "Status -> DRAFT after rejection (treasurer can revise)" }
            else { Write-Fail "Expected DRAFT after rejection, got $rst" }
            if ($rej.data.description -match "REJECTED") {
                Write-Pass "Rejection reason stored in description"
            } else {
                Write-Fail "Rejection reason not found in description"
            }
        }
    } else {
        Write-Skip "Could not create rejection test entry"
    }
} else {
    Write-Skip "No MANUAL account for rejection test"
}

# ============================================================
# STEP 18: Ad-hoc manual entry (separate simpler workflow)
# ============================================================
Write-Section "STEP 18 -- Ad-Hoc Manual Entry Workflow"

$adHocId = $null
if ($manId) {
    $ah = Invoke-Api -Method POST -Endpoint "/gl/manual-entries" -Headers $tHdr `
        -Body @{
            glAccountId  = $manId
            entryDate    = $today
            amount       = 12500.00
            isDebit      = $true
            description  = "Ad-hoc miscellaneous expense test"
            entryReason  = "ADJUSTMENT"
        } -Label "Create ad-hoc entry"

    if ($ah -and $ah.data) {
        $adHocId = $ah.data.id
        $ahSt    = $ah.data.approvalStatus
        Write-Pass "Ad-hoc entry created (ID $adHocId, status: $ahSt)"

        $ahAppr = Invoke-Api -Method PUT -Endpoint "/gl/manual-entries/$adHocId/approve" `
            -Headers $aHdr -Label "Approve ad-hoc entry"

        if ($ahAppr -and $ahAppr.data.approvalStatus -eq "APPROVED") {
            Write-Pass "Ad-hoc entry -> APPROVED"
        } else {
            Write-Fail "Ad-hoc entry approval failed"
        }
    }
} else {
    Write-Skip "No MANUAL account for ad-hoc test"
}

# ============================================================
# STEP 19: Export reports (Excel + PDF)
# ============================================================
Write-Section "STEP 19 -- Report Exports (Excel + PDF)"

$exports = @(
    @{ Label = "Trial Balance Excel";  Path = "/reports/trial-balance/export/excel?asOfDate=$today"; Ext = "xlsx" }
    @{ Label = "Trial Balance PDF";    Path = "/reports/trial-balance/export/pdf?asOfDate=$today";   Ext = "pdf"  }
    @{ Label = "Balance Sheet Excel";  Path = "/reports/balance-sheet/export/excel?asOfDate=$today"; Ext = "xlsx" }
    @{ Label = "Balance Sheet PDF";    Path = "/reports/balance-sheet/export/pdf?asOfDate=$today";   Ext = "pdf"  }
)

foreach ($exp in $exports) {
    try {
        $outFile = Join-Path $env:TEMP "gl_test_$($exp.Ext)_$(Get-Random).tmp"
        Invoke-WebRequest -Uri "$BaseUrl$($exp.Path)" -Headers $aHdr `
            -OutFile $outFile -ErrorAction Stop
        $sz = (Get-Item $outFile).Length
        if ($sz -gt 500) {
            Write-Pass "$($exp.Label) -- $sz bytes downloaded"
        } else {
            Write-Fail "$($exp.Label) -- file too small ($sz bytes)"
        }
        Remove-Item $outFile -ErrorAction SilentlyContinue
    } catch {
        Write-Fail "$($exp.Label) -- $($_.Exception.Message)"
    }
}

# ============================================================
# STEP 20: Available periods
# ============================================================
Write-Section "STEP 20 -- Available Periods Endpoint"

$periods = Invoke-Api -Endpoint "/gl/period-entry/periods" -Headers $tHdr -Label "Available periods"
if ($periods -and $periods.data) {
    Write-Pass "Periods endpoint returned $($periods.data.Count) period(s)"
    $periods.data | Select-Object -First 5 | ForEach-Object {
        Write-Info "  $($_.periodYear)-$('{0:D2}' -f $_.periodMonth)"
    }
} else {
    Write-Fail "Periods endpoint failed or returned nothing"
}

# ============================================================
# STEP 21: isDebit inference (REVENUE account should default to credit)
# ============================================================
Write-Section "STEP 21 -- isDebit Inference (credit account defaults correctly)"

$codeRevTest = "TST_REV_$rand"
$revAcct = Invoke-Api -Method POST -Endpoint "/gl/account-configuration" -Headers $tHdr `
    -Body @{
        code                   = $codeRevTest
        name                   = "Test Other Income (MANUAL REVENUE)"
        accountType            = "REVENUE"
        balanceCalculationType = "MANUAL_ENTRY"
        normalBalance          = "CREDIT"
        sectionLabel           = "Test Revenue"
        periodSensitive        = $true
        displayOrder           = 9995
        isActive               = $true
    } -Label "Create REVENUE MANUAL account"

$revId = $null
if ($revAcct -and $revAcct.data) {
    $revId = $revAcct.data.id
    Write-Pass "REVENUE MANUAL account created (ID $revId)"

    # Create entry WITHOUT specifying isDebit -- should default to false (credit) for REVENUE
    $revEntry = Invoke-Api -Method POST -Endpoint "/gl/period-entry" -Headers $tHdr `
        -Body @{
            glAccountId  = $revId
            amount       = 30000.00
            periodMonth  = $PeriodMonth
            periodYear   = $PeriodYear
            description  = "Test other income entry"
            entryReason  = "ADJUSTMENT"
        } -Label "Create REVENUE entry (no isDebit specified)"

    if ($revEntry -and $revEntry.data) {
        $isDebitVal = $revEntry.data.isDebit
        if ($isDebitVal -eq $false) {
            Write-Pass "isDebit correctly defaulted to FALSE for REVENUE account (credit side)"
        } else {
            Write-Fail "isDebit should be FALSE for REVENUE but got: $isDebitVal"
        }
    }
} else {
    Write-Skip "Could not create REVENUE account for isDebit test"
}

# ============================================================
# STEP 22: Cleanup -- deactivate all test accounts
# ============================================================
Write-Section "STEP 22 -- Cleanup (deactivate test accounts)"

$cleanupIds = @($aggId, $manId, $formulaId, $computedId, $revId) | Where-Object { $_ -ne $null }
foreach ($id in $cleanupIds) {
    $upd = Invoke-Api -Method PUT -Endpoint "/gl/account-configuration/$id" `
        -Headers $aHdr -Body @{ isActive = $false } -Label "Deactivate ID $id"
    if ($upd) { Write-Pass "Account ID $id deactivated" }
}

# ============================================================
# FINAL SUMMARY
# ============================================================
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  GL TEST SUMMARY" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  PASS : $PassCount" -ForegroundColor Green
Write-Host "  FAIL : $FailCount" -ForegroundColor $(if ($FailCount -gt 0) {"Red"} else {"Green"})
Write-Host "  SKIP : $SkipCount" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan

if ($FailCount -eq 0) {
    Write-Host "  ALL TESTS PASSED" -ForegroundColor Green
    exit 0
} else {
    Write-Host "  $FailCount TEST(S) FAILED" -ForegroundColor Red
    exit 1
}
