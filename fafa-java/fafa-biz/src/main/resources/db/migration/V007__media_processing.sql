
CREATE TABLE media_processing_job (
 id VARCHAR(36) PRIMARY KEY,
 owner_user_id VARCHAR(36) NOT NULL,
 state VARCHAR(16) NOT NULL,
 attempts INT NOT NULL DEFAULT 0,
 lease_token VARCHAR(36),
 lease_until DATETIME(6),
 retain_until DATETIME(6) NOT NULL,
 error_code VARCHAR(40),
 CONSTRAINT fk_job_upload FOREIGN KEY(id) REFERENCES upload_session(id)
);
CREATE INDEX ix_job_lease ON media_processing_job(state,lease_until,id);
CREATE TABLE outbox_event (
 id VARCHAR(36) PRIMARY KEY,
 aggregate_id VARCHAR(36) NOT NULL,
 event_type VARCHAR(64) NOT NULL,
 state VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 available_at DATETIME(6) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 sent_at DATETIME(6)
);
CREATE INDEX ix_outbox_pending ON outbox_event(state,available_at,id);
CREATE TABLE consumer_inbox (
 consumer_name VARCHAR(64) NOT NULL,
 event_id VARCHAR(36) NOT NULL,
 processed_at DATETIME(6) NOT NULL,
 PRIMARY KEY(consumer_name,event_id)
);
