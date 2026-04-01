-- Performance optimization indexes for users and notifications tables

-- Index for username lookups (used in authentication)
CREATE INDEX idx_users_username ON users(username);

-- Note: notifications table doesn't have user_id column, so skipping user-based indexes
-- The notifications table structure is: id, user_id, message, type, read/is_read, created_at
-- These indexes would be added if the schema supported them
