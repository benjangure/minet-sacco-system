-- Add indexes to audit_logs for better query performance
ALTER TABLE audit_logs ADD INDEX idx_action (action);
ALTER TABLE audit_logs ADD INDEX idx_entity_type (entity_type);
ALTER TABLE audit_logs ADD INDEX idx_entity_id (entity_id);
ALTER TABLE audit_logs ADD INDEX idx_timestamp (timestamp);
ALTER TABLE audit_logs ADD INDEX idx_user_id_action (user_id, action);
ALTER TABLE audit_logs ADD INDEX idx_entity_type_entity_id_action (entity_type, entity_id, action);
