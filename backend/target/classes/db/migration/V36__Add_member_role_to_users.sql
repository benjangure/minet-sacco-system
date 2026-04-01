-- V36: Add MEMBER role to users table role ENUM
-- Required for mobile app login credentials created during bulk member registration

ALTER TABLE users MODIFY COLUMN role ENUM(
    'ADMIN',
    'TREASURER',
    'LOAN_OFFICER',
    'CREDIT_COMMITTEE',
    'AUDITOR',
    'TELLER',
    'CUSTOMER_SUPPORT',
    'MEMBER'
) NULL;
