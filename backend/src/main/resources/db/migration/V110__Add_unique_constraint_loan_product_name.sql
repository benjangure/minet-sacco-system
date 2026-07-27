-- Keep the ORIGINAL loan product (lowest ID) when duplicates exist,
-- since existing loans were already applied against the original product.
-- Delete the newer duplicates (higher IDs).
DELETE FROM loan_products
WHERE id NOT IN (
    SELECT MIN(id) FROM loan_products GROUP BY name
);

-- Add unique constraint to prevent future duplicates
ALTER TABLE loan_products ADD CONSTRAINT uq_loan_product_name UNIQUE (name);
