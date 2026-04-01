-- Add exit tracking fields to members table
ALTER TABLE members ADD COLUMN exit_date TIMESTAMP NULL;
ALTER TABLE members ADD COLUMN exit_reason VARCHAR(100) NULL;

-- Create index for exit tracking queries
CREATE INDEX idx_members_exit_date ON members(exit_date);
CREATE INDEX idx_members_status_exit ON members(status, exit_date);
