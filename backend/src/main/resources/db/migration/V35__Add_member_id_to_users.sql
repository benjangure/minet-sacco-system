-- V35: Add member_id to users table to link member accounts for mobile app login
ALTER TABLE users ADD COLUMN member_id BIGINT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_member_id FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE SET NULL;
