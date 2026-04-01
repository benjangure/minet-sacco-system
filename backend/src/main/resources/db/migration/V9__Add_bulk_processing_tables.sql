-- Create table for bulk transaction batches
CREATE TABLE bulk_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_number VARCHAR(50) UNIQUE NOT NULL,
    batch_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    total_records INT NOT NULL,
    successful_records INT DEFAULT 0,
    failed_records INT DEFAULT 0,
    total_amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_by BIGINT,
    approved_at TIMESTAMP NULL,
    processed_at TIMESTAMP NULL,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (uploaded_by) REFERENCES users(id),
    FOREIGN KEY (approved_by) REFERENCES users(id),
    
    CONSTRAINT chk_batch_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

-- Create table for individual bulk transaction items
CREATE TABLE bulk_transaction_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    row_number INT NOT NULL,
    member_number VARCHAR(50),
    member_id BIGINT,
    savings_amount DECIMAL(15,2) DEFAULT 0,
    shares_amount DECIMAL(15,2) DEFAULT 0,
    loan_repayment_amount DECIMAL(15,2) DEFAULT 0,
    loan_number VARCHAR(50),
    loan_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(500),
    savings_transaction_id BIGINT,
    shares_transaction_id BIGINT,
    loan_repayment_id BIGINT,
    processed_at TIMESTAMP NULL,
    
    FOREIGN KEY (batch_id) REFERENCES bulk_batches(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (loan_id) REFERENCES loans(id),
    FOREIGN KEY (savings_transaction_id) REFERENCES transactions(id),
    FOREIGN KEY (shares_transaction_id) REFERENCES transactions(id),
    FOREIGN KEY (loan_repayment_id) REFERENCES loan_repayments(id),
    
    CONSTRAINT chk_item_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'SKIPPED'))
);

-- Create indexes for performance
CREATE INDEX idx_bulk_batches_status ON bulk_batches(status);
CREATE INDEX idx_bulk_batches_uploaded_by ON bulk_batches(uploaded_by);
CREATE INDEX idx_bulk_batches_batch_number ON bulk_batches(batch_number);
CREATE INDEX idx_bulk_transaction_items_batch_id ON bulk_transaction_items(batch_id);
CREATE INDEX idx_bulk_transaction_items_status ON bulk_transaction_items(status);
CREATE INDEX idx_bulk_transaction_items_member_id ON bulk_transaction_items(member_id);
