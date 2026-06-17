-- PURGE TEMPORARY PASSWORDS AFTER MEMBER CHANGES THEM (Security Hardening)
-- Purpose: Automatically delete temporary passwords from database once member sets their own
-- This ensures temporary passwords don't persist indefinitely if forgotten in the DB

UPDATE member_credentials
SET password = NULL,
    password_changed_at = NOW()
WHERE password_changed = TRUE 
  AND password IS NOT NULL;

-- Optional: Purge passwords older than 90 days (if you want aggressive cleanup)
-- Uncomment if your policy requires older passwords to be removed even if not changed
-- UPDATE member_credentials
-- SET password = NULL
-- WHERE password IS NOT NULL 
--   AND password_changed = FALSE
--   AND DATEDIFF(NOW(), created_at) > 90;

