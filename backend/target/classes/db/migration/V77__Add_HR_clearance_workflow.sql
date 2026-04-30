-- Add HR_STAFF role to users table
-- This migration adds support for HR clearance workflow

-- Create hr_loan_decisions table to track HR decisions
CREATE TABLE IF NOT EXISTS hr_loan_decisions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_id BIGINT NOT NULL,
    hr_user_id BIGINT NOT NULL,
    decision VARCHAR(20) NOT NULL COMMENT 'APPROVED or REJECTED',
    comment TEXT,
    reason VARCHAR(500) COMMENT 'Mandatory for rejection',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (loan_id) REFERENCES loans(id),
    FOREIGN KEY (hr_user_id) REFERENCES users(id),
    INDEX idx_loan_id (loan_id),
    INDEX idx_hr_user_id (hr_user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add index for querying pending HR clearance loans
CREATE INDEX idx_loan_status ON loans(status);
