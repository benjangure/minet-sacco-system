# ============================================================================
# ELIGIBILITY CALCULATION DIAGNOSTIC SCRIPT (PowerShell)
# ============================================================================
# This script runs the eligibility diagnostic SQL against the database
# and shows all the data needed to understand eligibility calculations.
#
# Usage: .\run_eligibility_debug.ps1 -MemberId 101
# ============================================================================

param(
    [Parameter(Mandatory=$true)]
    [int]$MemberId
)

# Load environment variables from .env
$envFile = ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match '^\s*([^#][^=]*)\s*=\s*(.*)$') {
            $key = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($key, $value)
        }
    }
}

# Get database configuration from environment or use defaults
$dbHost = $env:DB_HOST -or "localhost"
$dbPort = $env:DB_PORT -or "3306"
$dbName = $env:DB_NAME -or "minet_sacco"
$dbUser = $env:DB_USER -or "root"
$dbPassword = $env:DB_PASSWORD -or ""

Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host "ELIGIBILITY CALCULATION DIAGNOSTIC FOR MEMBER $MemberId" -ForegroundColor Cyan
Write-Host "============================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Database: $dbName"
Write-Host "Host: $dbHost`:$dbPort"
Write-Host "User: $dbUser"
Write-Host ""

# Build the SQL query
$sqlQuery = @"
SET @member_id = $MemberId;

-- ============================================================================
-- SECTION 1: MEMBER BASIC INFO
-- ============================================================================
SELECT '=== MEMBER BASIC INFO ===' as section;
SELECT 
    m.id,
    m.first_name,
    m.last_name,
    m.member_number,
    m.status,
    m.created_at
FROM members m
WHERE m.id = @member_id;

-- ============================================================================
-- SECTION 2: SAVINGS ACCOUNT
-- ============================================================================
SELECT '' as '';
SELECT '=== SAVINGS ACCOUNT ===' as section;
SELECT 
    a.id,
    a.member_id,
    a.account_type,
    a.balance,
    a.frozen_savings,
    a.created_at,
    a.updated_at
FROM accounts a
WHERE a.member_id = @member_id 
  AND a.account_type = 'SAVINGS';

-- ============================================================================
-- SECTION 3: ALL LOANS WHERE MEMBER IS BORROWER
-- ============================================================================
SELECT '' as '';
SELECT '=== LOANS WHERE MEMBER IS BORROWER ===' as section;
SELECT 
    l.id,
    l.member_id,
    l.amount,
    l.original_principal,
    l.status,
    l.outstanding_balance,
    l.total_repayable,
    l.created_at
FROM loans l
WHERE l.member_id = @member_id
ORDER BY l.id;

-- ============================================================================
-- SECTION 4: GUARANTORS FOR EACH LOAN (WHERE MEMBER IS BORROWER)
-- ============================================================================
SELECT '' as '';
SELECT '=== GUARANTORS FOR MEMBER''S LOANS ===' as section;
SELECT 
    g.id,
    g.loan_id,
    g.member_id as guarantor_member_id,
    m.member_number as guarantor_member_number,
    g.is_self_guarantee,
    g.guarantee_amount,
    g.pledge_amount,
    g.status,
    l.amount as loan_amount,
    l.outstanding_balance as loan_outstanding
FROM guarantors g
JOIN loans l ON g.loan_id = l.id
JOIN members m ON g.member_id = m.id
WHERE l.member_id = @member_id
ORDER BY l.id, g.id;

-- ============================================================================
-- SECTION 5: GUARANTOR PLEDGES (WHERE MEMBER IS GUARANTOR ON OTHER LOANS)
-- ============================================================================
SELECT '' as '';
SELECT '=== GUARANTOR PLEDGES (MEMBER AS GUARANTOR ON OTHER LOANS) ===' as section;
SELECT 
    g.id,
    g.loan_id,
    g.member_id as guarantor_member_id,
    g.is_self_guarantee,
    g.guarantee_amount,
    g.pledge_amount,
    g.status,
    l.member_id as borrower_member_id,
    bm.member_number as borrower_member_number,
    l.amount as loan_amount,
    l.outstanding_balance as loan_outstanding
FROM guarantors g
JOIN loans l ON g.loan_id = l.id
JOIN members bm ON l.member_id = bm.id
WHERE g.member_id = @member_id
  AND l.member_id != @member_id
ORDER BY l.id, g.id;

-- ============================================================================
-- SECTION 6: CALCULATION BREAKDOWN
-- ============================================================================
SELECT '' as '';
SELECT '=== CALCULATION BREAKDOWN ===' as section;

-- Get savings account balance
SELECT 
    'Savings Account Balance' as metric,
    COALESCE(a.balance, 0) as value
FROM accounts a
WHERE a.member_id = @member_id 
  AND a.account_type = 'SAVINGS'
UNION ALL

-- Get frozen savings from account table
SELECT 
    'Frozen Savings (from account table)',
    COALESCE(a.frozen_savings, 0)
