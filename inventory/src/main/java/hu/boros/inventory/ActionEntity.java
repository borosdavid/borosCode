package hu.boros.inventory;

import jakarta.persistence.*;

@Entity
@Table(name = "item_action")
public class ActionEntity {
  @Id
  @Column(length = 36)
  String id;

  @Column(nullable = false, length = 100, unique = true)
  String name;

  @Column(nullable = false, length = 12)
  String kind;

  @Column(nullable = false, columnDefinition = "tinyint")
  boolean active;

  protected ActionEntity() {}

  ActionEntity(String id, String name, String kind) {
    this.id = id;
    this.name = name;
    this.kind = kind;
    this.active = true;
  }
}
