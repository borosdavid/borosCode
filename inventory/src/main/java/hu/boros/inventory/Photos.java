package hu.boros.inventory;

import static org.springframework.http.HttpStatus.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Photos {
  final Path root;
  final URI processor;
  final JdbcTemplate db;
  final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
  final Logger log = LoggerFactory.getLogger(Photos.class);

  Photos(@Value("${app.photos}") String path, @Value("${app.imaging}") String url, JdbcTemplate db)
      throws IOException {
    root = Files.createDirectories(Path.of(path).toAbsolutePath());
    processor = URI.create(url + "/thumbnail");
    this.db = db;
  }

  Path path(String id, boolean small) {
    return root.resolve(UUID.fromString(id) + (small ? ".small.jpg" : ".jpg"));
  }

  String store(MultipartFile file) {
    String name = file.getOriginalFilename();
    Ledger.require(
        name != null
            && name.length() <= 255
            && name.toLowerCase(Locale.ROOT).matches(".+\\.jpe?g")
            && file.getSize() > 0
            && file.getSize() <= 20L * 1024 * 1024,
        "Hibás JPG vagy túl nagy fájl: " + name);
    String id = UUID.randomUUID().toString();
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) remove(id);
          }
        });
    try {
      byte[] original = file.getBytes();
      var req =
          HttpRequest.newBuilder(processor)
              .timeout(Duration.ofSeconds(30))
              .header("Content-Type", "image/jpeg")
              .header("X-Request-Id", Objects.toString(MDC.get("requestId"), id))
              .POST(HttpRequest.BodyPublishers.ofByteArray(original))
              .build();
      var result = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
      if (result.statusCode() == 422)
        throw new ResponseStatusException(UNPROCESSABLE_ENTITY, "Érvénytelen JPG: " + name);
      if (result.statusCode() != 200) throw new IOException("HTTP " + result.statusCode());
      Files.write(path(id, false), original, StandardOpenOption.CREATE_NEW);
      Files.write(path(id, true), result.body(), StandardOpenOption.CREATE_NEW);
      return id;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Megszakadt: " + name);
    } catch (IOException e) {
      log.warn("Képfeldolgozási hiba", e);
      throw new ResponseStatusException(SERVICE_UNAVAILABLE, "Próbáld újra a mentést: " + name);
    }
  }

  boolean remove(String id) {
    try {
      Files.deleteIfExists(path(id, false));
      Files.deleteIfExists(path(id, true));
      return true;
    } catch (IOException e) {
      log.warn("Fájltörlés újrapróbálása szükséges", e);
      return false;
    }
  }

  @Scheduled(fixedDelay = 10000)
  public void cleanup() {
    for (String id : db.queryForList("SELECT id FROM file_cleanup LIMIT 100", String.class))
      if (remove(id)) db.update("DELETE FROM file_cleanup WHERE id=?", id);
  }
}