FROM accounts a
WHERE a.member_id = @member_id 
  AND a.account_type = 'SAVINGS'
UNION ALL

-- Calculate frozen from self-guarantee loans
SELECT 
    'Frozen from Self-Guarantee Loans',
    COALESCE(SUM(g.pledge_amount), 0)
FROM guarantors g
JOIN loans l ON g.loan_id = l.id
WHERE l.member_id = @member_id
  AND g.is_self_guarantee = 1
  AND g.member_id = @member_id
  AND l.status IN ('DISBURSED', 'REPAID')
UNION ALL

-- Calculate frozen from guarantor pledges on other loans
SELECT 
    'Frozen from Guarantor Pledges (other loans)',
    COALESCE(SUM(g.pledge_amount), 0)
FROM guarantors g
JOIN loans l ON g.loan_id = l.id
WHERE g.member_id = @member_id
  AND l.member_id != @member_id
  AND l.status IN ('DISBURSED', 'REPAID')
UNION ALL

-- Calculate total frozen (both types)
SELECT 
    'Total Frozen (Self + Guarantor)',
    COALESCE(SUM(g.pledge_amount), 0)
FROM guarantors g
JOIN loans l ON g.loan_id = l.id
WHERE g.member_id = @member_id
  AND l.status IN ('DISBURSED', 'REPAID')
UNION ALL

-- Calculate true savings
SELECT 
    'True Savings (Balance - Frozen Self-Guarantee)',
    COALESCE(a.balance, 0) - COALESCE(
        (SELECT SUM(g.pledge_amount)
         FROM guarantors g
         JOIN loans l ON g.loan_id = l.id
         WHERE l.member_id = @member_id
           AND g.is_self_guarantee = 1
           AND g.member_id = @member_id
           AND l.status IN ('DISBURSED', 'REPAID')), 0)
FROM accounts a
WHERE a.member_id = @member_id 
  AND a.account_type = 'SAVINGS'
UNION ALL

-- Calculate gross eligibility
SELECT 
    'Gross Eligibility (True Savings × 3)',
    (COALESCE(a.balance, 0) - COALESCE(
        (SELECT SUM(g.pledge_amount)
         FROM guarantors g
         JOIN loans l ON g.loan_id = l.id
         WHERE l.member_id = @member_id
           AND g.is_self_guarantee = 1
           AND g.member_id = @member_id
           AND l.status IN ('DISBURSED', 'REPAID')), 0)) * 3
FROM accounts a
WHERE a.member_id = @member_id 
  AND a.account_type = 'SAVINGS'
UNION ALL

-- Calculate external guarantee outstanding
SELECT 
    'External Guarantee Outstanding',
    COALESCE(SUM(
        CASE 
            WHEN l.original_principal IS NOT NULL THEN l.original_principal
            ELSE l.amount
        END - COALESCE(
            (SELECT SUM(g.guarantee_amount)
             FROM guarantors g
             WHERE g.loan_id = l.id
               AND g.is_self_guarantee = 1), 0)
    ), 0)
FROM loans l
WHERE l.member_id = @member_id
  AND l.outstanding_balance > 0
  AND l.status IN ('DISBURSED', 'REPAID')
UNION ALL

-- Calculate remaining eligibility
SELECT 
    'Remaining Eligibility',
    ((COALESCE(a.balance, 0) - COALESCE(
        (SELECT SUM(g.pledge_amount)
         FROM guarantors g
         JOIN loans l ON g.loan_id = l.id
         WHERE l.member_id = @member_id
           AND g.is_self_guarantee = 1
           AND g.member_id = @member_id
           AND l.status IN ('DISBURSED', 'REPAID')), 0)) * 3) - 
    COALESCE(SUM(
        CASE 
            WHEN l.original_principal IS NOT NULL THEN l.original_principal
            ELSE l.amount
        END - COALESCE(
            (SELECT SUM(g.guarantee_amount)
             FROM guarantors g
             WHERE g.loan_id = l.id
               AND g.is_self_guarantee = 1), 0)
    ), 0)
FROM accounts a
LEFT JOIN loans l ON l.member_id = @member_id 
    AND l.outstanding_balance > 0
    AND l.status IN ('DISBURSED', 'REPAID')
WHERE a.member_id = @member_id 
  AND a.account_type = 'SAVINGS'
LIMIT 1;

