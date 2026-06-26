-- Make loan migration item fields nullable to support flexible CREATE/UPDATE modes
-- These fields were originally NOT NULL but need to be optional for:
-- CREATE mode (Loan Number blank): Other fields filled in later
-- UPDATE mode (Loan Number populated): Individual fields updated selectively
ALTER TABLE loan_migration_items MODIFY COLUMN employee_id VARCHAR(50) NULL;
ALTER TABLE loan_migration_items MODIFY COLUMN loan_product_name VARCHAR(100) NULL;
ALTER TABLE loan_migration_items MODIFY COLUMN principal_amount DECIMAL(15,2) NULL;
ALTER TABLE loan_migration_items MODIFY COLUMN term_months INT NULL;
ALTER TABLE loan_migration_items MODIFY COLUMN disbursement_date DATE NULL;
ALTER TABLE loan_migration_items MODIFY COLUMN loan_status VARCHAR(20) NULL;
ALTER TABLE loan_migration_items MODIFY COLUMN outstanding_balance DECIMAL(15,2) NULL;
ALTER TABLE loan_migration_items MODIFY COLUMN guarantorship_type VARCHAR(10) NULL;
