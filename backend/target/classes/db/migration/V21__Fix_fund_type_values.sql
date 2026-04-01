-- V21: Fix fund_type values to uppercase (STRING enum storage)
-- Delete all existing funds
DELETE FROM fund_configurations;

-- Re-insert with correct uppercase values
INSERT INTO fund_configurations (fund_type, enabled, display_name, description, minimum_amount, maximum_amount, display_order) VALUES
('EMERGENCY_FUND', TRUE, 'Emergency Fund', 'Personal emergency savings', 0.00, 1000000.00, 1),
('BENEVOLENT_FUND', FALSE, 'Benevolent Fund', 'Welfare fund for funerals and medical emergencies', 0.00, 1000000.00, 2),
('DEVELOPMENT_FUND', FALSE, 'Development Fund', 'SACCO development and infrastructure projects', 0.00, 1000000.00, 3),
('SCHOOL_FEES', FALSE, 'School Fees Fund', 'Education fund for children (withdrawable in Jan/Apr/Sep)', 0.00, 1000000.00, 4),
('HOLIDAY_FUND', FALSE, 'Holiday Fund', 'Christmas and holiday savings (withdrawable in December)', 0.00, 1000000.00, 5);
