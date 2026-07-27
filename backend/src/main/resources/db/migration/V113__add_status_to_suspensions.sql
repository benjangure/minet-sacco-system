-- Add status column to member_suspensions table
ALTER TABLE member_suspensions
ADD COLUMN status VARCHAR(50) DEFAULT 'PENDING';

-- Update existing active suspensions to APPROVED
UPDATE member_suspensions SET status = 'APPROVED' WHERE is_active = true;

-- Update existing inactive suspensions to APPROVED (they were lifted)
UPDATE member_suspensions SET status = 'APPROVED' WHERE is_active = false;
