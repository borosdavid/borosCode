package hu.boros.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public interface FileCleanupRepository extends JpaRepository<FileCleanupEntity, String> {
  List<FileCleanupEntity> findTop100ByOrderByIdAsc();

  @Modifying
  @Query(
      value = "insert into file_cleanup(id) select id from photo where item_id = :itemId",
      nativeQuery = true)
  int enqueueForItem(@Param("itemId") String itemId);

  @Modifying
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Query(value = "insert ignore into file_cleanup(id) values (:id)", nativeQuery = true)
  void enqueueRollback(@Param("id") String id);
}
