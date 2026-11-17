package hu.boros.inventory;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<ItemEntity, String> {
  @Query(
      """
      select item from ItemEntity item join fetch item.location
      where item.trashed = :trash
      order by case when item.status = 'SOLD' then 1 else 0 end, item.name, item.id
      """)
  List<ItemEntity> listed(@Param("trash") boolean trash);

  boolean existsByIdAndActionId(String id, String actionId);

  @Modifying
  @Query(
      """
      update ItemEntity item set item.name = :name, item.description = :description,
        item.locationId = :locationId, item.status = :status, item.asking = :asking,
        item.link = :link, item.actual = :actual, item.soldDate = :soldDate,
        item.actionId = :actionId, item.destinationId = :destinationId,
        item.version = item.version + 1
      where item.id = :id and item.version = :version and item.trashed = false
      """)
  int updateDraft(
      @Param("id") String id,
      @Param("version") long version,
      @Param("name") String name,
      @Param("description") String description,
      @Param("locationId") String locationId,
      @Param("status") String status,
      @Param("asking") BigDecimal asking,
      @Param("link") String link,
      @Param("actual") BigDecimal actual,
      @Param("soldDate") LocalDate soldDate,
      @Param("actionId") String actionId,
      @Param("destinationId") String destinationId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("update ItemEntity item set item.cover = :cover where item.id = :id")
  int updateCover(@Param("id") String id, @Param("cover") String cover);

  @Modifying
  @Query(
      """
      update ItemEntity item set item.status = 'SOLD', item.actual = :actual,
        item.soldDate = :soldDate, item.version = item.version + 1
      where item.id = :id and item.version = :version and item.trashed = false
        and item.status in ('WAIT', 'LISTED')
      """)
  int completeSale(
      @Param("id") String id,
      @Param("version") long version,
      @Param("actual") BigDecimal actual,
      @Param("soldDate") LocalDate soldDate);

  @Modifying
  @Query(
      """
      update ItemEntity item set item.locationId = item.destinationId,
        item.destinationId = null, item.actionId = null, item.version = item.version + 1
      where item.id = :id and item.version = :version and item.trashed = false
        and item.destinationId is not null
        and item.actionId in (select action.id from ActionEntity action where action.kind = 'MOVE')
      """)
  int completeMove(@Param("id") String id, @Param("version") long version);

  @Modifying
  @Query(
      """
      update ItemEntity item set item.trashed = :trash, item.version = item.version + 1
      where item.id = :id and item.version = :version and item.trashed <> :trash
      """)
  int setTrashed(
      @Param("id") String id, @Param("version") long version, @Param("trash") boolean trash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select item from ItemEntity item
      where item.id = :id and item.version = :version and item.trashed = true
      """)
  Optional<ItemEntity> lockForPurge(@Param("id") String id, @Param("version") long version);

  @Modifying
  @Query("delete from ItemEntity item where item.id = :id")
  int purge(@Param("id") String id);
}
