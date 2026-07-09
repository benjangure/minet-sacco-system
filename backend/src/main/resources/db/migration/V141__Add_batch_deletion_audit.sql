CREATE TABLE batch_deletion_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    batch_number VARCHAR(100),
    batch_type VARCHAR(50),
    deleted_by_user_id BIGINT NOT NULL,
    deleted_by_username VARCHAR(100),
    deleted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(500),
    rollback_status VARCHAR(20) DEFAULT 'COMPLETED',
    loans_deleted INT DEFAULT 0,
    guarantors_released INT DEFAULT 0,
    transactions_reversed INT DEFAULT 0,
    accounts_adjusted INT DEFAULT 0,
    error_message VARCHAR(1000),
    CONSTRAINT fk_deletion_audit_user FOREIGN KEY (deleted_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_batch_id ON batch_deletion_audit(batch_id);
CREATE INDEX idx_deleted_by ON batch_deletion_audit(deleted_by_user_id);
CREATE INDEX idx_deleted_at ON batch_deletion_audit(deleted_at);
