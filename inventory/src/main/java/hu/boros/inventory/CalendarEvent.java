package hu.boros.inventory;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "calendar_event")
public class CalendarEvent {
  enum Type {
    MOVE,
    SALE,
    TRANSPORT,
    HANDOVER,
    PICKUP,
    OTHER
  }

  enum Status {
    PLANNED,
    DONE,
    CANCELLED
  }

  @Embeddable
  record EventItem(
      @JdbcTypeCode(SqlTypes.CHAR) @Column(length = 36) String originLocationId,
      @Column(nullable = false, columnDefinition = "tinyint") boolean reservesSale) {}

  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String id;

  @Version long version;

  @Column(nullable = false, length = 200)
  String title;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "event_type", nullable = false, length = 16)
  Type type;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 16)
  Status status;

  @Column(length = 2000)
  String notes;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String locationId;

  LocalDate plannedStart;
  LocalDate plannedEnd;
  LocalDate actualStart;
  LocalDate actualEnd;

  @ElementCollection
  @CollectionTable(name = "calendar_event_item", joinColumns = @JoinColumn(name = "event_id"))
  @MapKeyColumn(name = "item_id", length = 36, columnDefinition = "char(36)")
  @BatchSize(size = 64)
  Map<String, EventItem> items = new HashMap<>();

  protected CalendarEvent() {}

  CalendarEvent(String id) {
    this.id = id;
  }
}
