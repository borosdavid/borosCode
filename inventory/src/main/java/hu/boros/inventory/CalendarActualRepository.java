package hu.boros.inventory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

interface CalendarActualRepository extends Repository<CalendarActual, String> {
  List<CalendarActual> findByEventIdIn(Collection<String> eventIds);

  @Query(
      """
      select f from CalendarActual f
      where f.actualOn >= :from and f.actualOn < :until
        and (:type is null or f.factType = :type)
        and (:locationId is null or f.originLocationId = :locationId or f.destinationLocationId = :locationId)
        and (:itemId is null or f.itemId = :itemId)
      """)
  List<CalendarActual> inPeriod(
      LocalDate from, LocalDate until, CalendarEvent.Type type, String locationId, String itemId);
}
