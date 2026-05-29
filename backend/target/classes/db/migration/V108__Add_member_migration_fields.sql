ALTER TABLE bulk_member_items ADD COLUMN IF NOT EXISTS nok_relationship VARCHAR(50);
ALTER TABLE bulk_member_items ADD COLUMN IF NOT EXISTS bank_branch VARCHAR(100);
ALTER TABLE bulk_member_items ADD COLUMN IF NOT EXISTS date_joined DATE;
ALTER TABLE bulk_member_items ADD COLUMN IF NOT EXISTS opening_shares_balance DECIMAL(15,2);
ALTER TABLE bulk_member_items ADD COLUMN IF NOT EXISTS opening_savings_balance DECIMAL(15,2);
