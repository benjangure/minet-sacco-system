-- V74__Create_migration_tables.sql
-- Create tables for data migration functionality

-- Add migration-related columns to members table
ALTER TABLE members ADD COLUMN IF NOT EXISTS consecutive_months_counter INT DEFAULT 0 COMMENT 'Consecutive months contributed for eligibility';
ALTER TABLE members ADD COLUMN IF NOT EXISTS migration_status VARCHAR(50) DEFAULT 'ACTIVE' COMMENT 'ACTIVE or MIGRATED';
ALTER TABLE members ADD COLUMN IF NOT EXISTS is_legacy_member BOOLEAN DEFAULT FALSE COMMENT 'True if member joined before go-live';

-- Add migration status to loans table
ALTER TABLE loans ADD COLUMN IF NOT EXISTS migration_status VARCHAR(50) DEFAULT 'ACTIVE' COMMENT 'ACTIVE or MIGRATED';

-- Add migration status to guarantors table
ALTER TABLE guarantors ADD COLUMN IF NOT EXISTS migration_status VARCHAR(50) DEFAULT 'ACTIVE' COMMENT 'ACTIVE or MIGRATED';

-- Create migration batches table
CREATE TABLE IF NOT EXISTS migration_batches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_records INT DEFAULT 0,
    successful_records INT DEFAULT 0,
    failed_records INT DEFAULT 0,
    verification_status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING, VERIFIED, FAILED',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    migration_executed BOOLEAN DEFAULT FALSE,
    executed_at TIMESTAMP NULL,
    error_message LONGTEXT,
    uploaded_by BIGINT COMMENT 'User who uploaded the file (Treasurer)',
    uploaded_at TIMESTAMP NULL,
    verified_by BIGINT COMMENT 'User who verified the file (Admin)',
    verified_at TIMESTAMP NULL,
    verification_notes LONGTEXT COMMENT 'Admin verification notes',
    approval_status VARCHAR(50) DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED',
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (uploaded_by) REFERENCES users(id),
    FOREIGN KEY (verified_by) REFERENCES users(id),
    INDEX idx_batch_date (batch_date),
    INDEX idx_verification_status (verification_status),
    INDEX idx_approval_status (approval_status),
    INDEX idx_migration_executed (migration_executed)
) COMMENT 'Tracks data migration batches with maker-checker workflow';

-- Create index for migration status queries
CREATE INDEX IF NOT EXISTS idx_member_migration_status ON members(migration_status);
CREATE INDEX IF NOT EXISTS idx_loan_migration_status ON loans(migration_status);
CREATE INDEX IF NOT EXISTS idx_guarantor_migration_status ON guarantors(migration_status);
