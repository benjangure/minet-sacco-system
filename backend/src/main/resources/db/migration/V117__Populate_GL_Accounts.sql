-- ASSET ACCOUNTS
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('LOAN_NORMAL', 'Normal Loans', 'ASSET', 'AGGREGATION', 
  JSON_OBJECT('table','loans','field','outstanding_balance','where','loan_type = ''NORMAL'' AND status = ''DISBURSED'''), 10),

('LOAN_EMERGENCY_1', 'Emergency Loan Type 1', 'ASSET', 'AGGREGATION',
  JSON_OBJECT('table','loans','field','outstanding_balance','where','loan_type = ''EMERGENCY_1'' AND status = ''DISBURSED'''), 11),

('LOAN_EMERGENCY_2', 'Emergency Loan Type 2', 'ASSET', 'AGGREGATION',
  JSON_OBJECT('table','loans','field','outstanding_balance','where','loan_type = ''EMERGENCY_2'' AND status = ''DISBURSED'''), 12),

('CBA_CALL_DEPOSITS', 'CBA Call Deposits', 'ASSET', 'AGGREGATION',
  JSON_OBJECT('table','accounts','field','balance','where','SAVINGS account'), 20),

('CBA_CURRENT', 'CBA Current Account', 'ASSET', 'AGGREGATION',
  JSON_OBJECT('table','accounts','field','balance','where','SAVINGS account'), 21),

('CO_OP_HOLDINGS', 'Co-op Holdings', 'ASSET', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 30),
('COOP_INSURANCE', 'Co-op Insurance', 'ASSET', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 31),
('KUSCCO', 'KUSCCO', 'ASSET', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 32),
('RECEIVABLES', 'Receivables', 'ASSET', 'AGGREGATION', JSON_OBJECT('table','accounts','field','balance'), 33);

-- LIABILITY ACCOUNTS
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('MEMBER_DEPOSITS', 'Member Deposits', 'LIABILITY', 'AGGREGATION',
  JSON_OBJECT('table','accounts','field','balance','where','SAVINGS'), 40),

('MEMBER_SHARES', 'Member Shares', 'LIABILITY', 'AGGREGATION',
  JSON_OBJECT('table','accounts','field','balance','where','SHARES'), 41),

('AUDITOR_PAYABLE', 'Auditor Fees Payable', 'LIABILITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 50),
('DIVIDEND_PAYABLE', 'Dividend Payable', 'LIABILITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 51),
('INTEREST_PAYABLE', 'Interest Payable', 'LIABILITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 52),
('COMMITTEE_ALLOWANCE_PAYABLE', 'Committee Allowance Payable', 'LIABILITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 53);

-- EQUITY ACCOUNTS
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('STATUTORY_RESERVE', 'Statutory Reserve', 'EQUITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 60),
('REVENUE_RESERVE', 'Revenue Reserve', 'EQUITY', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 61),
('RETAINED_EARNINGS', 'Retained Earnings', 'EQUITY', 'COMPUTED', JSON_OBJECT('type','computed'), 62);

-- REVENUE ACCOUNTS
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('INT_LOANS', 'Interest - Loans', 'REVENUE', 'AGGREGATION',
  JSON_OBJECT('table','transactions','field','amount','where','INTEREST'), 70),

('INT_DEPOSITS', 'Interest - Deposits', 'REVENUE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 71),
('ENTRANCE_FEES', 'Entrance Fees', 'REVENUE', 'AGGREGATION',
  JSON_OBJECT('table','transactions','field','amount','where','ENTRANCE_FEE'), 72),
('LOAN_PROCESSING_FEE', 'Loan Processing Fees', 'REVENUE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 73);

-- EXPENSE ACCOUNTS
INSERT INTO gl_accounts (code, name, account_type, balance_calculation_type, calculation_config, display_order) VALUES
('AUDIT_FEES', 'Audit Fees', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 80),
('TRAVEL_EXPENSES', 'Travel Expenses', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 81),
('SASRA_FEES', 'SASRA Fees', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 82),
('TRAINING', 'Training', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 83),
('COMMITTEE_ALLOWANCES', 'Committee Allowances', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 84),
('AGM_EXPENSES', 'AGM Expenses', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 85),
('INSURANCE_PREMIUMS', 'Insurance Premiums', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 86),
('BANK_CHARGES', 'Bank Charges', 'EXPENSE', 'AGGREGATION',
  JSON_OBJECT('table','transactions','field','amount','where','BANK_CHARGE'), 87),
('LOAN_LOSS_PROVISION', 'Loan Loss Provision', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 88),
('INCOME_TAX', 'Income Tax', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 89),
('INTEREST_EXPENSE', 'Interest Expense', 'EXPENSE', 'MANUAL_ENTRY', JSON_OBJECT('type','manual'), 90);
