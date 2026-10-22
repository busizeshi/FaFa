
ALTER TABLE upload_session ADD COLUMN upload_mode VARCHAR(16) NOT NULL DEFAULT 'SINGLE';
ALTER TABLE upload_session ADD COLUMN multipart_id VARCHAR(512);
ALTER TABLE upload_session ADD COLUMN reserved_bytes BIGINT NOT NULL DEFAULT 0;
UPDATE upload_session SET reserved_bytes=bytes;
ALTER TABLE media_asset ADD COLUMN media_type VARCHAR(16) NOT NULL DEFAULT 'IMAGE';
ALTER TABLE media_asset ADD COLUMN state VARCHAR(24) NOT NULL DEFAULT 'READY';
ALTER TABLE media_asset ADD COLUMN version BIGINT NOT NULL DEFAULT 1;
ALTER TABLE media_asset ADD COLUMN title VARCHAR(200) NOT NULL DEFAULT '';
ALTER TABLE media_asset ADD COLUMN description VARCHAR(2000) NOT NULL DEFAULT '';
ALTER TABLE media_asset ADD COLUMN captured_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
ALTER TABLE media_asset ADD COLUMN captured_at_source VARCHAR(24) NOT NULL DEFAULT 'LEGACY_IMPORT';
ALTER TABLE media_asset ADD COLUMN duration_millis BIGINT;
ALTER TABLE media_asset ADD COLUMN page_count INT;
ALTER TABLE media_asset ADD COLUMN charged_bytes BIGINT NOT NULL DEFAULT 0;
UPDATE media_asset SET charged_bytes=bytes;
CREATE INDEX ix_media_filter ON media_asset(owner_user_id,state,media_type,captured_at,id);
CREATE TABLE media_tag (
 media_id VARCHAR(36) NOT NULL,
 tag VARCHAR(30) NOT NULL,
 PRIMARY KEY(media_id,tag),
 CONSTRAINT fk_tag_media FOREIGN KEY(media_id) REFERENCES media_asset(id)
);
CREATE TABLE album (
 id VARCHAR(36) PRIMARY KEY,
 owner_user_id VARCHAR(36) NOT NULL,
 name VARCHAR(80) NOT NULL,
 description VARCHAR(1000) NOT NULL DEFAULT '',
 version BIGINT NOT NULL DEFAULT 1,
 CONSTRAINT fk_album_owner FOREIGN KEY(owner_user_id) REFERENCES user_account(id),
 INDEX ix_album_owner(owner_user_id,id)
);
CREATE TABLE album_item (
 album_id VARCHAR(36) NOT NULL,
 media_id VARCHAR(36) NOT NULL,
 PRIMARY KEY(album_id,media_id),
 CONSTRAINT fk_item_album FOREIGN KEY(album_id) REFERENCES album(id),
 CONSTRAINT fk_item_media FOREIGN KEY(media_id) REFERENCES media_asset(id)
);

CREATE TABLE media_reference (
 media_id VARCHAR(36) NOT NULL,
 business_type VARCHAR(16) NOT NULL,
 business_id VARCHAR(36) NOT NULL,
 PRIMARY KEY(media_id,business_type,business_id),
 CONSTRAINT fk_reference_media FOREIGN KEY(media_id) REFERENCES media_asset(id),
 INDEX ix_reference_business(business_type,business_id,media_id)
);
