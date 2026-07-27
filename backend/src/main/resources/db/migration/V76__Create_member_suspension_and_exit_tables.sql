-- V76__Create_member_suspension_and_exit_tables.sql
-- Create tables for member suspension and exit workflows

CREATE TABLE IF NOT EXISTS member_suspensions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    reason LONGTEXT NOT NULL COMMENT 'Reason for suspension',
    suspended_by BIGINT NOT NULL COMMENT 'User who suspended',
    suspended_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    lifted_by BIGINT COMMENT 'User who lifted suspension',
    lifted_at TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Whether suspension is currently active',
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (suspended_by) REFERENCES users(id),
    FOREIGN KEY (lifted_by) REFERENCES users(id),
    INDEX idx_member_id (member_id),
    INDEX idx_is_active (is_active),
    INDEX idx_suspended_at (suspended_at)
) COMMENT 'Tracks member suspensions';

CREATE TABLE IF NOT EXISTS member_exits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    exit_reason VARCHAR(50) NOT NULL COMMENT 'RETIREMENT, RESIGNATION, TERMINATION, DECEASED, OTHER',
    initiated_by BIGINT NOT NULL COMMENT 'User who initiated exit',
    approved_by BIGINT COMMENT 'User who approved exit (Treasurer)',
    savings_balance DECIMAL(19,2) COMMENT 'Member savings at exit',
    outstanding_loan DECIMAL(19,2) COMMENT 'Outstanding loan balance',
    loan_deduction DECIMAL(19,2) COMMENT 'Amount deducted from savings to clear loan',
    remaining_payout DECIMAL(19,2) COMMENT 'Remaining savings after loan deduction',
    shares_refund DECIMAL(19,2) DEFAULT 3000.00 COMMENT 'Shares refund (KES 3,000)',
    total_payout DECIMAL(19,2) COMMENT 'Total payout to member',
    exit_date TIMESTAMP COMMENT 'Date member exited',
    approved_at TIMESTAMP NULL COMMENT 'Date exit was approved',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes LONGTEXT COMMENT 'Exit notes',
    is_active_guarantor BOOLEAN DEFAULT FALSE COMMENT 'Whether member is active guarantor',
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (initiated_by) REFERENCES users(id),
    FOREIGN KEY (approved_by) REFERENCES users(id),
    INDEX idx_member_id (member_id),
    INDEX idx_exit_reason (exit_reason),
    INDEX idx_approved_by (approved_by),
    INDEX idx_created_at (created_at)
) COMMENT 'Tracks member exits and payouts';
