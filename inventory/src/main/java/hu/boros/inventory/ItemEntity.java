package hu.boros.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "item")
public class ItemEntity {
  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String id;

  @Column(nullable = false)
  long version;

  @Column(nullable = false, length = 200)
  String name;

  @Column(nullable = false, columnDefinition = "text")
  String description;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "location_id", nullable = false, length = 36)
  String locationId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "location_id", insertable = false, updatable = false)
  LocationEntity location;

  @Column(nullable = false, length = 12)
  String status;

  @Column(precision = 14, scale = 2)
  BigDecimal asking;

  @Column(length = 2000)
  String link;

  @Column(precision = 14, scale = 2)
  BigDecimal actual;

  @Column(name = "sold_date")
  LocalDate soldDate;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String cover;

  @Column(nullable = false, columnDefinition = "tinyint")
  boolean trashed;

  @Column(name = "action_id", length = 36)
  String actionId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "action_id", insertable = false, updatable = false)
  ActionEntity action;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "destination_id", length = 36)
  String destinationId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "destination_id", insertable = false, updatable = false)
  LocationEntity destination;

  protected ItemEntity() {}

  ItemEntity(Ledger.Draft draft, String actionId) {
    this.id = draft.id().toString();
    this.version = 1;
    this.name = draft.name().strip();
    this.description = draft.description();
    this.locationId = draft.location().toString();
    this.status = draft.status();
    this.asking = draft.asking();
    this.link = draft.link();
    this.actual = draft.actual();
    this.soldDate = draft.soldDate();
    this.actionId = actionId;
    this.destinationId = Objects.toString(draft.destination(), null);
  }
}
