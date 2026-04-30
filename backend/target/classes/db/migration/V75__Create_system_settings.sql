-- V75__Create_system_settings.sql
-- Create system settings table for admin-configurable rules

CREATE TABLE IF NOT EXISTS system_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL COMMENT 'Unique setting identifier',
    setting_value VARCHAR(255) NOT NULL COMMENT 'Setting value',
    setting_type VARCHAR(50) COMMENT 'Type: INTEGER, DECIMAL, BOOLEAN, STRING',
    description LONGTEXT COMMENT 'Setting description',
    updated_by BIGINT COMMENT 'User who last updated',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (updated_by) REFERENCES users(id),
    INDEX idx_setting_key (setting_key),
    INDEX idx_updated_at (updated_at)
) COMMENT 'System-wide configurable settings';

-- Insert default settings
INSERT INTO system_settings (setting_key, setting_value, setting_type, description) VALUES
    ('MAX_ACTIVE_LOANS', '3', 'INTEGER', 'Maximum number of active loans per member'),
    ('LOAN_MULTIPLIER', '3', 'DECIMAL', 'Loan multiplier (3x savings)'),
    ('MIN_CONTRIBUTION_MONTHS', '6', 'INTEGER', 'Minimum contribution months required for loan eligibility'),
    ('EMERGENCY_FUND_ENABLED', 'false', 'BOOLEAN', 'Whether emergency fund is enabled'),
    ('TEST_MODE_OVERRIDE', 'false', 'BOOLEAN', 'Test mode override for development')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
