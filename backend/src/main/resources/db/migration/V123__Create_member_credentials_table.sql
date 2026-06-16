-- Create member_credentials table to track password distribution
CREATE TABLE member_credentials (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    username VARCHAR(50) NOT NULL,
    member_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    has_national_id BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    email_sent_at TIMESTAMP NULL,
    password_changed BOOLEAN NOT NULL DEFAULT FALSE,
    password_changed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    
    INDEX idx_member_credentials_member_id (member_id),
    INDEX idx_member_credentials_username (username),
    INDEX idx_member_credentials_email_sent (email_sent),
    INDEX idx_member_credentials_password_changed (password_changed),
    
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comment for clarity
ALTER TABLE member_credentials COMMENT = 'Tracks member credential distribution and password setup status for admin visibility';