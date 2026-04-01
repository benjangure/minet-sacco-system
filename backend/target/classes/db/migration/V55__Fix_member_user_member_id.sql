-- Fix member users that don't have memberId set
-- Match users with role MEMBER to members by phone number or employee ID

-- Update users where memberId is NULL and role is MEMBER
-- Try to match by phone first
UPDATE users u
INNER JOIN members m ON m.phone = u.username
SET u.member_id = m.id
WHERE u.role = 'MEMBER' 
AND u.member_id IS NULL;

-- For any remaining users without memberId, try to match by employee ID
UPDATE users u
INNER JOIN members m ON m.employee_id = u.username
SET u.member_id = m.id
WHERE u.role = 'MEMBER' 
AND u.member_id IS NULL;
