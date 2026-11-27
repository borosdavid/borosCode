package hu.boros.inventory;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface CalendarEventRepository extends JpaRepository<CalendarEvent, String> {
  @Query(
      """
      select e from CalendarEvent e
      where coalesce(e.actualStart, e.plannedStart) < :until
        and coalesce(e.actualEnd, e.plannedEnd) >= :from
        and (:type is null or e.type = :type)
        and (:status is null or e.status = :status)
        and (:itemId is null or :itemId in (select key(link) from CalendarEvent ev
              join ev.items link where ev.id = e.id))
        and (:locationId is null or e.locationId = :locationId or exists (
              select 1 from CalendarEvent ev join ev.items link
              where ev.id = e.id and link.originLocationId = :locationId))
      order by coalesce(e.actualStart, e.plannedStart), e.id
      """)
  List<CalendarEvent> inPeriod(
      LocalDate from,
      LocalDate until,
      CalendarEvent.Type type,
      CalendarEvent.Status status,
      String locationId,
      String itemId);

  @Query(
      """
      select distinct e.id from CalendarEvent e join e.items link
      where e.type = :otherType and e.status = :status
        and e.plannedStart <= :end and e.plannedEnd >= :start
        and key(link) in :itemIds
      order by e.id
      """)
  List<String> overlapping(
      CalendarEvent.Type otherType,
      CalendarEvent.Status status,
      LocalDate start,
      LocalDate end,
      Collection<String> itemIds);

  @Query(
      """
      select distinct e from CalendarEvent e join e.items link
      where e.type = :type and e.status = :status and e.id <> :excludedId
        and key(link) in :itemIds
      order by e.plannedStart, e.id
      """)
  List<CalendarEvent> openPlansForItems(
      CalendarEvent.Type type,
      CalendarEvent.Status status,
      String excludedId,
      Collection<String> itemIds);
}
