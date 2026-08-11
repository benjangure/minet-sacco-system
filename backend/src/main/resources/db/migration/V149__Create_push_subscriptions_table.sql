-- Migration V149: Create push_subscriptions table for Web Push Notifications
-- This table stores push notification subscription data for PWA functionality

CREATE TABLE IF NOT EXISTS push_subscriptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    p256dh_key VARCHAR(255) NOT NULL,
    auth_key VARCHAR(255) NOT NULL,
    user_agent VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Foreign key constraint
    CONSTRAINT fk_push_subscription_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate subscriptions
    CONSTRAINT uk_user_endpoint 
        UNIQUE (user_id, endpoint),
    
    -- Index for better query performance
    INDEX idx_user_active (user_id, is_active),
    INDEX idx_last_used (last_used_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add comment to table
ALTER TABLE push_subscriptions COMMENT = 'Stores Web Push notification subscriptions for PWA functionality';
