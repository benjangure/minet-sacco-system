-- Check if EMP019 and EMP020 user accounts exist
SELECT username, password, role, member_id, enabled FROM users WHERE username IN ('EMP019', 'EMP020');

-- Check member data
SELECT id, member_number, first_name, last_name, national_id FROM members WHERE member_number IN ('EMP019', 'EMP020');
