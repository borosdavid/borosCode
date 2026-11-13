CREATE TABLE location (
 id CHAR(36) PRIMARY KEY, name VARCHAR(180) NOT NULL UNIQUE
);
CREATE TABLE item (
 id CHAR(36) PRIMARY KEY, version BIGINT NOT NULL, name VARCHAR(200) NOT NULL,
 description TEXT NOT NULL, location_id CHAR(36) NOT NULL, status VARCHAR(12) NOT NULL,
 asking DECIMAL(14,2), link VARCHAR(2000), actual DECIMAL(14,2), sold_date DATE,
 cover CHAR(36), trashed BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY(location_id) REFERENCES location(id), INDEX(trashed,status,location_id)
);
CREATE TABLE photo (
 id CHAR(36) PRIMARY KEY, item_id CHAR(36) NOT NULL, filename VARCHAR(255) NOT NULL,
 position_no INT NOT NULL, FOREIGN KEY(item_id) REFERENCES item(id) ON DELETE CASCADE
);
CREATE TABLE file_cleanup (id CHAR(36) PRIMARY KEY);

