package hu.boros.inventory;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {
  Optional<AccountEntity> findByUsername(String username);
}
