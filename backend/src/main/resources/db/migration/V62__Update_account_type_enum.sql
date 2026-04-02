-- V62__Update_account_type_enum.sql
-- Update account_type ENUM to include all account types defined in the Account entity

ALTER TABLE accounts MODIFY COLUMN account_type ENUM(
    'SAVINGS',
    'SHARES',
    'CONTRIBUTIONS',
    'BENEVOLENT_FUND',
    'DEVELOPMENT_FUND',
    'SCHOOL_FEES',
    'HOLIDAY_FUND',
    'EMERGENCY_FUND'
) DEFAULT 'SAVINGS';
