-- Ensure password column exists in member_credentials table
-- This is a safeguard migration that checks and adds the column if needed

DELIMITER ;;
CREATE PROCEDURE add_password_column_if_not_exists()
BEGIN
    DECLARE column_count INT;
    
    SELECT COUNT(*) INTO column_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_NAME = 'member_credentials'
    AND TABLE_SCHEMA = 'sacco_db'
    AND COLUMN_NAME = 'password';
    
    IF column_count = 0 THEN
        ALTER TABLE member_credentials ADD COLUMN `password` VARCHAR(255) NULL;
        CREATE INDEX idx_member_credentials_password ON member_credentials(`password`);
    END IF;
END;;
DELIMITER ;

CALL add_password_column_if_not_exists();
DROP PROCEDURE add_password_column_if_not_exists;

