-- V37: Add global maximum loan term setting to loan_eligibility_rules
-- Standard Kenyan SACCO maximum is 72 months (6 years)
ALTER TABLE loan_eligibility_rules ADD COLUMN max_loan_term_months INT NOT NULL DEFAULT 72;
