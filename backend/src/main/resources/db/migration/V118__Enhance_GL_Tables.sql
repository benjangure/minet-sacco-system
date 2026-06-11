-- V118: Enhance GL Tables with period sensitivity and normal balance tracking
-- Adds period-based filtering support and report structure fields
-- Do NOT reference or modify V116 or V117

-- Add new columns to gl_accounts table
ALTER TABLE gl_accounts
ADD COLUMN normal_balance ENUM('DEBIT','CREDIT') NULL COMMENT 'Which side this account normally appears on in reports',
ADD COLUMN section_label VARCHAR(100) NULL COMMENT 'Display group heading (e.g., Cash and Cash Equivalents)',
ADD COLUMN period_sensitive BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'If true, manual entries filtered by period month/year, not cumulative';

-- Add new columns to gl_manual_entries table
ALTER TABLE gl_manual_entries
ADD COLUMN period_month INT NULL COMMENT 'Accounting period month (1-12)',
ADD COLUMN period_year INT NULL COMMENT 'Accounting period year (e.g., 2023)',
ADD COLUMN period_status ENUM('DRAFT','POSTED','APPROVED','LOCKED') NOT NULL DEFAULT 'DRAFT' COMMENT 'Entry lifecycle status within a period';
