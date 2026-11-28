package hu.boros.inventory;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Subselect;
import org.hibernate.annotations.Synchronize;
import org.hibernate.type.SqlTypes;

@Entity
@Immutable
@Synchronize({"item", "calendar_inventory_fact"})
// The sale UUID is calculated by the same UUID v3 algorithm as CalendarFacts.saleId.
@Subselect(
    """
    SELECT CONCAT(SUBSTRING(i.sale_hash, 1, 8), '-', SUBSTRING(i.sale_hash, 9, 4),
       '-3', SUBSTRING(i.sale_hash, 14, 3), '-',
       SUBSTRING('89ab', (CONV(SUBSTRING(i.sale_hash, 17, 1), 16, 10) & 3) + 1, 1),
       SUBSTRING(i.sale_hash, 18, 3), '-', SUBSTRING(i.sale_hash, 21, 12)) AS id,
        'SALE' AS fact_type, i.id AS item_id, i.name AS item_name, f.event_id,
       CASE WHEN i.status = 'SOLD' THEN i.sold_date END AS actual_on,
       i.location_id AS origin_location_id, NULL AS destination_location_id,
       (i.status <> 'SOLD' OR i.sold_date IS NULL) AS withdrawn, i.version AS source_version
     FROM (SELECT id, name, status, sold_date, location_id, version, MD5(CONCAT('otthontar:calendar:sale:', id)) AS sale_hash FROM item) i
     LEFT JOIN calendar_inventory_fact f ON f.item_id = i.id AND f.fact_type = 'SALE'
     WHERE (i.status = 'SOLD' AND i.sold_date IS NOT NULL) OR f.event_id IS NOT NULL
     UNION ALL
     SELECT f.id, f.fact_type, f.item_id, i.name, f.event_id, f.moved_on,
       f.origin_location_id, f.destination_location_id, FALSE, 0
     FROM calendar_inventory_fact f JOIN item i ON i.id = f.item_id
     WHERE f.fact_type = 'MOVE'
    """)
class CalendarActual {
  @Id
  @Column(length = 36)
  String id;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  CalendarEvent.Type factType;

  String itemId;
  String itemName;
  String eventId;
  LocalDate actualOn;
  String originLocationId;
  String destinationLocationId;
  long withdrawn;
  long sourceVersion;

  protected CalendarActual() {}
}
