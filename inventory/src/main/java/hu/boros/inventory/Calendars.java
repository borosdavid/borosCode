package hu.boros.inventory;

import static hu.boros.inventory.CalendarEvent.Status.*;
import static hu.boros.inventory.CalendarEvent.Type.*;
import static org.springframework.http.HttpStatus.*;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class Calendars {
  private static final ZoneId CALENDAR_ZONE = ZoneId.of("Europe/Budapest");
  private static final int MAX_SEARCH_DAYS = 366;
  private static final int MAX_ITEMS = 100;

  record ItemLink(UUID itemId, UUID originLocationId) {}

  record Draft(
      UUID id,
      Long version,
      String title,
      CalendarEvent.Type type,
      CalendarEvent.Status status,
      String notes,
      UUID locationId,
      LocalDate plannedStart,
      LocalDate plannedEnd,
      LocalDate actualStart,
      LocalDate actualEnd,
      List<ItemLink> items) {}

  record Warning(String code, List<UUID> eventIds) {}

  record View(
      UUID id,
      long version,
      String title,
      CalendarEvent.Type type,
      CalendarEvent.Status status,
      String notes,
      UUID locationId,
      LocalDate plannedStart,
      LocalDate plannedEnd,
      LocalDate actualStart,
      LocalDate actualEnd,
      boolean overdue,
      List<ItemLink> items,
      List<Warning> warnings) {}

  private final CalendarEventRepository events;
  private final ItemRepository inventory;
  private final LocationRepository locations;
  private final EntityManager entities;

  Calendars(
      CalendarEventRepository events,
      ItemRepository inventory,
      LocationRepository locations,
      EntityManager entities) {
    this.events = events;
    this.inventory = inventory;
    this.locations = locations;
    this.entities = entities;
  }

  @Transactional(readOnly = true)
  public List<View> list(
      LocalDate from,
      LocalDate until,
      CalendarEvent.Type type,
      CalendarEvent.Status status,
      UUID locationId,
      UUID itemId) {
    validateDate(from);
    validateDate(until);
    long days = ChronoUnit.DAYS.between(from, until);
    require(days > 0 && days <= MAX_SEARCH_DAYS, "A lekérdezés 1–366 napra adható meg.");
    LocalDate today = LocalDate.now(CALENDAR_ZONE);
    return events.inPeriod(from, until, type, status, key(locationId), key(itemId)).stream()
        .map(event -> view(event, today, List.of()))
        .toList();
  }

  @Transactional(readOnly = true)
  public View get(UUID id) {
    return detail(find(id));
  }

  @Transactional
  public View create(Draft draft) {
    require(draft.id() != null && draft.version() == null, "Adj meg új eseményazonosítót.");
    var event = new CalendarEvent(draft.id().toString());
    apply(event, draft);
    entities.persist(event);
    events.flush();
    return detail(event);
  }

  @Transactional
  public View update(UUID id, Draft draft) {
    require(draft.id() == null || id.equals(draft.id()), "Az eseményazonosító nem egyezik.");
    require(draft.version() != null && draft.version() >= 0, "Az esemény verziója kötelező.");
    CalendarEvent event = find(id);
    if (event.version != draft.version())
      throw new ResponseStatusException(CONFLICT, "A naptáresemény időközben megváltozott.");
    apply(event, draft);
    events.flush();
    return detail(event);
  }

  private CalendarEvent find(UUID id) {
    return events
        .findById(id.toString())
        .orElseThrow(
            () -> new ResponseStatusException(NOT_FOUND, "A naptáresemény nem található."));
  }

  private void apply(CalendarEvent event, Draft draft) {
    String title = Objects.toString(draft.title(), "").strip();
    require(!title.isEmpty() && title.length() <= 200, "A cím 1–200 karakter lehet.");
    require(draft.type() != null && draft.status() != null, "A típus és az állapot kötelező.");
    require(draft.notes() == null || draft.notes().length() <= 2000, "A megjegyzés túl hosszú.");

    LocalDate plannedStart = draft.plannedStart();
    LocalDate plannedEnd = endOfRange(plannedStart, draft.plannedEnd());
    LocalDate actualStart = draft.actualStart();
    LocalDate actualEnd = endOfRange(actualStart, draft.actualEnd());
    if (draft.status() == DONE) {
      require(actualStart != null, "A tényleges kezdőnap kötelező.");
      if (plannedStart != null && plannedEnd.isAfter(plannedStart))
        require(draft.actualEnd() != null, "Többnapos eseménynél a tényleges zárónap kötelező.");
    } else {
      require(plannedStart != null, "A tervezett kezdőnap kötelező.");
      require(actualStart == null, "Tényleges dátum csak megtörtént eseményhez adható meg.");
    }

    var links = itemLinks(draft);
    event.title = title;
    event.type = draft.type();
    event.status = draft.status();
    event.notes = draft.notes();
    event.locationId = key(draft.locationId());
    event.plannedStart = plannedStart;
    event.plannedEnd = plannedEnd;
    event.actualStart = actualStart;
    event.actualEnd = actualEnd;
    event.items.keySet().retainAll(links.keySet());
    links.forEach(event.items::put);
  }

  private Map<String, CalendarEvent.EventItem> itemLinks(Draft draft) {
    var links = new LinkedHashMap<String, CalendarEvent.EventItem>();
    var locationIds = new HashSet<String>();
    if (draft.locationId() != null) locationIds.add(key(draft.locationId()));
    if (draft.items() != null) {
      require(
          draft.items().size() <= MAX_ITEMS, "Egy eseményhez legfeljebb 100 tárgy kapcsolható.");
      boolean reservesSale = draft.type() == SALE && draft.status() == PLANNED;
      for (ItemLink item : draft.items()) {
        require(item != null && item.itemId() != null, "A tárgy azonosítója kötelező.");
        String origin = key(item.originLocationId());
        if (origin != null) locationIds.add(origin);
        var previous =
            links.put(key(item.itemId()), new CalendarEvent.EventItem(origin, reservesSale));
        require(previous == null, "Egy tárgy csak egyszer kapcsolható ugyanahhoz az eseményhez.");
      }
    }
    require(inventory.findAllById(links.keySet()).size() == links.size(), "Ismeretlen tárgy.");
    require(
        locations.findAllById(locationIds).size() == locationIds.size(), "Ismeretlen helyszín.");
    return links;
  }

  private View detail(CalendarEvent event) {
    var warnings = List.<Warning>of();
    if (event.status == PLANNED
        && (event.type == SALE || event.type == MOVE)
        && !event.items.isEmpty()) {
      var conflicts =
          events.overlapping(
              event.type == SALE ? MOVE : SALE,
              PLANNED,
              event.plannedStart,
              event.plannedEnd,
              event.items.keySet());
      if (!conflicts.isEmpty())
        warnings =
            List.of(
                new Warning(
                    "SALE_MOVE_OVERLAP", conflicts.stream().map(UUID::fromString).toList()));
    }
    return view(event, LocalDate.now(CALENDAR_ZONE), warnings);
  }

  private static View view(CalendarEvent event, LocalDate today, List<Warning> warnings) {
    var links =
        event.items.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(
                link -> new ItemLink(uuid(link.getKey()), uuid(link.getValue().originLocationId())))
            .toList();
    return new View(
        uuid(event.id),
        event.version,
        event.title,
        event.type,
        event.status,
        event.notes,
        uuid(event.locationId),
        event.plannedStart,
        event.plannedEnd,
        event.actualStart,
        event.actualEnd,
        event.status == PLANNED && event.plannedEnd.isBefore(today),
        links,
        warnings);
  }

  private static LocalDate endOfRange(LocalDate start, LocalDate end) {
    if (start == null) {
      require(end == null, "Zárónaphoz add meg a kezdőnapot is.");
      return null;
    }
    validateDate(start);
    if (end == null) return start;
    validateDate(end);
    require(!end.isBefore(start), "A zárónap nem előzheti meg a kezdőnapot.");
    return end;
  }

  private static void validateDate(LocalDate date) {
    require(
        date.getYear() >= 1000 && date.getYear() <= 9999,
        "A dátum kívül esik az érvényes tartományon.");
  }

  private static String key(UUID id) {
    return Objects.toString(id, null);
  }

  private static UUID uuid(String id) {
    return id == null ? null : UUID.fromString(id);
  }

  private static void require(boolean valid, String message) {
    if (!valid) throw new ResponseStatusException(UNPROCESSABLE_ENTITY, message);
  }
}
