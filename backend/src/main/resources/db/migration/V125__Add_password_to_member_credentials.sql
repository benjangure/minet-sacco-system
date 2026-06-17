-- Add password column to member_credentials table to store temporary passwords
-- This allows staff to retrieve member credentials for delivery/support

-- Add password column (nullable, comes after email column)
ALTER TABLE member_credentials ADD COLUMN `password` VARCHAR(255) NULL;

-- Add index for quick lookups  
CREATE INDEX idx_member_credentials_password ON member_credentials(`password`);
