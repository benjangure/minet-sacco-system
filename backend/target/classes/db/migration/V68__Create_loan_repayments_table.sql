-- Create loan_repayments table to track individual loan repayments
CREATE TABLE IF NOT EXISTS loan_repayments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    loan_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    payment_method VARCHAR(50) DEFAULT 'CASH',
    reference_number VARCHAR(100),
    payment_date DATETIME NOT NULL,
    recorded_by BIGINT NOT NULL,
    description TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (loan_id) REFERENCES loans(id),
    FOREIGN KEY (recorded_by) REFERENCES users(id),
    
    INDEX idx_loan_id (loan_id),
    INDEX idx_payment_date (payment_date),
    INDEX idx_recorded_by (recorded_by)
);
