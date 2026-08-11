-- Create loan_topup_requests table
CREATE TABLE IF NOT EXISTS loan_topup_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    loan_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    requested_amount DECIMAL(15, 2) NOT NULL,
    purpose TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING_GUARANTOR_APPROVAL',
    requested_date DATETIME NOT NULL,
    reviewed_by BIGINT,
    review_date DATETIME,
    rejection_reason TEXT,
    disbursed_by BIGINT,
    disbursement_date DATETIME,
    total_guarantee_amount DECIMAL(15, 2) DEFAULT 0.00,
    guarantor_approval_count INT DEFAULT 0,
    guarantor_rejection_count INT DEFAULT 0,
    all_guarantors_approved BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_topup_request_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE CASCADE,
    CONSTRAINT fk_topup_request_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    CONSTRAINT fk_topup_request_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_topup_request_disbursed_by FOREIGN KEY (disbursed_by) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_topup_request_loan (loan_id),
    INDEX idx_topup_request_member (member_id),
    INDEX idx_topup_request_status (status),
    INDEX idx_topup_request_date (requested_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create topup_guarantors table
CREATE TABLE IF NOT EXISTS topup_guarantors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topup_request_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    guarantee_amount DECIMAL(15, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    requested_date DATETIME NOT NULL,
    approved_at DATETIME,
    rejected_at DATETIME,
    rejection_reason TEXT,
    pledge_amount DECIMAL(15, 2) DEFAULT 0.00,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_topup_guarantor_request FOREIGN KEY (topup_request_id) REFERENCES loan_topup_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_topup_guarantor_member FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    
    INDEX idx_topup_guarantor_request (topup_request_id),
    INDEX idx_topup_guarantor_member (member_id),
    INDEX idx_topup_guarantor_status (status),
    INDEX idx_topup_guarantor_date (requested_date),
    
    UNIQUE KEY uk_topup_guarantor_unique (topup_request_id, member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
