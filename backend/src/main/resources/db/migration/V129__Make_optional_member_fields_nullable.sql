-- Make optional fields nullable in members table for bulk upload with minimal data
ALTER TABLE members MODIFY COLUMN last_name VARCHAR(50) NULL;
ALTER TABLE members MODIFY COLUMN phone VARCHAR(15) NULL;
