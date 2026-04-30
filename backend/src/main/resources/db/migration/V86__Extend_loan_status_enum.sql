-- Extend the loans status ENUM to include new guarantor rejection handling statuses
-- This migration modifies the status column to support PENDING_GUARANTOR_REPLACEMENT

-- First, we need to change the ENUM type to support the new status values
-- MySQL requires us to modify the column definition

ALTER TABLE loans MODIFY COLUMN status ENUM(
    'PENDING',
    'PENDING_GUARANTOR_APPROVAL',
    'PENDING_GUARANTOR_REPLACEMENT',
    'PENDING_LOAN_OFFICER_REVIEW',
    'PENDING_CREDIT_COMMITTEE',
    'PENDING_TREASURER',
    'APPROVED',
    'REJECTED',
    'DISBURSED',
    'REPAID',
    'DEFAULTED',
    'ACTIVE'
) DEFAULT 'PENDING';

-- Also extend the guarantors status ENUM to include REPLACED status
ALTER TABLE guarantors MODIFY COLUMN status ENUM(
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'REPLACED',
    'ACTIVE',
    'DECLINED',
    'RELEASED'
) DEFAULT 'PENDING';
