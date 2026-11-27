CREATE TABLE item_save_request (
 account_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 operation_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 item_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NULL,
 PRIMARY KEY (account_id, operation_id)
) ENGINE=InnoDB;
