-- V13: Add bulk_member_items and bulk_loan_items tables

CREATE TABLE IF NOT EXISTS bulk_member_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    row_number INTEGER NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(15),
    national_id VARCHAR(20),
    date_of_birth DATE,
    department VARCHAR(50),
    employee_id VARCHAR(50),
    member_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(500),
    processed_at TIMESTAMP,
    FOREIGN KEY (batch_id) REFERENCES bulk_batches(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE TABLE IF NOT EXISTS bulk_loan_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    row_number INTEGER NOT NULL,
    member_number VARCHAR(50),
    loan_product_name VARCHAR(100),
    amount DECIMAL(15,2),
    purpose VARCHAR(255),
    guarantor1 VARCHAR(50),
    guarantor2 VARCHAR(50),
    member_id BIGINT,
    loan_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(500),
    processed_at TIMESTAMP,
    FOREIGN KEY (batch_id) REFERENCES bulk_batches(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (loan_id) REFERENCES loans(id)
);

CREATE INDEX idx_bulk_member_items_batch ON bulk_member_items(batch_id);
CREATE INDEX idx_bulk_loan_items_batch ON bulk_loan_items(batch_id);
