package hu.boros.inventory;

import static org.springframework.http.HttpStatus.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
      String cover,
      String actionId,
      UUID destination,
      @JsonInclude(JsonInclude.Include.NON_NULL) Boolean executeMove) {}

  record Place(UUID id, String name) {}

  final EntityManager entities;
  final LocationRepository locations;
  final ItemRepository items;
  final ActionRepository actions;
  final PhotoRepository photoRecords;
  final FileCleanupRepository cleanup;
  final Photos photos;
  private final CalendarFacts facts;
  private final CalendarLocationChanges locationChanges;

  Ledger(
      EntityManager entities,
      LocationRepository locations,
      ItemRepository items,
      ActionRepository actions,
      PhotoRepository photoRecords,
      FileCleanupRepository cleanup,
      Photos photos,
      CalendarFacts facts,
      CalendarLocationChanges locationChanges) {
    this.entities = entities;
    this.locations = locations;
    this.items = items;
    this.actions = actions;
    this.photoRecords = photoRecords;
    this.cleanup = cleanup;
    this.photos = photos;
    this.facts = facts;
    this.locationChanges = locationChanges;
  }

  static void require(boolean ok, String message) {
    if (!ok) throw new ResponseStatusException(UNPROCESSABLE_ENTITY, message);
  }

  static void changed(int rows) {
    if (rows != 1)
      throw new ResponseStatusException(
          CONFLICT, "A tétel időközben megváltozott. Hasonlítsd össze az adatokat.");
  }

  @Transactional(readOnly = true)
  public List<InventoryViews.PlaceView> places() {
    return locations.findAllByOrderByNameAsc().stream()
        .map(location -> new InventoryViews.PlaceView(location.id, location.name))
        .toList();
  }

  @Transactional
  public void place(Place p) {
    require(
        p.name() != null && !p.name().isBlank() && p.name().strip().length() <= 180,
        "Adj meg 1–180 karakteres helyszínnevet.");
    if (p.id() == null) {
      entities.persist(new LocationEntity(UUID.randomUUID().toString(), p.name().strip()));
      locations.flush();
    } else changed(locations.rename(p.id().toString(), p.name().strip()));
  }

  @Transactional(readOnly = true)
  public List<InventoryViews.ItemView> items(boolean trash) {
    return items.listed(trash).stream()
        .map(item -> InventoryViews.item(item, item.location.getName(), null))
        .toList();
  }

  @Transactional(readOnly = true)
  public InventoryViews.ItemView item(UUID id) {
    var item =
        items
            .findById(id.toString())
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "A tárgy nem található."));
    var pictures =
        photoRecords.findByItemIdOrderByPositionAscIdAsc(id.toString()).stream()
            .map(photo -> new InventoryViews.PhotoView(photo.id, photo.filename))
            .toList();
    return InventoryViews.item(item, null, pictures);
  }

  private static void validatePrice(BigDecimal price) {
    if (price != null)
      require(
          price.signum() >= 0 && price.scale() <= 2 && price.precision() - price.scale() <= 12,
          "Érvénytelen ár.");
  }

  record Sale(long version, BigDecimal actual, LocalDate soldDate) {}

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void completeSale(UUID id, Sale sale) {
    require(
        sale.actual() != null && sale.soldDate() != null,
        "Add meg a tényleges eladási árat és az eladás dátumát.");
    validatePrice(sale.actual());
    String key = id.toString();
    var current = lockItem(key, sale.version());
    changed(items.completeSale(key, sale.version(), sale.actual(), sale.soldDate()));
    facts.recordSale(key, !current.status.equals("SOLD") || current.soldDate == null);
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
    validatePrice(d.asking());
    validatePrice(d.actual());
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

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void save(
      Draft d, List<MultipartFile> uploads, CalendarLocationChanges.Approval approval) {
    validate(d);
    var id = d.id().toString();
    String action = d.actionId();
    if (action == null) action = d.status().equals("KEEP") ? "KEEP" : "SALE";
    if (action.isBlank()) action = null;
    String kind = "";
    if (action != null) {
      var choice =
          actions
              .lockForAssignment(action)
              .orElseThrow(
                  () -> new ResponseStatusException(UNPROCESSABLE_ENTITY, "Ismeretlen művelet."));
      if (!choice.active)
        require(items.existsByIdAndActionId(id, action), "Ez a művelet már ki van kapcsolva.");
      kind = choice.kind;
    }
    require(
        kind.equals("SALE") == !d.status().equals("KEEP"),
        "Az eladási állapot és a művelet eltér.");
    boolean executeMove = Boolean.TRUE.equals(d.executeMove());
    require(!executeMove || kind.equals("MOVE"), "Áthelyezéshez válaszd a költöztetési műveletet.");
    if (kind.equals("MOVE"))
      require(
          d.destination() != null && !d.destination().equals(d.location()),
          "Válassz az aktuálistól eltérő célhelyszínt.");
    else require(d.destination() == null, "Csak költöztetéshez adható meg célhelyszín.");
    boolean newSale = true;
    if (d.version() == 0) {
      require(approval == null, "Új tárgyhoz nem szükséges a helyváltoztatás jóváhagyása.");
      entities.persist(new ItemEntity(d, action));
      items.flush();
    } else {
      var current = lockItem(id, d.version());
      locationChanges.apply(id, current.locationId, d.location(), approval);
      newSale = !current.status.equals("SOLD") || current.soldDate == null;
      changed(
          items.updateDraft(
              id,
              d.version(),
              d.name().strip(),
              d.description(),
              d.location().toString(),
              d.status(),
              d.asking(),
              d.link(),
              d.actual(),
              d.soldDate(),
              action,
              Objects.toString(d.destination(), null)));
    }
    var ids =
        new ArrayList<>(
            photoRecords.findByItemIdOrderByPositionAscIdAsc(id).stream()
                .map(photo -> photo.id)
                .toList());
    int previous = ids.size();
    for (var upload : uploads) {
      String photo = photos.store(upload);
      entities.persist(new PhotoEntity(photo, id, upload.getOriginalFilename(), ids.size()));
      photoRecords.flush();
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
    if (d.status().equals("SOLD") && d.soldDate() != null) facts.recordSale(id, newSale);
    if (executeMove) {
      changed(items.completeMove(id, d.version() + 1));
      facts.recordMove(id, d.location().toString(), d.destination().toString());
    }
    items.updateCover(id, cover);
  }

  private ItemEntity lockItem(String id, long version) {
    var current = items.lockCurrent(id).orElse(null);
    changed(current != null && current.version == version && !current.trashed ? 1 : 0);
    return current;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void transition(UUID id, long version, String action) {
    String key = id.toString();
    if (action.equals("move")) {
      var current = lockItem(key, version);
      changed(items.completeMove(key, version));
      facts.recordMove(key, current.locationId, current.destinationId);
      return;
    }
    if (action.equals("purge")) {
      changed(items.lockForPurge(key, version).isPresent() ? 1 : 0);
      cleanup.enqueueForItem(key);
      items.purge(key);
    } else {
      require(Set.of("trash", "restore").contains(action), "Ismeretlen művelet.");
      boolean trash = action.equals("trash");
      changed(items.setTrashed(key, version, trash));
    }
  }
}
