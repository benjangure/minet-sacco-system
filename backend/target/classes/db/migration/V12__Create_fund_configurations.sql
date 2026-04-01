-- V12: Create fund_configurations table for optional fund management

CREATE TABLE fund_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fund_type VARCHAR(50) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    display_name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    minimum_amount DECIMAL(15,2) DEFAULT 0.00,
    maximum_amount DECIMAL(15,2) DEFAULT 1000000.00,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- Insert default configuration (only Emergency Fund enabled for Minet)
INSERT INTO fund_configurations (fund_type, enabled, display_name, description, minimum_amount, maximum_amount, display_order) VALUES
('EMERGENCY_FUND', TRUE, 'Emergency Fund', 'Personal emergency savings', 0.00, 1000000.00, 1),
('BENEVOLENT_FUND', FALSE, 'Benevolent Fund', 'Welfare fund for funerals and medical emergencies', 0.00, 1000000.00, 2),
('DEVELOPMENT_FUND', FALSE, 'Development Fund', 'SACCO development and infrastructure projects', 0.00, 1000000.00, 3),
('SCHOOL_FEES', FALSE, 'School Fees Fund', 'Education fund for children (withdrawable in Jan/Apr/Sep)', 0.00, 1000000.00, 4),
('HOLIDAY_FUND', FALSE, 'Holiday Fund', 'Christmas and holiday savings (withdrawable in December)', 0.00, 1000000.00, 5);

-- Create index for faster queries
CREATE INDEX idx_fund_configurations_enabled ON fund_configurations(enabled);
CREATE INDEX idx_fund_configurations_display_order ON fund_configurations(display_order);
