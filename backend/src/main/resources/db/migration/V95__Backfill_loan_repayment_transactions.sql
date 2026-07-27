-- V94: Backfill loan repayment transactions
-- Loan repayments were stored in loan_repayments table but never created
-- corresponding records in the transactions table. This migration creates
-- those missing transaction records so they appear in Member Transaction History.
-- Note: loan_repayments.created_by maps to the User who recorded the repayment.

INSERT INTO transactions (account_id, transaction_type, amount, description, transaction_date, created_by)
SELECT
    a.id AS account_id,
    'LOAN_REPAYMENT' AS transaction_type,
    lr.amount,
    CONCAT(
        'Loan repayment - Loan #', l.loan_number,
        ' - Method: ', COALESCE(lr.payment_method, 'CASH'),
        CASE WHEN lr.reference_number IS NOT NULL AND lr.reference_number != ''
             THEN CONCAT(' - Ref: ', lr.reference_number)
             ELSE ''
        END
    ) AS description,
    COALESCE(lr.payment_date, lr.created_at, NOW()) AS transaction_date,
    lr.created_by AS created_by
FROM loan_repayments lr
JOIN loans l ON lr.loan_id = l.id
JOIN members m ON l.member_id = m.id
JOIN accounts a ON a.member_id = m.id AND a.account_type = 'SAVINGS'
-- Only backfill repayments that don't already have a matching transaction
WHERE NOT EXISTS (
    SELECT 1 FROM transactions t
    WHERE t.account_id = a.id
      AND t.transaction_type = 'LOAN_REPAYMENT'
      AND t.amount = lr.amount
      AND DATE(t.transaction_date) = DATE(COALESCE(lr.payment_date, lr.created_at))
);
