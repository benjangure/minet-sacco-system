-- Fix guarantor pledge amounts for loans where all guarantors have the same pledge amount
-- This fixes the bug where pledge_amount was set to the full loan amount instead of the actual guarantee amount

-- For self-guarantees: pledge_amount should be the self-guarantee amount (50% of loan in this case)
UPDATE guarantors g
JOIN loans l ON g.loan_id = l.id
SET g.pledge_amount = ROUND(l.original_principal * 0.5, 2),
    g.guarantee_amount = ROUND(l.original_principal * 0.5, 2)
WHERE g.self_guarantee = 1
  AND g.pledge_amount = l.original_principal
  AND l.status IN ('DISBURSED', 'REPAID');

-- For external guarantors, distribute the remaining 50% equally
-- Count how many external guarantors each loan has
UPDATE guarantors g
JOIN loans l ON g.loan_id = l.id
JOIN (
  SELECT loan_id, COUNT(*) as external_count
  FROM guarantors
  WHERE self_guarantee = 0
  GROUP BY loan_id
) counts ON g.loan_id = counts.loan_id
SET g.pledge_amount = ROUND(l.original_principal * 0.5 / counts.external_count, 2),
    g.guarantee_amount = ROUND(l.original_principal * 0.5 / counts.external_count, 2)
WHERE g.self_guarantee = 0
  AND g.pledge_amount = l.original_principal
  AND l.status IN ('DISBURSED', 'REPAID');
