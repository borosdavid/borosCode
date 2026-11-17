package hu.boros.inventory;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "file_cleanup")
public class FileCleanupEntity {
  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String id;

  protected FileCleanupEntity() {}
}
