-- V148: Enhanced Member Exit Tracking
-- This migration improves exit tracking and adds audit columns

-- ============================================================
-- PART 1: Add exit tracking columns to members table
-- ============================================================

-- exitDate and exitReason already exist in entity, add new columns

ALTER TABLE members
ADD COLUMN exited_by BIGINT NULL COMMENT 'User ID who marked member as exited',
ADD COLUMN exit_notes TEXT NULL COMMENT 'Additional notes about the exit';

-- ============================================================
-- PART 2: Add guarantor replacement audit columns
-- ============================================================

ALTER TABLE guarantors
ADD COLUMN replaced_at DATETIME NULL COMMENT 'When this guarantor was replaced',
ADD COLUMN replaced_by_guarantor_id BIGINT NULL COMMENT 'ID of guarantor who replaced this one',
ADD COLUMN replacement_reason VARCHAR(100) NULL COMMENT 'Reason for replacement: MEMBER_EXIT, REJECTION, etc';

CREATE INDEX idx_guarantors_replaced_at ON guarantors(replaced_at);

ALTER TABLE topup_guarantors
ADD COLUMN replaced_at DATETIME NULL COMMENT 'When this guarantor was replaced',
ADD COLUMN replaced_by_guarantor_id BIGINT NULL COMMENT 'ID of guarantor who replaced this one',
ADD COLUMN replacement_reason VARCHAR(100) NULL COMMENT 'Reason for replacement: MEMBER_EXIT, REJECTION, etc';

CREATE INDEX idx_topup_guarantors_replaced_at ON topup_guarantors(replaced_at);
