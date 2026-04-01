-- Delete old notifications that don't have a target_role set
-- These are from before the role-based filtering was implemented
DELETE FROM notifications WHERE target_role IS NULL;
