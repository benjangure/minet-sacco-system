-- Add migration_mode and snapshot_id to loan_migration_items
ALTER TABLE loan_migration_items 
ADD COLUMN migration_mode VARCHAR(10);

ALTER TABLE loan_migration_items 
ADD COLUMN snapshot_id BIGINT;

-- Create loan_migration_snapshots table
CREATE TABLE loan_migration_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    original_outstanding_balance DECIMAL(15, 2),
    original_term_months INT,
    original_interest_collected DECIMAL(15, 2),
    original_disbursement_date DATETIME,
    snapshot_created_at DATETIME NOT NULL,
    snapshot_reason VARCHAR(500),
    CONSTRAINT fk_snapshot_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE,
    INDEX idx_snapshot_loan_id (loan_id),
    UNIQUE KEY uk_snapshot_item (loan_id) -- One snapshot per loan per item
);

-- Add foreign key for snapshot_id in loan_migration_items
ALTER TABLE loan_migration_items 
ADD CONSTRAINT fk_item_snapshot FOREIGN KEY (snapshot_id) REFERENCES loan_migration_snapshots(id) ON DELETE SET NULL;
