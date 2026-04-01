-- Add target_role column to notifications table to track which role the notification was intended for
ALTER TABLE notifications ADD COLUMN target_role VARCHAR(50) NULL AFTER type;

-- Add index for filtering by role
CREATE INDEX idx_notifications_user_role ON notifications(user_id, target_role);
