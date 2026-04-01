-- V17__Create_kyc_documents_table.sql
-- KYC Document Management System

-- Create KYC documents table
CREATE TABLE kyc_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    document_type ENUM('NATIONAL_ID', 'PASSPORT', 'PASSPORT_PHOTO', 'APPLICATION_LETTER', 'KRA_PIN_CERTIFICATE') NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    uploaded_by BIGINT NOT NULL,
    verification_status ENUM('PENDING', 'VERIFIED', 'REJECTED') DEFAULT 'PENDING',
    verified_by BIGINT,
    verified_at TIMESTAMP NULL,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id),
    FOREIGN KEY (verified_by) REFERENCES users(id),
    UNIQUE KEY unique_member_document_type (member_id, document_type),
    INDEX idx_member_id (member_id),
    INDEX idx_document_type (document_type),
    INDEX idx_verification_status (verification_status),
    INDEX idx_upload_date (upload_date)
);

-- Add KYC completion tracking to members table
ALTER TABLE members ADD COLUMN kyc_completion_status ENUM('INCOMPLETE', 'COMPLETE', 'VERIFIED') DEFAULT 'INCOMPLETE' AFTER status;
ALTER TABLE members ADD COLUMN kyc_completed_at TIMESTAMP NULL AFTER kyc_completion_status;
ALTER TABLE members ADD COLUMN kyc_verified_at TIMESTAMP NULL AFTER kyc_completed_at;

-- Create index for KYC status tracking
CREATE INDEX idx_kyc_completion_status ON members(kyc_completion_status);
CREATE INDEX idx_kyc_completed_at ON members(kyc_completed_at);

-- Create audit log for KYC document operations
CREATE TABLE kyc_document_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    kyc_document_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    performed_by BIGINT NOT NULL,
    old_status VARCHAR(50),
    new_status VARCHAR(50),
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (kyc_document_id) REFERENCES kyc_documents(id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (performed_by) REFERENCES users(id),
    INDEX idx_member_id (member_id),
    INDEX idx_timestamp (timestamp)
);
