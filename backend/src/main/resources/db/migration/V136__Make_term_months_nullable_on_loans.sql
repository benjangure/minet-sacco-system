-- Make term_months nullable on loans table to support migrated loans created without term
-- Migrated loans can have term_months set later via UPDATE
ALTER TABLE loans MODIFY COLUMN term_months INT NULL;
