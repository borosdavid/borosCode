package hu.boros.inventory;

import static org.springframework.http.HttpStatus.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calendar")
class CalendarWeb {
  private final Calendars calendars;

  CalendarWeb(Calendars calendars) {
    this.calendars = calendars;
  }

  @GetMapping
  List<Calendars.View> list(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate until,
      @RequestParam(required = false) CalendarEvent.Type type,
      @RequestParam(required = false) CalendarEvent.Status status,
      @RequestParam(required = false) UUID locationId,
      @RequestParam(required = false) UUID itemId) {
    return calendars.list(from, until, type, status, locationId, itemId);
  }

  @GetMapping("/{id}")
  Calendars.View detail(@PathVariable UUID id) {
    return calendars.get(id);
  }

  @PostMapping
  @ResponseStatus(CREATED)
  Calendars.View create(@RequestBody Calendars.Draft draft) {
    return save(null, draft);
  }

  @PutMapping("/{id}")
  Calendars.View update(@PathVariable UUID id, @RequestBody Calendars.Draft draft) {
    return save(id, draft);
  }

  private Calendars.View save(UUID id, Calendars.Draft draft) {
    try {
      return id == null ? calendars.create(draft) : calendars.update(id, draft);
    } catch (DataIntegrityViolationException failure) {
      var related = calendars.conflictingSalePlans(id == null ? draft.id() : id, draft);
      if (related.isEmpty()) throw failure;
      var problem =
          ProblemDetail.forStatusAndDetail(CONFLICT, "A tárgynak már van nyitott eladási terve.");
      problem.setProperty("conflictingEvents", related);
      throw new ErrorResponseException(CONFLICT, problem, null);
    }
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail stale() {
    return ProblemDetail.forStatusAndDetail(CONFLICT, "A naptáresemény időközben megváltozott.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail conflict() {
    return ProblemDetail.forStatusAndDetail(
        CONFLICT,
        "Az eseményazonosító foglalt, a tárgynak már van nyitott eladási terve, vagy egy hivatkozott adat megváltozott.");
  }
}
