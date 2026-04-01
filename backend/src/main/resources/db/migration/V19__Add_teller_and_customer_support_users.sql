-- V19__Add_teller_and_customer_support_users.sql
-- Add TELLER and CUSTOMER_SUPPORT users for KYC workflow testing

-- Insert TELLER user (password: teller123)
INSERT INTO users (username, password, email, role, enabled) VALUES
('teller', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'teller@minet.co.ke', 'TELLER', TRUE);

-- Insert CUSTOMER_SUPPORT user (password: support123)
INSERT INTO users (username, password, email, role, enabled) VALUES
('customer_support', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'support@minet.co.ke', 'CUSTOMER_SUPPORT', TRUE);
