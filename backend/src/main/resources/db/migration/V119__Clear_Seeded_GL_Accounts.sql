-- GL accounts were previously seeded in V117. They are now created and managed by the treasurer through the UI configuration page. This migration clears the hardcoded seed data.

-- GL accounts are now managed by the treasurer via UI, not migrations
DELETE FROM gl_manual_entries;
DELETE FROM gl_account_calculations;
DELETE FROM gl_account_audit;
DELETE FROM gl_accounts;