-- ============================================================================
-- SECTION 7: DETAILED LOAN BREAKDOWN
-- ============================================================================
SELECT '' as '';
SELECT '=== DETAILED LOAN BREAKDOWN ===' as section;
SELECT 
    l.id as loan_id,
    l.amount as loan_amount,
    l.original_principal,
    l.outstanding_balance,
    l.status,
    COALESCE(
        (SELECT SUM(g.guarantee_amount)
         FROM guarantors g
         WHERE g.loan_id = l.id
           AND g.is_self_guarantee = 1), 0) as self_guarantee_amount,
    COALESCE(
        (SELECT SUM(g.pledge_amount)
         FROM guarantors g
         WHERE g.loan_id = l.id
           AND g.is_self_guarantee = 1), 0) as self_guarantee_frozen,
    COALESCE(
        (SELECT SUM(g.guarantee_amount)
         FROM guarantors g
         WHERE g.loan_id = l.id
           AND g.is_self_guarantee = 0), 0) as external_guarantee_amount,
    COALESCE(
        (SELECT SUM(g.pledge_amount)
         FROM guarantors g
         WHERE g.loan_id = l.id
           AND g.is_self_guarantee = 0), 0) as external_guarantee_frozen,
    (CASE 
        WHEN l.original_principal IS NOT NULL THEN l.original_principal
        ELSE l.amount
    END - COALESCE(
        (SELECT SUM(g.guarantee_amount)
         FROM guarantors g
         WHERE g.loan_id = l.id
           AND g.is_self_guarantee = 1), 0)) as external_outstanding
FROM loans l
WHERE l.member_id = @member_id
ORDER BY l.id;

-- ============================================================================
-- SECTION 8: GUARANTOR DETAILS FOR THIS MEMBER'S LOANS
-- ============================================================================
SELECT '' as '';
SELECT '=== GUARANTOR DETAILS ===' as section;
SELECT 
    g.id,
    g.loan_id,
    g.member_id as guarantor_id,
    m.member_number as guarantor_number,
    g.is_self_guarantee,
    g.guarantee_amount,
    g.pledge_amount,
    g.status,
    CASE 
        WHEN g.is_self_guarantee = 1 THEN 'SELF'
        ELSE 'EXTERNAL'
    END as guarantee_type
FROM guarantors g
JOIN loans l ON g.loan_id = l.id
JOIN members m ON g.member_id = m.id
WHERE l.member_id = @member_id
ORDER BY l.id, g.id;

-- ============================================================================
-- SECTION 9: SUMMARY FOR QUICK REFERENCE
-- ============================================================================
SELECT '' as '';
SELECT '=== QUICK SUMMARY ===' as section;
SELECT 
    @member_id as member_id,
    (SELECT m.member_number FROM members m WHERE m.id = @member_id) as member_number,
    (SELECT COALESCE(a.balance, 0) FROM accounts a WHERE a.member_id = @member_id AND a.account_type = 'SAVINGS') as savings_balance,
    (SELECT COALESCE(SUM(g.pledge_amount), 0)
     FROM guarantors g
     JOIN loans l ON g.loan_id = l.id
     WHERE l.member_id = @member_id
       AND g.is_self_guarantee = 1
       AND g.member_id = @member_id
       AND l.status IN ('DISBURSED', 'REPAID')) as frozen_self_guarantee,
    (SELECT COALESCE(SUM(g.pledge_amount), 0)
     FROM guarantors g
     JOIN loans l ON g.loan_id = l.id
     WHERE g.member_id = @member_id
       AND l.member_id != @member_id
       AND l.status IN ('DISBURSED', 'REPAID')) as frozen_guarantor_pledges,
    (SELECT COUNT(*) FROM loans l WHERE l.member_id = @member_id AND l.status IN ('DISBURSED', 'REPAID')) as active_loans,
    (SELECT COUNT(*) FROM guarantors g JOIN loans l ON g.loan_id = l.id WHERE g.member_id = @member_id AND l.member_id != @member_id AND l.status IN ('DISBURSED', 'REPAID')) as guarantor_on_loans;
"@

# Build connection string
$connectionString = "Server=$dbHost;Port=$dbPort;Database=$dbName;Uid=$dbUser;Pwd=$dbPassword;"

try {
    # Load MySQL .NET connector
    [void][System.Reflection.Assembly]::LoadWithPartialName("MySql.Data")
    
    $connection = New-Object MySql.Data.MySqlClient.MySqlConnection($connectionString)
    $connection.Open()
    
    $command = $connection.CreateCommand()
    $command.CommandText = $sqlQuery
    $command.CommandTimeout = 30
    
    $reader = $command.ExecuteReader()
    
    $sectionCount = 0
    do {
        $sectionCount++
        $table = New-Object System.Data.DataTable
        $table.Load($reader)
        
        if ($table.Rows.Count -gt 0) {
            $table | Format-Table -AutoSize
            Write-Host ""
        }
    } while ($reader.NextResult())
    
    $reader.Close()
    $connection.Close()
    
    Write-Host "============================================================================" -ForegroundColor Cyan
    Write-Host "END OF DIAGNOSTIC REPORT" -ForegroundColor Cyan
    Write-Host "============================================================================" -ForegroundColor Cyan
}
catch {
    Write-Host "Error: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Make sure you have MySQL .NET connector installed:" -ForegroundColor Yellow
    Write-Host "  Install-Package MySql.Data" -ForegroundColor Yellow
}
