CREATE TABLE user_account (
    id VARCHAR(36) PRIMARY KEY,
    email_canonical VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    email_display VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    auth_version BIGINT NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_user_email UNIQUE (email_canonical)
);
CREATE TABLE user_profile (
    user_id VARCHAR(36) PRIMARY KEY,
    nickname VARCHAR(30) NOT NULL,
    signature VARCHAR(150),
    avatar_media_id VARCHAR(36),
    timezone VARCHAR(64) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);
CREATE TABLE legal_document (
    document_type VARCHAR(32) NOT NULL,
    version VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    effective_at DATETIME(6) NOT NULL,
    PRIMARY KEY (document_type, version),
    INDEX idx_legal_current (document_type, status, effective_at)
);
CREATE TABLE user_consent (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    purpose VARCHAR(32) NOT NULL,
    document_version VARCHAR(64) NOT NULL,
    accepted_at DATETIME(6) NOT NULL,
    withdrawn_at DATETIME(6),
    CONSTRAINT fk_consent_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT uk_user_consent UNIQUE (user_id, purpose, document_version)
);
CREATE TABLE security_audit (
    event_id VARCHAR(36) PRIMARY KEY,
    actor_id VARCHAR(36),
    action VARCHAR(64) NOT NULL,
    result VARCHAR(32) NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    request_id VARCHAR(80) NOT NULL,
    INDEX idx_audit_actor_time (actor_id, occurred_at)
);
