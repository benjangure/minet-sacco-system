-- Create audit logs table for tracking all system actions
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    entity_details LONGTEXT,
    comments LONGTEXT,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'SUCCESS',
    error_message LONGTEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(100),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_entity_type (entity_type),
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_entity_id (entity_id),
    INDEX idx_audit_status (status)
);

-- Add missing columns if they don't exist (for idempotency)
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS comments LONGTEXT;
