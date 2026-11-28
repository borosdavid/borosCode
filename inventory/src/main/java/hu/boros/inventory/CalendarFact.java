package hu.boros.inventory;

import jakarta.persistence.*;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "calendar_inventory_fact")
class CalendarFact {
  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String id;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 16)
  CalendarEvent.Type factType;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(nullable = false, length = 36)
  String itemId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String eventId;

  LocalDate movedOn;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String originLocationId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String destinationLocationId;

  protected CalendarFact() {}

  CalendarFact(String id, CalendarEvent.Type factType, String itemId) {
    this.id = id;
    this.factType = factType;
    this.itemId = itemId;
  }
}
