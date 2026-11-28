package hu.boros.inventory;

import static org.springframework.http.HttpStatus.*;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ItemSaves {
  private final EntityManager entities;
  private final Ledger ledger;
  private final ObjectMapper json;
  private final PhotoUploads pending;

  ItemSaves(EntityManager entities, Ledger ledger, ObjectMapper json, PhotoUploads pending) {
    this.entities = entities;
    this.ledger = ledger;
    this.json = json;
    this.pending = pending;
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public InventoryViews.ItemView save(
      String accountId,
      UUID operation,
      Ledger.Draft draft,
      List<MultipartFile> uploads,
      CalendarLocationChanges.Approval approval) {
    return retainReceipt(
        accountId,
        operation,
        draft,
        fingerprint(draft, uploads, approval, null),
        () -> ledger.save(draft, uploads, approval));
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public InventoryViews.ItemView saveStaged(
      String accountId,
      UUID operation,
      Ledger.Draft draft,
      List<UUID> staged,
      CalendarLocationChanges.Approval approval) {
    Ledger.require(
        !staged.isEmpty()
            && !staged.contains(null)
            && new HashSet<>(staged).size() == staged.size(),
        "Hibás képlista.");
    return retainReceipt(
        accountId,
        operation,
        draft,
        fingerprint(draft, List.of(), approval, staged),
        () -> ledger.save(draft, approval, () -> pending.take(accountId, staged)));
  }

  private InventoryViews.ItemView retainReceipt(
      String accountId, UUID operation, Ledger.Draft draft, String fingerprint, Runnable save) {
    String operationId = operation.toString();
    // The unique key serializes concurrent retries. A rollback removes the claim as well.
    entities
        .createNativeQuery(
            """
            insert into item_save_request(account_id, operation_id, fingerprint)
            values (:account, :operation, :fingerprint)
            on duplicate key update operation_id = operation_id
            """)
        .setParameter("account", accountId)
        .setParameter("operation", operationId)
        .setParameter("fingerprint", fingerprint)
        .executeUpdate();
    Object[] receipt =
        (Object[])
            entities
                .createNativeQuery(
                    """
                    select fingerprint, item_id from item_save_request
                    where account_id = :account and operation_id = :operation for update
                    """)
                .setParameter("account", accountId)
                .setParameter("operation", operationId)
                .getSingleResult();
    if (!fingerprint.equals(receipt[0]))
      throw new ResponseStatusException(
          CONFLICT, "A mentési azonosítóhoz másik tartalom tartozik.");
    if (receipt[1] != null) return ledger.item(UUID.fromString((String) receipt[1]));

    save.run();
    var saved = ledger.item(draft.id());
    entities
        .createNativeQuery(
            """
            update item_save_request set item_id = :item
            where account_id = :account and operation_id = :operation
            """)
        .setParameter("item", saved.id())
        .setParameter("account", accountId)
        .setParameter("operation", operationId)
        .executeUpdate();
    return saved;
  }

  private String fingerprint(
      Ledger.Draft draft,
      List<MultipartFile> uploads,
      CalendarLocationChanges.Approval approval,
      List<UUID> staged) {
    try (var output =
        new DigestOutputStream(
            OutputStream.nullOutputStream(), MessageDigest.getInstance("SHA-256"))) {
      if (staged != null) output.write(1); // Distinguish staged and legacy request fingerprints.
      writePart(output, json.writeValueAsBytes(draft));
      for (var upload : uploads) {
        String name = upload.getOriginalFilename();
        writePart(output, name == null ? new byte[0] : name.getBytes(StandardCharsets.UTF_8));
        output.write(ByteBuffer.allocate(Long.BYTES).putLong(upload.getSize()).array());
        try (var input = upload.getInputStream()) {
          input.transferTo(output);
        }
      }
      if (approval != null) {
        writePart(
            output,
            json.writeValueAsBytes(
                new CalendarLocationChanges.Approval(
                    approval.from(),
                    approval.to(),
                    approval.events() == null
                        ? null
                        : new java.util.TreeMap<>(approval.events()))));
      }
      if (staged != null) {
        output.write(
            Long.BYTES); // Distinguish staged saves from the existing length-prefixed format.
        writePart(output, json.writeValueAsBytes(staged));
      }
      return HexFormat.of().formatHex(output.getMessageDigest().digest());
    } catch (IOException e) {
      throw new ResponseStatusException(
          SERVICE_UNAVAILABLE, "A feltöltés nem olvasható. Próbáld újra.", e);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void writePart(OutputStream output, byte[] bytes) throws IOException {
    output.write(ByteBuffer.allocate(Long.BYTES).putLong(bytes.length).array());
    output.write(bytes);
  }
}
