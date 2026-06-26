-- Phase A: Bulk Loan Data Update Items table
-- For tracking bulk updates to loan fields (Employee ID, Loan Number, and 5 Phase A fields)

CREATE TABLE IF NOT EXISTS bulk_loan_data_update_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    row_number INT NOT NULL,
    
    -- Input fields from file
    employee_id VARCHAR(50),
    loan_number VARCHAR(50),
    loan_status VARCHAR(50),
    disbursement_date DATE,
    interest_rate DECIMAL(10, 2),
    outstanding_balance DECIMAL(15, 2),
    purpose VARCHAR(500),
    
    -- Processing fields
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, PROCESSED, FAILED
    error_message VARCHAR(1000),
    processed_at DATETIME,
    
    -- References
    loan_id BIGINT,
    member_id BIGINT,
    
    -- Timestamps
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_bulk_loan_data_update_batch FOREIGN KEY (batch_id) REFERENCES bulk_batches(id) ON DELETE CASCADE,
    CONSTRAINT fk_bulk_loan_data_update_loan FOREIGN KEY (loan_id) REFERENCES loans(id),
    CONSTRAINT fk_bulk_loan_data_update_member FOREIGN KEY (member_id) REFERENCES members(id),
    
    -- Indexes
    INDEX idx_batch_id (batch_id),
    INDEX idx_status (status),
    INDEX idx_loan_id (loan_id),
    INDEX idx_member_id (member_id),
    INDEX idx_row_number (row_number),
    INDEX idx_batch_status (batch_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add index for faster lookups by employee ID and loan number
CREATE INDEX idx_employee_loan ON bulk_loan_data_update_items(employee_id, loan_number);
