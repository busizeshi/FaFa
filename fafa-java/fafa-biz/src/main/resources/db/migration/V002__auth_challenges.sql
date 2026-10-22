CREATE TABLE auth_challenge (
    id VARCHAR(36) PRIMARY KEY,
    purpose VARCHAR(32) NOT NULL,
    target_canonical VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    code_hmac VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL,
    consumed_at DATETIME(6),
    send_state VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    INDEX idx_challenge_target (target_canonical, purpose, created_at),
    INDEX idx_challenge_expiry (expires_at)
);
CREATE TABLE auth_ticket (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6),
    version BIGINT NOT NULL DEFAULT 1,
    INDEX idx_ticket_user (user_id, purpose),
    INDEX idx_ticket_expiry (expires_at)
);
