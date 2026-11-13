package hu.boros.inventory;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LedgerTest {
  @Test
  void staleVersionIsAConflict() {
    var ex = assertThrows(ResponseStatusException.class, () -> Ledger.changed(0));
    assertEquals(409, ex.getStatusCode().value());
  }

  @Test
  void acceptsUnlistedItemWithoutPrice() {
    Ledger.validate(draft(null, ""));
  }

  @Test
  void rejectsNegativePriceAndExecutableLink() {
    assertThrows(
        ResponseStatusException.class, () -> Ledger.validate(draft(new BigDecimal("-1"), "")));
    assertThrows(
        ResponseStatusException.class, () -> Ledger.validate(draft(null, "javascript:alert(1)")));
  }

  private Ledger.Draft draft(BigDecimal price, String link) {
    return new Ledger.Draft(
        UUID.randomUUID(),
        0,
        "Lámpa",
        "",
        UUID.randomUUID(),
        "WAIT",
        price,
        link,
        null,
        null,
        null);
  }
}
