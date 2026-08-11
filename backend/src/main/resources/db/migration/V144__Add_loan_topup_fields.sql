-- Add Loan Top-Up Support (Incremental Top-Up on Same Loan)
-- This allows members to add funds to an existing loan without creating a new loan

ALTER TABLE loans
ADD COLUMN total_topup_amount DECIMAL(15,2) DEFAULT 0 COMMENT 'Cumulative sum of all top-up amounts added to this loan',
ADD COLUMN topup_count INT DEFAULT 0 COMMENT 'Number of times this loan has been topped up',
ADD COLUMN last_topup_date TIMESTAMP NULL COMMENT 'Timestamp of the most recent top-up transaction',
ADD COLUMN principal_before_topup DECIMAL(15,2) NULL COMMENT 'Principal amount before the last top-up (for audit trail)';

-- Create indexes for performance
CREATE INDEX idx_loans_topup_count ON loans(topup_count);
CREATE INDEX idx_loans_last_topup_date ON loans(last_topup_date);

-- Create loan_topup_history table to track each top-up transaction
CREATE TABLE loan_topup_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    topup_amount DECIMAL(15,2) NOT NULL,
    outstanding_before_topup DECIMAL(15,2) NOT NULL,
    outstanding_after_topup DECIMAL(15,2) NOT NULL,
    principal_paid_before_topup DECIMAL(15,2) NOT NULL,
    new_guarantors_added INT DEFAULT 0,
    topup_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_by BIGINT,
    notes TEXT,
    CONSTRAINT fk_topup_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE,
    CONSTRAINT fk_topup_user FOREIGN KEY (processed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_topup_amount_positive CHECK (topup_amount > 0)
) COMMENT='Audit trail of all loan top-up transactions showing before/after balances';

CREATE INDEX idx_topup_history_loan ON loan_topup_history(loan_id);
CREATE INDEX idx_topup_history_date ON loan_topup_history(topup_date);
