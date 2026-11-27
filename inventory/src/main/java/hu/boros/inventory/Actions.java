package hu.boros.inventory;

import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class Actions {
  record Choice(String id, String name, String kind, boolean active) {}

  private final EntityManager entities;
  private final ActionRepository actions;

  Actions(EntityManager entities, ActionRepository actions) {
    this.entities = entities;
    this.actions = actions;
  }

  @Transactional(readOnly = true)
  public List<Choice> all() {
    return actions.ordered().stream()
        .map(action -> new Choice(action.id, action.name, action.kind, action.active))
        .toList();
  }

  @Transactional
  public void save(Choice choice) {
    Ledger.require(
        choice.name() != null && !choice.name().isBlank() && choice.name().strip().length() <= 100,
        "Adj meg 1–100 karakteres nevet.");
    String name = choice.name().strip();
    if (choice.id() == null) {
      Ledger.require(
          "MOVE".equals(choice.kind()) || "CUSTOM".equals(choice.kind()), "Válassz típust.");
      entities.persist(new ActionEntity(UUID.randomUUID().toString(), name, choice.kind()));
      actions.flush();
    } else {
      Ledger.changed(actions.updateSettings(choice.id(), name, choice.active()));
    }
  }
}
