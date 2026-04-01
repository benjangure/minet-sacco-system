-- V41__Add_released_to_guarantor_status.sql
-- Add RELEASED status to guarantors.status ENUM
-- RELEASED means the loan has been fully repaid and the guarantor's pledged savings are unfrozen

ALTER TABLE guarantors
    MODIFY COLUMN status ENUM('PENDING', 'ACCEPTED', 'DECLINED', 'REJECTED', 'ACTIVE', 'RELEASED') DEFAULT 'PENDING';
