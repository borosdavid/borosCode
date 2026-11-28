package hu.boros.inventory;

import static org.springframework.http.HttpStatus.CONFLICT;

import java.util.*;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;

@Service
class CalendarLocationChanges {
  record Approval(UUID from, UUID to, Map<UUID, Long> events) {}

  private final CalendarEventRepository events;

  CalendarLocationChanges(CalendarEventRepository events) {
    this.events = events;
  }

  void apply(String itemId, String oldLocation, UUID newLocation, Approval approval) {
    if (newLocation.toString().equals(oldLocation) && approval == null) return;
    var related = events.openForItem(CalendarEvent.Status.PLANNED, itemId);
    var versions = new LinkedHashMap<UUID, Long>();
    for (var event : related) versions.put(UUID.fromString(event.id), event.version);
    var expected = new Approval(UUID.fromString(oldLocation), newLocation, versions);
    if (!expected.equals(approval) && (approval != null || !related.isEmpty())) {
      var detail =
          ProblemDetail.forStatusAndDetail(
              CONFLICT,
              "A következő naptáresemények érintettek. Ellenőrizd és hagyd jóvá a helyváltoztatást.");
      detail.setProperty("code", "CALENDAR_LOCATION_APPROVAL");
      detail.setProperty("approval", expected);
      detail.setProperty("events", related.stream().map(Calendars::reference).toList());
      throw new ErrorResponseException(CONFLICT, detail, null);
    }
    for (var event : related) {
      var previous = event.items.get(itemId);
      event.items.put(
          itemId, new CalendarEvent.EventItem(newLocation.toString(), previous.reservesSale()));
    }
  }
}
