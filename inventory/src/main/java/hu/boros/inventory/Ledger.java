package hu.boros.inventory;

import static org.springframework.http.HttpStatus.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Ledger {
  record Draft(
      UUID id,
      long version,
      String name,
      String description,
      UUID location,
      String status,
      BigDecimal asking,
      String link,
      BigDecimal actual,
      LocalDate soldDate,
      String cover) {}

  record Place(UUID id, String name) {}

  final JdbcTemplate db;
  final Photos photos;

  Ledger(JdbcTemplate db, Photos photos) {
    this.db = db;
    this.photos = photos;
  }

  static void require(boolean ok, String message) {
    if (!ok) throw new ResponseStatusException(UNPROCESSABLE_ENTITY, message);
  }

  static void changed(int rows) {
    if (rows != 1)
      throw new ResponseStatusException(
          CONFLICT, "A tétel időközben megváltozott. Hasonlítsd össze az adatokat.");
  }

  List<Map<String, Object>> places() {
    return db.queryForList("SELECT id,name FROM location ORDER BY name");
  }

  @Transactional
  public void place(Place p) {
    require(
        p.name() != null && !p.name().isBlank() && p.name().strip().length() <= 180,
        "Adj meg 1–180 karakteres helyszínnevet.");
    if (p.id() == null)
      db.update("INSERT INTO location VALUES(?,?)", UUID.randomUUID().toString(), p.name().strip());
    else
      changed(
          db.update("UPDATE location SET name=? WHERE id=?", p.name().strip(), p.id().toString()));
  }

  List<Map<String, Object>> items(boolean trash) {
    return db.queryForList(
        "SELECT i.*,l.name AS place FROM item i JOIN location l ON i.location_id=l.id WHERE trashed=? ORDER BY (status='SOLD'), i.name, i.id",
        trash);
  }

  Map<String, Object> item(UUID id) {
    var rows = db.queryForList("SELECT * FROM item WHERE id=?", id.toString());
    if (rows.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "A tárgy nem található.");
    var row = rows.getFirst();
    if (row.get("sold_date") instanceof java.sql.Date date)
      row.put("sold_date", date.toLocalDate().toString());
    row.put(
        "photos",
        db.queryForList(
            "SELECT id,filename FROM photo WHERE item_id=? ORDER BY position_no", id.toString()));
    return row;
  }

  static void validate(Draft d) {
    require(d.id() != null && d.location() != null, "Válassz helyszínt.");
    require(
        d.name() != null && !d.name().isBlank() && d.name().length() <= 200,
        "A tárgy neve 1–200 karakter lehet.");
    require(
        d.description() != null && d.description().length() <= 12000,
        "A leírás legfeljebb 12 000 karakter lehet.");
    require(
        d.status() != null && Set.of("KEEP", "WAIT", "LISTED", "SOLD").contains(d.status()),
        "Ismeretlen állapot.");
    for (var n : new BigDecimal[] {d.asking(), d.actual()})
      if (n != null)
        require(
            n.signum() >= 0 && n.scale() <= 2 && n.precision() - n.scale() <= 12,
            "Érvénytelen ár.");
    if (d.link() != null && !d.link().isBlank()) {
      URI uri;
      try {
        uri = URI.create(d.link());
      } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Érvénytelen link.");
      }
      require(
          d.link().length() <= 2000
              && uri.getHost() != null
              && Set.of("https", "http").contains(uri.getScheme()),
          "Érvénytelen hirdetési link.");
    }
  }

  @Transactional
  public void save(Draft d, List<MultipartFile> uploads) {
    validate(d);
    var id = d.id().toString();
    var args =
        new Object[] {
          d.name().strip(),
          d.description(),
          d.location().toString(),
          d.status(),
          d.asking(),
          d.link(),
          d.actual(),
          d.soldDate(),
          id
        };
    if (d.version() == 0) {
      db.update(
          "INSERT INTO item(name,description,location_id,status,asking,link,actual,sold_date,id,version) VALUES(?,?,?,?,?,?,?,?,?,1)",
          args);
    } else {
      var params = new ArrayList<>(Arrays.asList(args));
      params.add(d.version());
      changed(
          db.update(
              "UPDATE item SET name=?,description=?,location_id=?,status=?,asking=?,link=?,actual=?,sold_date=?,version=version+1 WHERE id=? AND version=? AND trashed=0",
              params.toArray()));
    }
    var ids =
        db.queryForList(
            "SELECT id FROM photo WHERE item_id=? ORDER BY position_no", String.class, id);
    int previous = ids.size();
    for (var upload : uploads) {
      String photo = photos.store(upload);
      db.update(
          "INSERT INTO photo VALUES(?,?,?,?)", photo, id, upload.getOriginalFilename(), ids.size());
      ids.add(photo);
    }
    String cover = d.cover();
    if (cover != null && cover.startsWith("new:")) {
      int index = Integer.parseInt(cover.substring(4));
      require(index >= 0 && index < uploads.size(), "Ismeretlen új borítókép.");
      cover = ids.get(previous + index);
    }
    if (cover == null || cover.isBlank()) cover = ids.isEmpty() ? null : ids.getFirst();
    require(cover == null || ids.contains(cover), "A borítókép nem ehhez a tárgyhoz tartozik.");
    db.update("UPDATE item SET cover=? WHERE id=?", cover, id);
  }

  @Transactional
  public void transition(UUID id, long version, String action) {
    String key = id.toString();
    if (action.equals("purge")) {
      var locked =
          db.queryForList(
              "SELECT id FROM item WHERE id=? AND version=? AND trashed=1 FOR UPDATE",
              key,
              version);
      changed(locked.size());
      db.update("INSERT INTO file_cleanup SELECT id FROM photo WHERE item_id=?", key);
      db.update("DELETE FROM item WHERE id=?", key);
    } else {
      require(Set.of("trash", "restore").contains(action), "Ismeretlen művelet.");
      boolean trash = action.equals("trash");
      changed(
          db.update(
              "UPDATE item SET trashed=?,version=version+1 WHERE id=? AND version=? AND trashed=?",
              trash,
              key,
              version,
              !trash));
    }
  }
}
