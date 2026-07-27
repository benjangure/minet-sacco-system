-- V78: Add HR_STAFF role to users table role ENUM
-- Required for HR clearance workflow

ALTER TABLE users MODIFY COLUMN role ENUM(
    'ADMIN',
    'TREASURER',
    'LOAN_OFFICER',
    'CREDIT_COMMITTEE',
    'AUDITOR',
    'TELLER',
    'CUSTOMER_SUPPORT',
    'MEMBER',
    'HR_STAFF'
) NULL;
