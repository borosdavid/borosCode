package hu.boros.inventory;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class Actions {
  record Choice(String id, String name, String kind, boolean active) {}

  private final JdbcTemplate db;

  Actions(JdbcTemplate db) {
    this.db = db;
  }

  List<Choice> all() {
    return db.query(
        "SELECT * FROM item_action ORDER BY FIELD(kind,'KEEP','SALE','MOVE','CUSTOM'), name",
        (r, i) ->
            new Choice(
                r.getString("id"),
                r.getString("name"),
                r.getString("kind"),
                r.getBoolean("active")));
  }

  @Transactional
  public void save(Choice choice) {
    Ledger.require(
        choice.name() != null && !choice.name().isBlank() && choice.name().strip().length() <= 100,
        "Adj meg 1–100 karakteres nevet.");
    String name = choice.name().strip();
    if (choice.id() == null) {
      Ledger.require(
          "MOVE".equals(choice.kind()) || "CUSTOM".equals(choice.kind()), "Válassz típust.");
      db.update(
          "INSERT INTO item_action(id,name,kind) VALUES(?,?,?)",
          UUID.randomUUID().toString(),
          name,
          choice.kind());
    } else {
      Ledger.changed(
          db.update(
              "UPDATE item_action SET name=?, active=? WHERE id=?",
              name,
              choice.active(),
              choice.id()));
    }
  }
}
