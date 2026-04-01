-- V38: Fix loan product min/max terms to match Kenyan SACCO industry standards
-- Emergency Loan: 1–12 months (short-term, urgent needs)
UPDATE loan_products SET min_term_months = 1, max_term_months = 12  WHERE name = 'Emergency Loan';

-- Development Loan: 12–72 months (long-term, property/major investments — SACCO max)
UPDATE loan_products SET min_term_months = 12, max_term_months = 72 WHERE name = 'Development Loan';

-- School Fees Loan: 1–12 months (tied to academic calendar)
UPDATE loan_products SET min_term_months = 1, max_term_months = 12  WHERE name = 'School Fees Loan';

-- Asset Financing: 6–48 months (vehicles, equipment)
UPDATE loan_products SET min_term_months = 6, max_term_months = 48  WHERE name = 'Asset Financing';
