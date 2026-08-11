-- Fix MySQL reserved word conflict: row_number
-- MySQL 8.0+ added ROW_NUMBER() as a window function, making 'row_number' a reserved word
-- Backticks are required around reserved words when used as identifiers

ALTER TABLE bulk_transaction_items CHANGE COLUMN `row_number` `item_row_number` INT NOT NULL;

ALTER TABLE bulk_loan_items CHANGE COLUMN `row_number` `item_row_number` INT NOT NULL;

ALTER TABLE bulk_member_items CHANGE COLUMN `row_number` `item_row_number` INT NOT NULL;

ALTER TABLE bulk_loan_data_update_items CHANGE COLUMN `row_number` `item_row_number` INT NULL;

ALTER TABLE loan_migration_items CHANGE COLUMN `row_number` `item_row_number` INT NOT NULL;

-- Verify the changes
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND COLUMN_NAME = 'item_row_number'
ORDER BY TABLE_NAME;
