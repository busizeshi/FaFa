CREATE TABLE admin_account (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    auth_version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL
);
CREATE TABLE admin_role (
    id VARCHAR(32) PRIMARY KEY,
    name VARCHAR(80) NOT NULL
);
CREATE TABLE admin_permission (
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(80) NOT NULL
);
CREATE TABLE admin_account_role (
    admin_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(32) NOT NULL,
    PRIMARY KEY(admin_id,role_id),
    FOREIGN KEY(admin_id) REFERENCES admin_account(id),
    FOREIGN KEY(role_id) REFERENCES admin_role(id)
);
CREATE TABLE admin_role_permission (
    role_id VARCHAR(32) NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    PRIMARY KEY(role_id,permission_code),
    FOREIGN KEY(role_id) REFERENCES admin_role(id),
    FOREIGN KEY(permission_code) REFERENCES admin_permission(code)
);
CREATE TABLE admin_session (
    token_hash VARCHAR(64) PRIMARY KEY,
    admin_id VARCHAR(36) NOT NULL,
    auth_version BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    FOREIGN KEY(admin_id) REFERENCES admin_account(id),
    INDEX idx_admin_session_expiry(expires_at)
);
CREATE TABLE admin_login_throttle (
    fingerprint VARCHAR(64) PRIMARY KEY,
    attempts INT NOT NULL,
    window_start DATETIME(6) NOT NULL
);
CREATE TABLE admin_audit (
    operation_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    operator_id VARCHAR(36) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    state VARCHAR(24) NOT NULL,
    result_code VARCHAR(32),
    request_id VARCHAR(80) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_admin_audit_actor(operator_id,created_at),
    INDEX idx_admin_audit_state(state,updated_at)
);
CREATE TABLE admin_bootstrap_lock (id INT PRIMARY KEY);
INSERT INTO admin_bootstrap_lock(id) VALUES(1);
INSERT INTO admin_permission(code,name) VALUES
('USER_READ','查看账号摘要'),('USER_WRITE','限制或恢复账号'),
('QUOTA_READ','查看额度申请'),('QUOTA_WRITE','审批和赠送存储额度'),
('BREED_READ','查看品种字典'),('BREED_WRITE','维护品种字典'),
('AUDIT_READ','查看操作审计');
INSERT INTO admin_role(id,name) VALUES('SUPER_ADMIN','系统管理员'),('SUPPORT','客服只读'),
('QUOTA_ADMIN','额度管理员'),('CONFIG_ADMIN','字典管理员'),('AUDITOR','审计员');
INSERT INTO admin_role_permission(role_id,permission_code) SELECT 'SUPER_ADMIN',code FROM admin_permission;
INSERT INTO admin_role_permission(role_id,permission_code) VALUES
('SUPPORT','USER_READ'),('SUPPORT','BREED_READ'),('QUOTA_ADMIN','QUOTA_READ'),('QUOTA_ADMIN','QUOTA_WRITE'),
('CONFIG_ADMIN','BREED_READ'),('CONFIG_ADMIN','BREED_WRITE'),('AUDITOR','AUDIT_READ');
