-- V25__Create_loan_eligibility_rules.sql
-- Create table for configurable loan eligibility rules

CREATE TABLE IF NOT EXISTS loan_eligibility_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    min_member_savings DECIMAL(19, 2) NOT NULL DEFAULT 10000.00,
    min_member_shares DECIMAL(19, 2) NOT NULL DEFAULT 5000.00,
    min_savings_to_loan_ratio DECIMAL(5, 2) NOT NULL DEFAULT 0.20,
    max_outstanding_to_savings_ratio DECIMAL(5, 2) NOT NULL DEFAULT 0.50,
    max_active_loans INT NOT NULL DEFAULT 3,
    min_guarantor_savings DECIMAL(19, 2) NOT NULL DEFAULT 10000.00,
    min_guarantor_shares DECIMAL(19, 2) NOT NULL DEFAULT 5000.00,
    min_guarantor_savings_to_loan_ratio DECIMAL(5, 2) NOT NULL DEFAULT 0.50,
    max_guarantor_outstanding_to_savings_ratio DECIMAL(5, 2) NOT NULL DEFAULT 0.50,
    max_guarantor_commitments INT NOT NULL DEFAULT 3,
    allow_defaulters BOOLEAN NOT NULL DEFAULT FALSE,
    allow_exited_members BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Insert default rules
INSERT INTO loan_eligibility_rules (
    min_member_savings,
    min_member_shares,
    min_savings_to_loan_ratio,
    max_outstanding_to_savings_ratio,
    max_active_loans,
    min_guarantor_savings,
    min_guarantor_shares,
    min_guarantor_savings_to_loan_ratio,
    max_guarantor_outstanding_to_savings_ratio,
    max_guarantor_commitments,
    allow_defaulters,
    allow_exited_members
) VALUES (
    10000.00,
    5000.00,
    0.20,
    0.50,
    3,
    10000.00,
    5000.00,
    0.50,
    0.50,
    3,
    FALSE,
    FALSE
);
