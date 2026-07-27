-- Add guarantor reassignment tracking fields and update ENUMs
-- This migration adds support for the guarantor reassignment workflow

-- Add new columns to guarantors table for tracking reassignment
ALTER TABLE guarantors ADD COLUMN previous_guarantee_amount DECIMAL(19,2) AFTER guarantee_amount;
ALTER TABLE guarantors ADD COLUMN reassignment_reason VARCHAR(255) AFTER previous_guarantee_amount;

-- Update loans status ENUM to include PENDING_GUARANTOR_REASSIGNMENT
ALTER TABLE loans MODIFY COLUMN status ENUM(
    'PENDING',
    'PENDING_GUARANTOR_APPROVAL',
    'PENDING_GUARANTOR_REPLACEMENT',
    'PENDING_GUARANTOR_REASSIGNMENT',
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

-- Update guarantors status ENUM to include PENDING_REASSIGNMENT
ALTER TABLE guarantors MODIFY COLUMN status ENUM(
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'REPLACED',
    'ACTIVE',
    'DECLINED',
    'RELEASED',
    'PENDING_REASSIGNMENT'
) DEFAULT 'PENDING';
