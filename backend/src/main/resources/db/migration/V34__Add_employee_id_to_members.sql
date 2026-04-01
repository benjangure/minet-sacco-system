-- V34: Add employee_id field to members table and use it as the member identifier
-- Members will use their employee ID as their SACCO identifier instead of auto-generated numbers

ALTER TABLE members ADD COLUMN employee_id VARCHAR(50) NULL;
ALTER TABLE members ADD CONSTRAINT uq_members_employee_id UNIQUE (employee_id);

-- Also add a MEMBER role to users for future mobile app login
-- (No data changes needed here - credentials are created at approval time)
