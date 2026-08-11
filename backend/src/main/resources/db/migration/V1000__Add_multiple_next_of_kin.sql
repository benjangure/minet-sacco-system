-- V1000: Add Multiple Next of Kin with Percentage Allocation
-- Allows members to assign multiple beneficiaries with custom percentage shares

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
    CONSTRAINT chk_percentage CHECK (percentage >= 0 AND percentage <= 100),
    INDEX idx_next_of_kin_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Migrate existing next of kin data to new table
INSERT INTO next_of_kin (member_id, full_name, relationship, phone, percentage, is_primary)
SELECT 
    id,
    next_of_kin_name,
    COALESCE(next_of_kin_relationship, 'Not Specified'),
    next_of_kin_phone,
    100.00,
    TRUE
FROM members
WHERE next_of_kin_name IS NOT NULL 
  AND next_of_kin_name != ''
  AND next_of_kin_phone IS NOT NULL
  AND next_of_kin_phone != '';
