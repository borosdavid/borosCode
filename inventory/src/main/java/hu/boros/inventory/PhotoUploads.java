package hu.boros.inventory;

import static org.springframework.http.HttpStatus.*;

import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ProblemDetail;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
class PhotoUploads {
  record Prepared(String id, String filename) {}

  private final EntityManager entities;
  private final Photos photos;
  private final long maxBatchBytes;

  PhotoUploads(
      EntityManager entities,
      Photos photos,
      @Value("${app.photos-max-batch-size}") DataSize maxBatch) {
    this.entities = entities;
    this.photos = photos;
    this.maxBatchBytes = maxBatch.toBytes();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void stage(String account, UUID uploadId, MultipartFile file) {
    Photos.validate(file);
    String key = uploadId.toString();
    String fingerprint = fingerprint(file);
    entities
        .createNativeQuery(
            """
        insert into pending_photo_upload(account_id, upload_id, fingerprint)
        values (:account, :upload, :fingerprint)
        on duplicate key update upload_id = upload_id
        """)
        .setParameter("account", account)
        .setParameter("upload", key)
        .setParameter("fingerprint", fingerprint)
        .executeUpdate();
    Object[] receipt =
        (Object[])
            entities
                .createNativeQuery(
                    """
        select fingerprint, photo_id from pending_photo_upload
        where account_id = :account and upload_id = :upload for update
        """)
                .setParameter("account", account)
                .setParameter("upload", key)
                .getSingleResult();
    if (!fingerprint.equals(receipt[0]))
      throw new ResponseStatusException(CONFLICT, "Ehhez a feltöltéshez másik kép tartozik.");
    String photo = receipt[1] == null ? photos.store(file) : (String) receipt[1];
    entities
        .createNativeQuery(
            """
        update pending_photo_upload
        set photo_id = :photo, filename = :name, byte_size = :size,
            expires_at = current_timestamp(6) + interval 1 day
        where account_id = :account and upload_id = :upload
        """)
        .setParameter("account", account)
        .setParameter("upload", key)
        .setParameter("photo", photo)
        .setParameter("name", file.getOriginalFilename())
        .setParameter("size", file.getSize())
        .executeUpdate();
  }

  // The caller owns the item-save transaction. A rejected save keeps its uploaded photos pending.
  List<Prepared> take(String account, List<UUID> uploadIds) {
    var ids = uploadIds.stream().map(UUID::toString).toList();
    var byId = new HashMap<String, Prepared>();
    long bytes = 0;
    for (String id : new TreeSet<>(ids)) {
      var found =
          entities
              .createNativeQuery(
                  """
          select photo_id, filename, byte_size from pending_photo_upload
          where account_id = :account and upload_id = :upload
            and expires_at > current_timestamp(6) for update
          """)
              .setParameter("account", account)
              .setParameter("upload", id)
              .getResultList();
      if (found.isEmpty()) {
        var problem =
            ProblemDetail.forStatusAndDetail(
                CONFLICT, "A képfeltöltés lejárt vagy hiányos. A nyitott képek újra feltölthetők.");
        problem.setProperty("code", "PHOTO_UPLOAD_MISSING");
        throw new ErrorResponseException(CONFLICT, problem, null);
      }
      var row = (Object[]) found.getFirst();
      byId.put(id, new Prepared((String) row[0], (String) row[1]));
      bytes += ((Number) row[2]).longValue();
      if (bytes > maxBatchBytes)
        throw new ResponseStatusException(
            PAYLOAD_TOO_LARGE, "A fotók túllépik a mentési méretkorlátot.");
    }
    for (String id : ids) {
      entities
          .createNativeQuery(
              """
          delete from pending_photo_upload where account_id = :account and upload_id = :upload
          """)
          .setParameter("account", account)
          .setParameter("upload", id)
          .executeUpdate();
    }
    return ids.stream().map(byId::get).toList();
  }

  @Scheduled(fixedDelay = 60_000)
  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void expire() {
    var expired =
        entities
            .createNativeQuery(
                """
        select account_id, upload_id, photo_id from pending_photo_upload
        where expires_at <= current_timestamp(6)
        order by expires_at limit 100 for update skip locked
        """)
            .getResultList();
    for (Object entry : expired) {
      var row = (Object[]) entry;
      entities
          .createNativeQuery("insert into file_cleanup(id) values (:photo)")
          .setParameter("photo", row[2])
          .executeUpdate();
      entities
          .createNativeQuery(
              """
          delete from pending_photo_upload where account_id = :account and upload_id = :upload
          """)
          .setParameter("account", row[0])
          .setParameter("upload", row[1])
          .executeUpdate();
    }
  }

  private static String fingerprint(MultipartFile file) {
    try (var output =
            new DigestOutputStream(
                OutputStream.nullOutputStream(), MessageDigest.getInstance("SHA-256"));
        var input = file.getInputStream()) {
      output.write(file.getOriginalFilename().getBytes(StandardCharsets.UTF_8));
      output.write(0);
      input.transferTo(output);
      return HexFormat.of().formatHex(output.getMessageDigest().digest());
    } catch (IOException e) {
      throw new ResponseStatusException(
          SERVICE_UNAVAILABLE, "A kép nem olvasható. Próbáld újra.", e);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
