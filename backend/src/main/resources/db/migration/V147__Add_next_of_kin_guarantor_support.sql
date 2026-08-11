-- V147: Add Next of Kin Guarantor Support
-- This migration adds support for backup (next of kin) guarantors
-- When a primary guarantor exits or defaults, their NOK can step in

-- ============================================================
-- PART 1: Add NOK columns to guarantors table
-- ============================================================

ALTER TABLE guarantors 
ADD COLUMN next_of_kin_guarantor_id BIGINT NULL COMMENT 'Reference to the NOK guarantor for this primary guarantor',
ADD COLUMN is_next_of_kin BOOLEAN DEFAULT FALSE COMMENT 'True if this is a next of kin (backup) guarantor',
ADD COLUMN primary_guarantor_id BIGINT NULL COMMENT 'Reference to the primary guarantor this NOK is backing';

-- Add foreign key constraints
ALTER TABLE guarantors 
ADD CONSTRAINT fk_guarantor_next_of_kin 
    FOREIGN KEY (next_of_kin_guarantor_id) 
    REFERENCES guarantors(id) 
    ON DELETE SET NULL;

ALTER TABLE guarantors 
ADD CONSTRAINT fk_guarantor_primary 
    FOREIGN KEY (primary_guarantor_id) 
    REFERENCES guarantors(id) 
    ON DELETE CASCADE;

-- Add indexes for performance
CREATE INDEX idx_guarantors_next_of_kin ON guarantors(next_of_kin_guarantor_id);
CREATE INDEX idx_guarantors_primary ON guarantors(primary_guarantor_id);
CREATE INDEX idx_guarantors_is_nok ON guarantors(is_next_of_kin);

-- ============================================================
-- PART 2: Add NOK columns to topup_guarantors table
-- ============================================================

ALTER TABLE topup_guarantors 
ADD COLUMN next_of_kin_guarantor_id BIGINT NULL COMMENT 'Reference to the NOK guarantor for this primary guarantor',
ADD COLUMN is_next_of_kin BOOLEAN DEFAULT FALSE COMMENT 'True if this is a next of kin (backup) guarantor',
ADD COLUMN primary_guarantor_id BIGINT NULL COMMENT 'Reference to the primary guarantor this NOK is backing';

-- Add foreign key constraints
ALTER TABLE topup_guarantors 
ADD CONSTRAINT fk_topup_guarantor_next_of_kin 
    FOREIGN KEY (next_of_kin_guarantor_id) 
    REFERENCES topup_guarantors(id) 
    ON DELETE SET NULL;

ALTER TABLE topup_guarantors 
ADD CONSTRAINT fk_topup_guarantor_primary 
    FOREIGN KEY (primary_guarantor_id) 
    REFERENCES topup_guarantors(id) 
    ON DELETE CASCADE;

-- Add indexes for performance
CREATE INDEX idx_topup_guarantors_next_of_kin ON topup_guarantors(next_of_kin_guarantor_id);
CREATE INDEX idx_topup_guarantors_primary ON topup_guarantors(primary_guarantor_id);
CREATE INDEX idx_topup_guarantors_is_nok ON topup_guarantors(is_next_of_kin);

-- ============================================================
-- PART 3: Add new status values for guarantor replacement
-- ============================================================

-- Note: Status is an ENUM in Java entity, but stored as VARCHAR in DB
-- New statuses to support:
-- - REPLACED_DUE_TO_EXIT: Primary guarantor was replaced because they exited
-- - ACTIVATED_FROM_NOK: NOK guarantor was promoted to primary

-- No DB change needed here, just documentation for application layer
