-- Add created_by column to users table to track who created each user
ALTER TABLE users ADD COLUMN created_by BIGINT;

-- Add foreign key constraint
ALTER TABLE users ADD CONSTRAINT fk_users_created_by 
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL;

-- Set created_by for existing users
-- Admin (id=1) is self-created or system-created
UPDATE users SET created_by = NULL WHERE id = 1;

-- All other existing users were likely created by admin
UPDATE users SET created_by = 1 WHERE id > 1 AND created_by IS NULL;
