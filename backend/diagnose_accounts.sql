-- Diagnostic query to check account data for Edwin Kibe (EMP121)
SELECT 
    m.id,
    m.member_number,
    m.first_name,
    m.last_name,
    a.id as account_id,
    a.account_type,
    a.balance,
    a.frozen_savings,
    a.created_at,
    a.updated_at
FROM members m
LEFT JOIN accounts a ON m.id = a.member_id
WHERE m.member_number = 'EMP121'
ORDER BY a.account_type;

-- Also check if there are SHARES accounts
SELECT 
    m.id,
    m.member_number,
    m.first_name,
    m.last_name,
    COUNT(DISTINCT a.id) as total_accounts,
    GROUP_CONCAT(DISTINCT a.account_type) as account_types,
    SUM(CASE WHEN a.account_type = 'SAVINGS' THEN a.balance ELSE 0 END) as total_savings,
    SUM(CASE WHEN a.account_type = 'SHARES' THEN a.balance ELSE 0 END) as total_shares,
    SUM(a.balance) as total_all_accounts
FROM members m
LEFT JOIN accounts a ON m.id = a.member_id
WHERE m.member_number = 'EMP121'
GROUP BY m.id, m.member_number, m.first_name, m.last_name;

-- Check guarantor pledges for this member
SELECT 
    m.member_number,
    m.first_name,
    m.last_name,
    g.id as guarantor_id,
    g.self_guarantee,
    g.status,
    g.pledge_amount,
    g.guarantee_amount,
    l.loan_number,
    l.member_id as borrower_id,
    CONCAT(lm.first_name, ' ', lm.last_name) as borrower_name
FROM members m
LEFT JOIN guarantors g ON m.id = g.member_id
LEFT JOIN loans l ON g.loan_id = l.id
LEFT JOIN members lm ON l.member_id = lm.id
WHERE m.member_number = 'EMP121'
ORDER BY g.self_guarantee DESC, g.status;
