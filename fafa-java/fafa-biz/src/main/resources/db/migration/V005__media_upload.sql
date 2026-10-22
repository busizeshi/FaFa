
CREATE TABLE upload_session (
 id VARCHAR(36) PRIMARY KEY,
 owner_user_id VARCHAR(36) NOT NULL,
 pet_id VARCHAR(36) NOT NULL,
 mime VARCHAR(32) NOT NULL,
 bytes BIGINT NOT NULL,
 sha256 VARCHAR(64) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 state VARCHAR(16) NOT NULL,
 asset_id VARCHAR(36),
 cleaned BOOLEAN NOT NULL DEFAULT FALSE,
 CONSTRAINT ck_upload_bytes CHECK (bytes > 0),
 CONSTRAINT fk_upload_owner FOREIGN KEY (owner_user_id) REFERENCES user_account(id),
 CONSTRAINT fk_upload_pet FOREIGN KEY (pet_id) REFERENCES pet(id)
);
CREATE INDEX ix_upload_expiry ON upload_session(cleaned,expires_at,id);
CREATE TABLE media_asset (
 id VARCHAR(36) PRIMARY KEY,
 owner_user_id VARCHAR(36) NOT NULL,
 pet_id VARCHAR(36) NOT NULL,
 bytes BIGINT NOT NULL,
 mime VARCHAR(32) NOT NULL,
 sha256 VARCHAR(64) NOT NULL,
 width INT NOT NULL,
 height INT NOT NULL,
 CONSTRAINT fk_media_session FOREIGN KEY(id) REFERENCES upload_session(id)
);
CREATE INDEX ix_media_owner_pet ON media_asset(owner_user_id,pet_id,id);

CREATE TABLE media_binding (
 pet_id VARCHAR(36) PRIMARY KEY,
 media_id VARCHAR(36) NOT NULL,
 CONSTRAINT fk_avatar_pet FOREIGN KEY(pet_id) REFERENCES pet(id),
 CONSTRAINT fk_avatar_media FOREIGN KEY(media_id) REFERENCES media_asset(id)
);
