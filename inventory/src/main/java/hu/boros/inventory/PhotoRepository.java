package hu.boros.inventory;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<PhotoEntity, String> {
  List<PhotoEntity> findByItemIdOrderByPositionAscIdAsc(String itemId);
}
