CREATE TABLE calendar_event (
 id CHAR(36) PRIMARY KEY,
 version BIGINT NOT NULL,
 title VARCHAR(200) NOT NULL,
 event_type VARCHAR(16) NOT NULL,
 status VARCHAR(16) NOT NULL,
 notes VARCHAR(2000),
 location_id CHAR(36),
 planned_start DATE,
 planned_end DATE,
 actual_start DATE,
 actual_end DATE,
 FOREIGN KEY (location_id) REFERENCES location(id) ON DELETE SET NULL,
 INDEX (status, event_type, planned_start, planned_end)
);
CREATE TABLE calendar_event_item (
 event_id CHAR(36) NOT NULL,
 item_id CHAR(36) NOT NULL,
 origin_location_id CHAR(36),
 reserves_sale BOOLEAN NOT NULL,
 open_sale_item_id CHAR(36) GENERATED ALWAYS AS
   (CASE WHEN reserves_sale THEN item_id ELSE NULL END) VIRTUAL,
 PRIMARY KEY (event_id, item_id),
 UNIQUE KEY uq_calendar_open_sale (open_sale_item_id),
 FOREIGN KEY (event_id) REFERENCES calendar_event(id) ON DELETE CASCADE,
 FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE,
 FOREIGN KEY (origin_location_id) REFERENCES location(id) ON DELETE SET NULL
);
