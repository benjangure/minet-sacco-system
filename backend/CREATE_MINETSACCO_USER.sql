-- Create MySQL user for minetsacco database
-- Run this as root user in MySQL Workbench or mysql command line

-- Step 1: Create the user if it doesn't exist
CREATE USER IF NOT EXISTS 'minetsacco'@'localhost' IDENTIFIED BY '0a0b0c0D.';

-- Step 2: Grant all privileges on minetsacco database
GRANT ALL PRIVILEGES ON minetsacco.* TO 'minetsacco'@'localhost';

-- Step 3: Apply the changes
FLUSH PRIVILEGES;

-- Step 4: Verify the user and permissions
SELECT user, host FROM mysql.user WHERE user = 'minetsacco';
SHOW GRANTS FOR 'minetsacco'@'localhost';
