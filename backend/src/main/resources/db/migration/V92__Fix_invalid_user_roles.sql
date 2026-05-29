-- Fix invalid user roles that don't exist in the User.Role enum
-- HR_STAFF is not a valid role, change it to TELLER (closest match for staff)

UPDATE users 
SET role = 'TELLER' 
WHERE role = 'HR_STAFF';

-- Log the change
-- This migration fixes users with invalid roles that cause enum parsing errors
