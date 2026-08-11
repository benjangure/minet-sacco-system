-- Add Multiple Next of Kin with Percentage Allocation
-- This allows members to assign multiple beneficiaries with percentage shares

USE minetsacco;

-- Create next_of_kin table
CREATE TABLE IF NOT EXISTS next_of_kin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    phone VARCHAR(15) NOT NULL,
    email VARCHAR(100),
    id_number VARCHAR(20),
    percentage DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_next_of_kin_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    CONSTRAINT chk_percentage CHECK (percentage >= 0 AND percentage <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create index for faster lookups
CREATE INDEX idx_next_of_kin_member ON next_of_kin(member_id);

-- Migrate existing next of kin data to new table
INSERT INTO next_of_kin (member_id, full_name, relationship, phone, percentage, is_primary)
SELECT 
    id,
    next_of_kin_name,
    COALESCE(next_of_kin_relationship, 'Not Specified'),
    next_of_kin_phone,
    100.00, -- Give existing NOK 100%
    TRUE
FROM members
WHERE next_of_kin_name IS NOT NULL 
  AND next_of_kin_name != ''
  AND next_of_kin_phone IS NOT NULL
  AND next_of_kin_phone != '';

-- Verification
SELECT 'Next of Kin Migration Summary' as Info;
SELECT 
    'Migrated Next of Kin' as Status,
    COUNT(*) as Total,
    COUNT(DISTINCT member_id) as Members
FROM next_of_kin;

-- Note: Old columns in members table will be kept for backward compatibility
-- They can be dropped later after confirming the new system works
