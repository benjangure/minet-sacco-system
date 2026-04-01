-- V49__Update_loan_status_enum.sql
-- Update the loans table status column to include new workflow stages

ALTER TABLE loans MODIFY COLUMN status ENUM(
    'PENDING',
    'PENDING_GUARANTOR_APPROVAL',
    'PENDING_LOAN_OFFICER_REVIEW',
    'PENDING_CREDIT_COMMITTEE',
    'PENDING_TREASURER',
    'APPROVED',
    'REJECTED',
    'DISBURSED',
    'REPAID',
    'DEFAULTED'
) DEFAULT 'PENDING';
