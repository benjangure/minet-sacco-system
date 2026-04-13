-- V72: Fix self_guarantee NULL values and ensure proper defaults
-- Purpose: Convert NULL self_guarantee values to FALSE and fix specific records

-- Step 1: Update all NULL self_guarantee values to FALSE
UPDATE guarantors
SET self_guarantee = FALSE
WHERE self_guarantee IS NULL;

-- Step 2: Identify self-guarantors (where guarantor member_id = loan member_id)
-- and set their self_guarantee flag to TRUE
UPDATE guarantors g
SET self_guarantee = TRUE
WHERE g.member_id = (
  SELECT l.member_id
  FROM loans l
  WHERE l.id = g.loan_id
)
AND g.self_guarantee = FALSE;

-- Step 3: Recalculate frozen_savings for all members with self-guaranteed loans
UPDATE accounts a
SET frozen_savings = (
  SELECT COALESCE(SUM(g.guarantee_amount), 0)
  FROM guarantors g
  JOIN loans l ON g.loan_id = l.id
  WHERE g.member_id = a.member_id 
    AND g.self_guarantee = true
    AND l.status IN ('DISBURSED', 'ACTIVE', 'REPAID')
    AND l.outstanding_balance > 0
)
WHERE account_type = 'SAVINGS'
  AND a.member_id IN (
    SELECT DISTINCT g.member_id
    FROM guarantors g
    WHERE g.self_guarantee = true
  );

-- Step 4: Verify the fix
SELECT 'Self-guarantee NULL values fixed and frozen_savings recalculated' as status;

-- Step 5: Show updated guarantors with self_guarantee = true
SELECT 
  g.id,
  g.loan_id,
  l.loan_number,
  g.member_id,
  m.first_name,
  m.last_name,
  g.guarantee_amount,
  g.self_guarantee,
  a.frozen_savings,
  l.status
FROM guarantors g
JOIN loans l ON g.loan_id = l.id
JOIN members m ON g.member_id = m.id
JOIN accounts a ON g.member_id = a.member_id AND a.account_type = 'SAVINGS'
WHERE g.self_guarantee = true
ORDER BY l.id DESC
LIMIT 20;
