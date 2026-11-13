CREATE TABLE item_action (
 id VARCHAR(36) PRIMARY KEY,
 name VARCHAR(100) NOT NULL UNIQUE,
 kind VARCHAR(12) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE
);
INSERT INTO item_action (id,name,kind) VALUES
 ('KEEP','Megtartás','KEEP'), ('SALE','Eladás','SALE'), ('MOVE','Költöztetés','MOVE');
ALTER TABLE item
 ADD action_id VARCHAR(36) NULL,
 ADD destination_id CHAR(36) NULL,
 ADD FOREIGN KEY(action_id) REFERENCES item_action(id),
 ADD FOREIGN KEY(destination_id) REFERENCES location(id);
UPDATE item SET action_id = IF(status = 'KEEP','KEEP','SALE');

