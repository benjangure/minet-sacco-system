-- V48: Add teller_message column to deposit_requests table
-- Allows teller to add optional message when approving/rejecting deposits

ALTER TABLE deposit_requests ADD COLUMN teller_message VARCHAR(500);
