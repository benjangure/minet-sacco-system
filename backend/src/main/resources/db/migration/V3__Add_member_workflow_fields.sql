-- V3__Add_member_workflow_fields.sql
-- Add workflow and KYC document fields to members table

ALTER TABLE members
    ADD COLUMN id_document_path VARCHAR(255),
    ADD COLUMN photo_path VARCHAR(255),
    ADD COLUMN application_letter_path VARCHAR(255),
    ADD COLUMN kra_pin_path VARCHAR(255),
    ADD COLUMN bank_name VARCHAR(100),
    ADD COLUMN bank_account_number VARCHAR(50),
    ADD COLUMN bank_branch VARCHAR(50),
    ADD COLUMN next_of_kin_name VARCHAR(100),
    ADD COLUMN next_of_kin_phone VARCHAR(15),
    ADD COLUMN next_of_kin_relationship VARCHAR(50),
    ADD COLUMN created_by BIGINT,
    ADD COLUMN approved_by BIGINT,
    ADD COLUMN approved_at TIMESTAMP NULL DEFAULT NULL,
    ADD COLUMN rejection_reason VARCHAR(255);

-- Update status enum to include workflow statuses
ALTER TABLE members MODIFY COLUMN status ENUM('PENDING', 'APPROVED', 'ACTIVE', 'DORMANT', 'SUSPENDED', 'REJECTED', 'EXITED') DEFAULT 'PENDING';

-- Update existing members to ACTIVE status
UPDATE members SET status = 'ACTIVE' WHERE status IS NULL OR status = 'ACTIVE';
