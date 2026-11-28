CREATE TABLE pending_photo_upload (
 account_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 upload_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 photo_id CHAR(36),
 filename VARCHAR(255),
 byte_size BIGINT,
 expires_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 PRIMARY KEY (account_id, upload_id),
 INDEX ix_pending_photo_expiry (expires_at)
);
