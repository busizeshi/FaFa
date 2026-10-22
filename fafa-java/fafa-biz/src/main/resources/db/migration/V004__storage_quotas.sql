CREATE TABLE quota_account (
    user_id VARCHAR(36) NOT NULL,
    bucket VARCHAR(32) NOT NULL,
    granted BIGINT NOT NULL,
    used BIGINT NOT NULL,
    reserved BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY(user_id,bucket),
    CONSTRAINT fk_quota_user FOREIGN KEY(user_id) REFERENCES user_account(id),
    CONSTRAINT ck_quota_balance CHECK(granted>=0 AND used>=0 AND reserved>=0 AND granted-used>=reserved)
);
CREATE TABLE quota_reservation (
    operation_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    bucket VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    settled_amount BIGINT,
    state VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_reservation_owner_state(user_id,state,created_at)
);
CREATE TABLE quota_ledger (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    bucket VARCHAR(32) NOT NULL,
    operation_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    entry_type VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    operator_id VARCHAR(36),
    reason VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_ledger_operation UNIQUE(operation_id,entry_type),
    INDEX idx_ledger_user(user_id,bucket,id)
);
CREATE TABLE grant_request (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    bucket VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    state VARCHAR(16) NOT NULL,
    granted_amount BIGINT,
    decision_reason VARCHAR(500),
    operator_id VARCHAR(36),
    version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_grant_user(user_id,id),
    INDEX idx_grant_state(state,id)
);
CREATE TABLE admin_business_operation (
    operation_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    operator_id VARCHAR(36) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    request_json LONGTEXT NOT NULL,
    response_json LONGTEXT,
    created_at DATETIME(6) NOT NULL
);
