-- Increase error_message column size for bulk_member_items to accommodate multiple validation errors
ALTER TABLE bulk_member_items MODIFY COLUMN error_message VARCHAR(2000);

-- Also increase for other bulk tables to be consistent
ALTER TABLE bulk_transaction_items MODIFY COLUMN error_message VARCHAR(2000);
ALTER TABLE bulk_loan_items MODIFY COLUMN error_message VARCHAR(2000);
ALTER TABLE bulk_disbursement_items MODIFY COLUMN error_message VARCHAR(2000);
