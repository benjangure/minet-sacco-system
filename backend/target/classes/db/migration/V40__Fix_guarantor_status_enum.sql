-- V40__Fix_guarantor_status_enum.sql
-- Fix guarantors.status ENUM to include all values used by the application
-- Original: ENUM('PENDING', 'ACCEPTED', 'DECLINED')
-- Required: ENUM('PENDING', 'ACCEPTED', 'DECLINED', 'REJECTED', 'ACTIVE')

ALTER TABLE guarantors
    MODIFY COLUMN status ENUM('PENDING', 'ACCEPTED', 'DECLINED', 'REJECTED', 'ACTIVE') DEFAULT 'PENDING';
