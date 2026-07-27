-- Add first_login column to users table
ALTER TABLE users ADD COLUMN first_login BOOLEAN DEFAULT false NOT NULL;

-- Set existing member users to have firstLogin = true (they need to set up passwords)
UPDATE users 
SET first_login = true 
WHERE role = 'MEMBER' AND first_login = false;

-- Add comment for clarity
ALTER TABLE users MODIFY COLUMN first_login BOOLEAN DEFAULT false NOT NULL COMMENT 'Indicates if user needs to set up password on first login';