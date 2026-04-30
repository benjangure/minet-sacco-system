-- Fix self-guarantee amounts that were stored incorrectly
-- For self-guarantees, the guarantee_amount should equal the loan principal amount
-- This migration corrects cases where the guarantee_amount is half the loan amount
-- 
-- Issue: When a member self-guarantees a loan, the guarantee_amount was being stored
-- as half the loan amount instead of the full loan amount. This caused eligibility
-- calculations to show incorrect frozen amounts.
--
-- Example: Member applies for 20,000 loan and self-guarantees full 20,000
-- Bug: guarantee_amount stored as 10,000 instead of 20,000
-- Fix: Update guarantee_amount to match original_principal

UPDATE guarantors g
JOIN loans l ON g.loan_id = l.id
SET g.guarantee_amount = l.original_principal,
    g.pledge_amount = l.original_principal
WHERE g.self_guarantee = 1
  AND g.guarantee_amount < l.original_principal
  AND g.guarantee_amount * 2 = l.original_principal;

-- Verify the fix
SELECT 'Self-guarantee amounts fixed' as status,
       COUNT(*) as rows_updated
FROM guarantors g
JOIN loans l ON g.loan_id = l.id
WHERE g.self_guarantee = 1
  AND g.guarantee_amount = l.original_principal;
