-- Manual V125 Migration - Add password column to member_credentials table
-- Run this if Flyway doesn't automatically pick up V125

-- Check if password column already exists
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'member_credentials' 
    AND COLUMN_NAME = 'password'
    AND TABLE_SCHEMA = 'sacco_db'
);

-- Only add if it doesn't exist
IF @column_exists = 0 THEN
    ALTER TABLE member_credentials ADD COLUMN password VARCHAR(255) NULL AFTER email;
    CREATE INDEX idx_member_credentials_password ON member_credentials(password);
    ALTER TABLE member_credentials COMMENT = 'Tracks member credentials including temporary password for initial login and delivery status';
    
    -- Insert into flyway history to mark migration as complete
    INSERT INTO flyway_schema_history (
        installed_rank,
        version,
        description,
        type,
        script,
        checksum,
        installed_by,
        installed_on,
        execution_time,
        success
    ) VALUES (
        125,
        '125',
        'Add password to member credentials',
        'SQL',
        'V125__Add_password_to_member_credentials.sql',
        NULL,
        'manual',
        NOW(),
        0,
        TRUE
    );
    
    SELECT 'V125 migration completed successfully!' as status;
ELSE
    SELECT 'V125 migration already applied - password column exists' as status;
END IF;
