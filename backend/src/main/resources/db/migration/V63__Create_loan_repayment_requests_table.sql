-- Create loan_repayment_requests table for bank transfer repayment requests
CREATE TABLE IF NOT EXISTS loan_repayment_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    proof_file_path VARCHAR(500),
    proof_file_name VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    approved_by VARCHAR(255),
    confirmed_amount DECIMAL(19, 2),
    rejection_reason TEXT,
    
    FOREIGN KEY (loan_id) REFERENCES loans(id),
    FOREIGN KEY (member_id) REFERENCES members(id),
    
    INDEX idx_status (status),
    INDEX idx_member_id (member_id),
    INDEX idx_loan_id (loan_id),
    INDEX idx_created_at (created_at)
);

-- Add comment
ALTER TABLE loan_repayment_requests COMMENT = 'Tracks bank transfer loan repayment requests pending teller approval';
