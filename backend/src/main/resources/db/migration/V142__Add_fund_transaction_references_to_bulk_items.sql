-- Add transaction reference columns for fund types to bulk_transaction_items
-- This allows proper rollback of MONTHLY_CONTRIBUTIONS batches

ALTER TABLE bulk_transaction_items ADD COLUMN benevolent_fund_transaction_id BIGINT;
ALTER TABLE bulk_transaction_items ADD COLUMN development_fund_transaction_id BIGINT;
ALTER TABLE bulk_transaction_items ADD COLUMN school_fees_transaction_id BIGINT;
ALTER TABLE bulk_transaction_items ADD COLUMN holiday_fund_transaction_id BIGINT;
ALTER TABLE bulk_transaction_items ADD COLUMN emergency_fund_transaction_id BIGINT;

-- Add foreign key constraints
ALTER TABLE bulk_transaction_items 
  ADD CONSTRAINT fk_benevolent_fund_transaction 
  FOREIGN KEY (benevolent_fund_transaction_id) 
  REFERENCES transactions(id) ON DELETE SET NULL;

ALTER TABLE bulk_transaction_items 
  ADD CONSTRAINT fk_development_fund_transaction 
  FOREIGN KEY (development_fund_transaction_id) 
  REFERENCES transactions(id) ON DELETE SET NULL;

ALTER TABLE bulk_transaction_items 
  ADD CONSTRAINT fk_school_fees_transaction 
  FOREIGN KEY (school_fees_transaction_id) 
  REFERENCES transactions(id) ON DELETE SET NULL;

ALTER TABLE bulk_transaction_items 
  ADD CONSTRAINT fk_holiday_fund_transaction 
  FOREIGN KEY (holiday_fund_transaction_id) 
  REFERENCES transactions(id) ON DELETE SET NULL;

ALTER TABLE bulk_transaction_items 
  ADD CONSTRAINT fk_emergency_fund_transaction 
  FOREIGN KEY (emergency_fund_transaction_id) 
  REFERENCES transactions(id) ON DELETE SET NULL;
