package hu.boros.inventory;

import static hu.boros.inventory.CalendarEvent.Status.PLANNED;
import static hu.boros.inventory.CalendarEvent.Type.*;
import static org.springframework.http.HttpStatus.*;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class CalendarFacts {
  record Fact(
      UUID id,
      long version,
      CalendarEvent.Type type,
      UUID itemId,
      String itemName,
      LocalDate date,
      UUID originLocationId,
      UUID destinationLocationId,
      UUID eventId,
      boolean withdrawn) {}

  record Assignment(UUID eventId, long eventVersion, long factVersion) {}

  private final EntityManager entities;
  private final CalendarEventRepository events;
  private final CalendarActualRepository actuals;
  private final ItemRepository items;
  private final Clock clock;

  CalendarFacts(
      EntityManager entities,
      CalendarEventRepository events,
      CalendarActualRepository actuals,
      ItemRepository items,
      Clock clock) {
    this.entities = entities;
    this.events = events;
    this.actuals = actuals;
    this.items = items;
    this.clock = clock;
  }

  LocalDate today() {
    return LocalDate.now(clock.withZone(ZoneId.of("Europe/Budapest")));
  }

  static UUID saleId(String itemId) {
    return UUID.nameUUIDFromBytes(
        ("otthontar:calendar:sale:" + itemId).getBytes(StandardCharsets.UTF_8));
  }

  void recordSale(String itemId, boolean newlySold) {
    var previous = entities.find(CalendarFact.class, saleId(itemId).toString());
    if (!newlySold && previous != null && previous.eventId != null) return;
    var plan = onlyPlan(SALE, itemId);
    if (plan != null) attachSale(itemId, previous, plan);
  }

  void recordMove(String itemId, String origin, String destination) {
    var fact = new CalendarFact(UUID.randomUUID().toString(), MOVE, itemId);
    fact.movedOn = today();
    fact.originLocationId = origin;
    fact.destinationLocationId = destination;
    fact.eventId = onlyPlan(MOVE, itemId);
    entities.persist(fact);
  }

  private String onlyPlan(CalendarEvent.Type type, String itemId) {
    var matches = events.openPlansForItems(type, PLANNED, "", List.of(itemId));
    return matches.size() == 1 ? matches.getFirst().id : null;
  }

  private void attachSale(String itemId, CalendarFact existing, String plan) {
    if (existing != null) {
      existing.eventId = plan;
    } else {
      var fact = new CalendarFact(saleId(itemId).toString(), SALE, itemId);
      fact.eventId = plan;
      entities.persist(fact);
    }
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Fact assign(UUID factId, Assignment assignment) {
    var before = findActual(factId);
    Ledger.changed(items.lockCurrent(before.itemId).isPresent() ? 1 : 0);
    entities.refresh(before);
    if (before.sourceVersion != assignment.factVersion() || before.withdrawn != 0)
      throw new ResponseStatusException(CONFLICT, "A leltári tétel időközben megváltozott.");
    Ledger.require(assignment.eventId() != null, "Válassz naptáreseményt.");
    var plan =
        events
            .findById(assignment.eventId().toString())
            .orElseThrow(
                () -> new ResponseStatusException(NOT_FOUND, "A naptáresemény nem található."));
    if (plan.version != assignment.eventVersion()
        || (before.eventId != null && !before.eventId.equals(plan.id)))
      throw new ResponseStatusException(
          CONFLICT, "Az esemény vagy a kapcsolata időközben megváltozott.");
    Ledger.require(
        plan.status != CalendarEvent.Status.CANCELLED
            && plan.type == before.factType
            && plan.items.containsKey(before.itemId),
        "Az esemény típusának és a kapcsolt tárgynak egyeznie kell.");
    var stored = entities.find(CalendarFact.class, factId.toString());
    if (before.factType == SALE) attachSale(before.itemId, stored, plan.id);
    else stored.eventId = plan.id;
    entities.flush();
    entities.refresh(before);
    return fact(before);
  }

  @Transactional(readOnly = true)
  public List<Calendars.EventReference> candidates(UUID factId) {
    var actual = findActual(factId);
    if (actual.withdrawn != 0 || actual.eventId != null) return List.of();
    return events.linkable(actual.factType, CalendarEvent.Status.CANCELLED, actual.itemId).stream()
        .map(Calendars::reference)
        .toList();
  }

  @Transactional(readOnly = true)
  public Fact get(UUID id) {
    return fact(findActual(id));
  }

  private CalendarActual findActual(UUID id) {
    var row = entities.find(CalendarActual.class, id.toString());
    if (row == null) throw new ResponseStatusException(NOT_FOUND, "A leltári tétel nem található.");
    return row;
  }

  void removeInvalidLinks(CalendarEvent event) {
    for (var fact :
        entities
            .createQuery("select f from CalendarFact f where f.eventId = :id", CalendarFact.class)
            .setParameter("id", event.id)
            .getResultList()) {
      if (event.type != fact.factType || !event.items.containsKey(fact.itemId)) fact.eventId = null;
    }
  }

  Map<String, List<Fact>> attached(Collection<String> ids) {
    if (ids.isEmpty()) return Map.of();
    return actuals.findByEventIdIn(ids).stream()
        .map(CalendarFacts::fact)
        .sorted(
            Comparator.comparing(Fact::date, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Fact::id))
        .collect(Collectors.groupingBy(f -> f.eventId().toString()));
  }

  List<Fact> inPeriod(
      LocalDate from, LocalDate until, CalendarEvent.Type type, UUID locationId, UUID itemId) {
    return actuals
        .inPeriod(
            from, until, type, Objects.toString(locationId, null), Objects.toString(itemId, null))
        .stream()
        .map(CalendarFacts::fact)
        .toList();
  }

  private static Fact fact(CalendarActual row) {
    return new Fact(
        uuid(row.id),
        row.sourceVersion,
        row.factType,
        uuid(row.itemId),
        row.itemName,
        row.actualOn,
        uuid(row.originLocationId),
        uuid(row.destinationLocationId),
        uuid(row.eventId),
        row.withdrawn != 0);
  }

  private static UUID uuid(String key) {
    return key == null ? null : UUID.fromString(key);
  }
}

@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
class CalendarTime {
  @Bean
  Clock calendarClock() {
    return Clock.systemUTC();
  }
}
