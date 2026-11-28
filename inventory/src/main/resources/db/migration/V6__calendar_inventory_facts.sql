ALTER TABLE item ADD INDEX ix_item_sale_calendar (status, sold_date);

CREATE TABLE calendar_inventory_fact (
 id CHAR(36) PRIMARY KEY,
 fact_type VARCHAR(16) NOT NULL,
 item_id CHAR(36) NOT NULL,
 event_id CHAR(36),
 moved_on DATE,
 origin_location_id CHAR(36),
 destination_location_id CHAR(36),
 FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE,
 FOREIGN KEY (event_id) REFERENCES calendar_event(id) ON DELETE SET NULL,
 FOREIGN KEY (origin_location_id) REFERENCES location(id) ON DELETE SET NULL,
 FOREIGN KEY (destination_location_id) REFERENCES location(id) ON DELETE SET NULL,
 INDEX (fact_type, moved_on),
 INDEX (item_id, fact_type),
 sale_item_id CHAR(36) GENERATED ALWAYS AS
   (CASE WHEN fact_type = 'SALE' THEN item_id ELSE NULL END) VIRTUAL,
 UNIQUE KEY uq_calendar_sale_fact (sale_item_id),
 CONSTRAINT ck_calendar_inventory_fact CHECK (
   (fact_type = 'SALE' AND moved_on IS NULL)
   OR (fact_type = 'MOVE' AND moved_on IS NOT NULL)
 )
);
