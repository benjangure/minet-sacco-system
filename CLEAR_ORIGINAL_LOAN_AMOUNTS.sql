-- Clear original_amount and original_principal columns in loans table
-- This will make the UI show KES 0 for all loans since these are the fields displayed

SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- Update the original amount fields to 0
UPDATE loans 
SET 
    original_amount = 0,
    original_principal = 0
WHERE id > 0;

-- Verify the update
SELECT 
    COUNT(*) as total_loans,
    SUM(original_amount) as sum_original_amount,
    SUM(original_principal) as sum_original_principal,
    SUM(amount) as sum_amount,
    SUM(outstanding_balance) as sum_outstanding
FROM loans;

COMMIT;

SET SQL_SAFE_UPDATES = 1;

SELECT '✓ Original loan amounts cleared successfully' AS status;
