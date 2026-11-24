package hu.boros.inventory;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ActionRepository extends JpaRepository<ActionEntity, String> {
  @Query(
      """
      select action from ActionEntity action
      order by case action.kind when 'KEEP' then 1 when 'SALE' then 2
        when 'MOVE' then 3 when 'CUSTOM' then 4 else 0 end, action.name
      """)
  List<ActionEntity> ordered();

  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query("select action from ActionEntity action where action.id = :id")
  Optional<ActionEntity> lockForAssignment(@Param("id") String id);

  @Modifying
  @Query(
      """
      update ActionEntity action set action.name = :name, action.active = :active
      where action.id = :id
      """)
  int updateSettings(
      @Param("id") String id, @Param("name") String name, @Param("active") boolean active);
}
