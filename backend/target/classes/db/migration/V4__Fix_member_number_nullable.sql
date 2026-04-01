-- V4__Fix_member_number_nullable.sql
-- Make member_number nullable since it's auto-generated after approval

ALTER TABLE members MODIFY COLUMN member_number VARCHAR(20) UNIQUE NULL;
