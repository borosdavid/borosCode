package hu.boros.inventory;

import jakarta.persistence.*;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "app_account")
public class AccountEntity {
  @Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(length = 36)
  String id;

  @Column(nullable = false, unique = true, length = 64)
  String username;

  @Column(name = "password_hash", nullable = false, length = 255)
  String passwordHash;

  @Column(nullable = false)
  boolean enabled = true;

  @Version long version;

  protected AccountEntity() {}

  AccountEntity(String username, String passwordHash) {
    this.id = UUID.randomUUID().toString();
    this.username = username;
    this.passwordHash = passwordHash;
  }
}
