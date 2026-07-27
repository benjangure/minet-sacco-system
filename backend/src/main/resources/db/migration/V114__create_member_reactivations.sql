-- Create member_reactivations table
CREATE TABLE member_reactivations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    initiated_by BIGINT,
    initiated_at DATETIME,
    validated_by BIGINT,
    validated_at DATETIME,
    validation_notes TEXT,
    is_active TINYINT(1) DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',
    FOREIGN KEY (member_id) REFERENCES members(id),
    FOREIGN KEY (initiated_by) REFERENCES users(id),
    FOREIGN KEY (validated_by) REFERENCES users(id),
    INDEX idx_member_id (member_id),
    INDEX idx_status (status)
);
