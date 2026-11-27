package hu.boros.inventory;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "location")
public class LocationEntity {
  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String id;

  @Column(nullable = false, length = 180, unique = true)
  String name;

  protected LocationEntity() {}

  LocationEntity(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
