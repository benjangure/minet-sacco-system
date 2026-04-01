-- Create bulk_disbursement_items table
CREATE TABLE bulk_disbursement_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    row_number INT,
    loan_number VARCHAR(50),
    loan_id BIGINT,
    disbursement_amount DECIMAL(19, 2),
    disbursement_account VARCHAR(50),
    status VARCHAR(50),
    error_message TEXT,
    processed_at TIMESTAMP NULL,
    
    FOREIGN KEY (batch_id) REFERENCES bulk_batches(id),
    FOREIGN KEY (loan_id) REFERENCES loans(id),
    INDEX idx_batch_id (batch_id),
    INDEX idx_loan_id (loan_id),
    INDEX idx_status (status)
);
