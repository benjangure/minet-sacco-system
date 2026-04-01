-- V2__Insert_initial_data.sql
-- Initial test data for Minet SACCO system

-- Insert default admin user (password: admin123)
-- Using BCrypt with strength 10
INSERT INTO users (username, password, email, role, enabled) VALUES
('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin@minet.co.ke', 'ADMIN', TRUE),
('treasurer', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'treasurer@minet.co.ke', 'TREASURER', TRUE),
('loan_officer', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'loans@minet.co.ke', 'LOAN_OFFICER', TRUE),
('credit_committee', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'committee@minet.co.ke', 'CREDIT_COMMITTEE', TRUE);

-- Insert sample loan products
INSERT INTO loan_products (name, description, interest_rate, max_amount, min_amount, term_months) VALUES
('Emergency Loan', 'Quick loan for emergencies, processed within 24 hours', 12.00, 100000.00, 5000.00, 12),
('Development Loan', 'Long-term loan for property development and major investments', 10.00, 5000000.00, 100000.00, 60),
('School Fees Loan', 'Education loan for school fees payment', 8.00, 500000.00, 10000.00, 24),
('Asset Financing', 'Loan for purchasing vehicles, equipment, or other assets', 11.00, 2000000.00, 50000.00, 48);

-- Insert sample members
INSERT INTO members (member_number, first_name, last_name, email, phone, national_id, date_of_birth, employment_status, employer, department, status) VALUES
('M001', 'John', 'Kamau', 'john.kamau@minet.co.ke', '0712345678', '12345678', '1985-03-15', 'PERMANENT', 'Minet Insurance', 'IT', 'ACTIVE'),
('M002', 'Jane', 'Wanjiku', 'jane.wanjiku@minet.co.ke', '0723456789', '23456789', '1990-07-22', 'PERMANENT', 'Minet Insurance', 'Finance', 'ACTIVE'),
('M003', 'Peter', 'Ochieng', 'peter.ochieng@minet.co.ke', '0734567890', '34567890', '1988-11-10', 'PERMANENT', 'Minet Insurance', 'Operations', 'ACTIVE'),
('M004', 'Mary', 'Akinyi', 'mary.akinyi@minet.co.ke', '0745678901', '45678901', '1992-05-18', 'CONTRACT', 'Minet Insurance', 'HR', 'ACTIVE'),
('M005', 'David', 'Mwangi', 'david.mwangi@minet.co.ke', '0756789012', '56789012', '1987-09-25', 'PERMANENT', 'Minet Insurance', 'Sales', 'ACTIVE');

-- Insert sample accounts for members
INSERT INTO accounts (member_id, account_type, balance) VALUES
(1, 'SAVINGS', 50000.00),
(1, 'SHARES', 100000.00),
(2, 'SAVINGS', 75000.00),
(2, 'SHARES', 150000.00),
(3, 'SAVINGS', 30000.00),
(3, 'SHARES', 80000.00),
(4, 'SAVINGS', 45000.00),
(4, 'SHARES', 120000.00),
(5, 'SAVINGS', 60000.00),
(5, 'SHARES', 200000.00);
