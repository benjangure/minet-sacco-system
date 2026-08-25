-- ============================================================================
-- V150: Default SACCO Chart of Accounts
-- ============================================================================
-- Inserts a standard chart of accounts only if gl_accounts is empty.
-- The treasurer can edit, deactivate, or add accounts through the UI at any time.
-- Config format: {"table":"...", "accountType/transactionType/loanProductId":"..."}
-- ============================================================================

INSERT INTO gl_accounts
  (code, name, account_type, balance_calculation_type, calculation_config,
   normal_balance, section_label, period_sensitive, display_order, is_active)
SELECT * FROM (SELECT
  '1001' AS code,
  'Cash and Bank' AS name,
  'ASSET' AS account_type,
  'AGGREGATION' AS balance_calculation_type,
  '{"table":"transactions","transactionType":"DEPOSIT"}' AS calculation_config,
  'DEBIT' AS normal_balance,
  'Current Assets' AS section_label,
  FALSE AS period_sensitive,
  10 AS display_order,
  TRUE AS is_active
UNION ALL SELECT '1002','Loan Portfolio (Outstanding)','ASSET','AGGREGATION',
  '{"table":"loans"}','DEBIT','Non-Current Assets',FALSE,20,TRUE
UNION ALL SELECT '1003','Interest Receivable','ASSET','AGGREGATION',
  '{"table":"transactions","transactionType":"INTEREST"}','DEBIT','Current Assets',FALSE,30,TRUE
UNION ALL SELECT '1004','Other Assets','ASSET','MANUAL_ENTRY',
  '{}','DEBIT','Non-Current Assets',FALSE,40,TRUE

-- LIABILITIES
UNION ALL SELECT '2001','Member Savings Deposits','LIABILITY','AGGREGATION',
  '{"table":"accounts","accountType":"SAVINGS"}','CREDIT','Current Liabilities',FALSE,110,TRUE
UNION ALL SELECT '2002','Member Share Capital','LIABILITY','AGGREGATION',
  '{"table":"accounts","accountType":"SHARES"}','CREDIT','Non-Current Liabilities',FALSE,120,TRUE
UNION ALL SELECT '2003','Benevolent Fund','LIABILITY','AGGREGATION',
  '{"table":"accounts","accountType":"BENEVOLENT_FUND"}','CREDIT','Non-Current Liabilities',FALSE,130,TRUE
UNION ALL SELECT '2004','Development Fund','LIABILITY','AGGREGATION',
  '{"table":"accounts","accountType":"DEVELOPMENT_FUND"}','CREDIT','Non-Current Liabilities',FALSE,140,TRUE
UNION ALL SELECT '2005','School Fees Fund','LIABILITY','AGGREGATION',
  '{"table":"accounts","accountType":"SCHOOL_FEES"}','CREDIT','Non-Current Liabilities',FALSE,150,TRUE
UNION ALL SELECT '2006','Holiday Fund','LIABILITY','AGGREGATION',
  '{"table":"accounts","accountType":"HOLIDAY_FUND"}','CREDIT','Non-Current Liabilities',FALSE,160,TRUE
UNION ALL SELECT '2007','Emergency Fund','LIABILITY','AGGREGATION',
  '{"table":"accounts","accountType":"EMERGENCY_FUND"}','CREDIT','Non-Current Liabilities',FALSE,170,TRUE
UNION ALL SELECT '2008','Accrued Expenses','LIABILITY','MANUAL_ENTRY',
  '{}','CREDIT','Current Liabilities',FALSE,180,TRUE
UNION ALL SELECT '2009','Dividends Payable','LIABILITY','MANUAL_ENTRY',
  '{}','CREDIT','Current Liabilities',TRUE,190,TRUE

-- EQUITY
UNION ALL SELECT '3001','Retained Earnings (Net Income)','EQUITY','COMPUTED',
  '{"compute":"RETAINED_EARNINGS"}','CREDIT','Equity',FALSE,210,TRUE
UNION ALL SELECT '3002','Statutory Reserve Fund','EQUITY','MANUAL_ENTRY',
  '{}','CREDIT','Equity',FALSE,220,TRUE
UNION ALL SELECT '3003','Share Capital Surplus','EQUITY','MANUAL_ENTRY',
  '{}','CREDIT','Equity',FALSE,230,TRUE

-- REVENUE
UNION ALL SELECT '4001','Interest Income from Loans','REVENUE','AGGREGATION',
  '{"table":"transactions","transactionType":"INTEREST"}','CREDIT','Operating Revenue',TRUE,310,TRUE
UNION ALL SELECT '4002','Loan Repayment Income','REVENUE','AGGREGATION',
  '{"table":"transactions","transactionType":"LOAN_REPAYMENT"}','CREDIT','Operating Revenue',TRUE,320,TRUE
UNION ALL SELECT '4003','Entrance / Registration Fees','REVENUE','AGGREGATION',
  '{"table":"transactions","transactionType":"ENTRANCE_FEE"}','CREDIT','Other Revenue',TRUE,330,TRUE
UNION ALL SELECT '4004','Late Payment Penalties','REVENUE','MANUAL_ENTRY',
  '{}','CREDIT','Other Revenue',TRUE,340,TRUE
UNION ALL SELECT '4005','Other Income','REVENUE','MANUAL_ENTRY',
  '{}','CREDIT','Other Revenue',TRUE,350,TRUE

-- EXPENSES
UNION ALL SELECT '5001','Staff Salaries and Benefits','EXPENSE','MANUAL_ENTRY',
  '{}','DEBIT','Operating Expenses',TRUE,410,TRUE
UNION ALL SELECT '5002','Office Rent','EXPENSE','MANUAL_ENTRY',
  '{}','DEBIT','Operating Expenses',TRUE,420,TRUE
UNION ALL SELECT '5003','Bank Charges','EXPENSE','AGGREGATION',
  '{"table":"transactions","transactionType":"BANK_CHARGE"}','DEBIT','Operating Expenses',TRUE,430,TRUE
UNION ALL SELECT '5004','Audit and Professional Fees','EXPENSE','MANUAL_ENTRY',
  '{}','DEBIT','Operating Expenses',TRUE,440,TRUE
UNION ALL SELECT '5005','Loan Loss Provision','EXPENSE','MANUAL_ENTRY',
  '{}','DEBIT','Operating Expenses',TRUE,450,TRUE
UNION ALL SELECT '5006','Depreciation','EXPENSE','MANUAL_ENTRY',
  '{}','DEBIT','Operating Expenses',TRUE,460,TRUE
UNION ALL SELECT '5007','Other Operating Expenses','EXPENSE','MANUAL_ENTRY',
  '{}','DEBIT','Operating Expenses',TRUE,470,TRUE
) AS defaults
WHERE NOT EXISTS (SELECT 1 FROM gl_accounts LIMIT 1);
