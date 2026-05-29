-- Add approval fields to member_exits table
ALTER TABLE member_exits 
ADD COLUMN approval_notes TEXT,
ADD COLUMN status VARCHAR(50);

-- Set default status for existing records
UPDATE member_exits SET status = 'PENDING' WHERE status IS NULL;
