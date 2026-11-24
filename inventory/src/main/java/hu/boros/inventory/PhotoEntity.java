package hu.boros.inventory;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "photo")
public class PhotoEntity {
  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "item_id", nullable = false, length = 36)
  String itemId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "item_id", insertable = false, updatable = false)
  ItemEntity item;

  @Column(nullable = false, length = 255)
  String filename;

  @Column(name = "position_no", nullable = false)
  int position;

  protected PhotoEntity() {}

  PhotoEntity(String id, String itemId, String filename, int position) {
    this.id = id;
    this.itemId = itemId;
    this.filename = filename;
    this.position = position;
  }
}
