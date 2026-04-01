-- V47: Fix notifications table structure
-- Ensure the table has the correct columns and structure

-- Drop the old `read` column if it exists and add `is_read` if it doesn't
ALTER TABLE notifications DROP COLUMN IF EXISTS `read`;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_read TINYINT(1) DEFAULT 0;

-- Ensure user_id column exists
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- Add created_at column if it doesn't exist
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Drop existing foreign key if it exists, then add the new one
ALTER TABLE notifications DROP FOREIGN KEY IF EXISTS fk_notifications_user_id;
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user_id 
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Add indexes if they don't exist
CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications(created_at);
