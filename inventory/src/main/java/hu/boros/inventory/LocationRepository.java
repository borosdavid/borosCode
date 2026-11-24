package hu.boros.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LocationRepository extends JpaRepository<LocationEntity, String> {
  List<LocationEntity> findAllByOrderByNameAsc();

  @Modifying
  @Query("update LocationEntity location set location.name = :name where location.id = :id")
  int rename(@Param("id") String id, @Param("name") String name);
}
