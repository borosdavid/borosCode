package hu.boros.inventory;

import static hu.boros.inventory.CalendarEvent.Status.*;
import static hu.boros.inventory.CalendarEvent.Type.*;
import static org.springframework.http.HttpStatus.*;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class Calendars {
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

  record EventReference(UUID id, String title, long version, LocalDate date) {}

  static EventReference reference(CalendarEvent event) {
    return new EventReference(uuid(event.id), event.title, event.version, start(event));
  }

  enum Source {
    CALENDAR,
    INVENTORY
  }

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
      List<Warning> warnings,
      Source source,
      List<CalendarFacts.Fact> facts) {}

  private final CalendarEventRepository events;
  private final ItemRepository inventory;
  private final LocationRepository locations;
  private final EntityManager entities;
  private final CalendarFacts facts;

  Calendars(
      CalendarEventRepository events,
      ItemRepository inventory,
      LocationRepository locations,
      EntityManager entities,
      CalendarFacts facts) {
    this.events = events;
    this.inventory = inventory;
    this.locations = locations;
    this.entities = entities;
    this.facts = facts;
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
    var inPeriod = events.inPeriod(from, until, type, status, key(locationId), key(itemId));
    var attached = facts.attached(inPeriod.stream().map(event -> event.id).toList());
    var result = new ArrayList<View>();
    var represented = new HashSet<UUID>();
    LocalDate today = facts.today();
    for (var event : inPeriod) {
      var linked = attached.getOrDefault(event.id, List.of());
      result.add(view(event, today, List.of(), linked));
      if (event.status == CANCELLED) continue;
      for (var fact : linked) {
        if (!fact.withdrawn()
            && !fact.date().isBefore(start(event))
            && !fact.date().isAfter(end(event))) represented.add(fact.id());
      }
    }
    if (status == null || status == DONE) {
      for (var fact : facts.inPeriod(from, until, type, locationId, itemId)) {
        if (!represented.contains(fact.id())) result.add(actualView(fact));
      }
    }
    result.sort(
        Comparator.comparing(
                (View event) ->
                    event.actualStart() == null ? event.plannedStart() : event.actualStart())
            .thenComparing(View::id));
    return result;
  }

  @Transactional(readOnly = true)
  public View get(UUID id) {
    return detail(find(id));
  }

  private static LocalDate start(CalendarEvent event) {
    return event.actualStart == null ? event.plannedStart : event.actualStart;
  }

  private static LocalDate end(CalendarEvent event) {
    return event.actualEnd == null ? event.plannedEnd : event.actualEnd;
  }

  @Transactional(readOnly = true)
  public List<EventReference> conflictingSalePlans(UUID id, Draft draft) {
    if (draft.status() != PLANNED || draft.type() != SALE || draft.items() == null)
      return List.of();
    var itemIds = draft.items().stream().map(item -> key(item.itemId())).toList();
    if (itemIds.isEmpty()) return List.of();
    return events.openPlansForItems(SALE, PLANNED, id.toString(), itemIds).stream()
        .map(Calendars::reference)
        .toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public View create(Draft draft) {
    require(draft.id() != null && draft.version() == null, "Adj meg új eseményazonosítót.");
    var event = new CalendarEvent(draft.id().toString());
    var links = itemLinks(draft);
    lockItems(event, links.keySet());
    apply(event, draft, links);
    entities.persist(event);
    events.flush();
    return detail(event);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public View update(UUID id, Draft draft) {
    require(draft.id() == null || id.equals(draft.id()), "Az eseményazonosító nem egyezik.");
    require(draft.version() != null && draft.version() >= 0, "Az esemény verziója kötelező.");
    CalendarEvent event = find(id);
    var links = itemLinks(draft);
    lockItems(event, links.keySet());
    entities.refresh(event);
    if (event.version != draft.version())
      throw new ResponseStatusException(CONFLICT, "A naptáresemény időközben megváltozott.");
    apply(event, draft, links);
    facts.removeInvalidLinks(event);
    events.flush();
    return detail(event);
  }

  private CalendarEvent find(UUID id) {
    return events
        .findById(id.toString())
        .orElseThrow(
            () -> new ResponseStatusException(NOT_FOUND, "A naptáresemény nem található."));
  }

  private void lockItems(CalendarEvent event, Set<String> newIds) {
    var ids = new TreeSet<>(event.items.keySet());
    ids.addAll(newIds);
    if (!ids.isEmpty()) {
      var existing = inventory.lockForCalendar(ids).stream().map(item -> item.id).toList();
      require(existing.containsAll(newIds), "Ismeretlen tárgy.");
    }
  }

  private void apply(CalendarEvent event, Draft draft, Map<String, CalendarEvent.EventItem> links) {
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
    var linked = facts.attached(List.of(event.id)).getOrDefault(event.id, List.of());
    return view(event, facts.today(), warnings, linked);
  }

  private static View actualView(CalendarFacts.Fact fact) {
    String title = (fact.type() == SALE ? "Eladva: " : "Költöztetve: ") + fact.itemName();
    return new View(
        fact.id(),
        fact.version(),
        title,
        fact.type(),
        DONE,
        null,
        fact.destinationLocationId() == null
            ? fact.originLocationId()
            : fact.destinationLocationId(),
        null,
        null,
        fact.date(),
        fact.date(),
        false,
        List.of(new ItemLink(fact.itemId(), fact.originLocationId())),
        List.of(),
        Source.INVENTORY,
        List.of(fact));
  }

  private static View view(
      CalendarEvent event,
      LocalDate today,
      List<Warning> warnings,
      List<CalendarFacts.Fact> facts) {
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
        warnings,
        Source.CALENDAR,
        facts);
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
