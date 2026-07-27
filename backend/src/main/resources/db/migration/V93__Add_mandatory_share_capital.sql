-- Add mandatory 3000 KES share capital to all existing members
-- This is a one-time contribution that all members must make upon joining the SACCO

UPDATE accounts 
SET balance = 3000.00 
WHERE account_type = 'SHARES' 
AND balance = 0 
AND member_id IN (SELECT id FROM members WHERE status IN ('ACTIVE', 'APPROVED', 'DORMANT', 'SUSPENDED'));

-- Log this as a system transaction for audit purposes
-- Insert a transaction record for each member's share capital contribution
INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date, created_by)
SELECT 
    a.id,
    'DEPOSIT',
    3000.00,
    'Mandatory share capital contribution upon membership',
    NOW(),
    NULL
FROM accounts a
INNER JOIN members m ON a.member_id = m.id
WHERE a.account_type = 'SHARES'
AND a.balance = 3000.00
AND m.status IN ('ACTIVE', 'APPROVED', 'DORMANT', 'SUSPENDED')
AND NOT EXISTS (
    SELECT 1 FROM transactions t 
    WHERE t.account_id = a.id 
    AND t.transaction_type = 'DEPOSIT'
    AND t.description LIKE '%share capital%'
);
