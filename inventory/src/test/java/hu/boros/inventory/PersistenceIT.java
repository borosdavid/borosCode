package hu.boros.inventory;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import jakarta.persistence.EntityManagerFactory;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class PersistenceIT {
  @TempDir static Path photoRoot;

  static final HttpClient http =
      HttpClient.newBuilder()
          .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
          .connectTimeout(Duration.ofSeconds(3))
          .build();
  static final AtomicInteger imagingStatus = new AtomicInteger(200);
  static final AtomicInteger imagingCalls = new AtomicInteger();
  static final String legacyPlace = UUID.randomUUID().toString();
  static final List<String> legacyIds = new ArrayList<>();
  static final byte[] jpeg = Base64.getDecoder().decode("/9j/2Q==");
  static HttpServer imaging;
  static ConfigurableApplicationContext context;
  static ObjectMapper json;
  static String origin;
  static String csrfHeader;
  static String csrfToken;
  static String accountId;
  static final String accountPassword = UUID.randomUUID().toString();
  static String databaseUrl;
  static String databaseUser;
  static String databasePassword;
  static Map<String, String> originalSchema;
  static List<Map<String, Object>> legacyList;
  static Map<String, Map<String, Object>> legacyDetails;

  @BeforeAll
  static void start() throws Exception {
    databaseUrl = System.getProperty("test.db.url", "");
    assertTrue(
        databaseUrl.matches(
            "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/inventory_jpa_test(?:_[a-zA-Z0-9]+)?(?:\\?.*)?"),
        "Kizárólag a külön inventory_jpa_test séma használható, explicit test.db.url paraméterrel.");
    databaseUser = System.getProperty("test.db.user", "root");
    databasePassword = Objects.toString(System.getenv("OTTHONTAR_TEST_DB_PASSWORD"), "");
    Flyway.configure()
        .dataSource(databaseUrl, databaseUser, databasePassword)
        .target("2")
        .load()
        .migrate();
    try (var connection = connection();
        var sql = connection.createStatement()) {
      try (var result = sql.executeQuery("select count(*) from item")) {
        assertTrue(result.next());
        assertEquals(0, result.getInt(1), "Friss, üres tesztadatbázis szükséges.");
      }
      sql.executeUpdate("insert into location values ('" + legacyPlace + "','Régi helyszín')");
      for (int index = 0; index < 4; index++) {
        String id = UUID.randomUUID().toString();
        legacyIds.add(id);
        String status = index == 0 ? "SOLD" : index == 1 ? "KEEP" : "WAIT";
        sql.executeUpdate(
            "insert into item(id,version,name,description,location_id,status,actual,sold_date,trashed,action_id) values ('"
                + id
                + "',7,'Régi "
                + index
                + "','Ékezetes leírás','"
                + legacyPlace
                + "','"
                + status
                + "',999999999999.99,'2026-11-13',"
                + (index == 3 ? 1 : 0)
                + ",'"
                + (status.equals("KEEP") ? "KEEP" : "SALE")
                + "')");
      }
      String firstPhoto = UUID.randomUUID().toString();
      String secondPhoto = UUID.randomUUID().toString();
      sql.executeUpdate(
          "insert into photo values ('"
              + secondPhoto
              + "','"
              + legacyIds.getFirst()
              + "','második.jpg',1),('"
              + firstPhoto
              + "','"
              + legacyIds.getFirst()
              + "','első.jpg',0)");
      sql.executeUpdate(
          "update item set cover='" + firstPhoto + "' where id='" + legacyIds.getFirst() + "'");
    }
    Flyway.configure()
        .dataSource(databaseUrl, databaseUser, databasePassword)
        .target("4")
        .load()
        .migrate();
    originalSchema = schema();
    legacyList =
        rows(
            "select i.*,l.name as place from item i join location l on i.location_id=l.id where trashed=0 order by (status='SOLD'),i.name,i.id");
    legacyDetails = new HashMap<>();
    for (String id : legacyIds) {
      var item = rows("select * from item where id='" + id + "'").getFirst();
      item.put(
          "photos",
          rows("select id,filename from photo where item_id='" + id + "' order by position_no"));
      legacyDetails.put(id, item);
    }
    imaging = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    imaging.createContext(
        "/thumbnail",
        exchange -> {
          imagingCalls.incrementAndGet();
          exchange.getRequestBody().readAllBytes();
          exchange.getResponseHeaders().set("Content-Type", "image/jpeg");
          exchange.sendResponseHeaders(imagingStatus.get(), jpeg.length);
          exchange.getResponseBody().write(jpeg);
          exchange.close();
        });
    imaging.start();
    startApplication();
  }

  static void startApplication() {
    context =
        new SpringApplicationBuilder(Inventory.class)
            .run(
                "--server.port=0",
                "--server.address=127.0.0.1",
                "--server.servlet.session.cookie.secure=false",
                "--spring.datasource.url=" + databaseUrl,
                "--spring.datasource.username=" + databaseUser,
                "--spring.datasource.password=" + databasePassword,
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.jpa.open-in-view=false",
                "--spring.jpa.properties.hibernate.generate_statistics=true",
                "--logging.level.root=WARN",
                "--logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
                "--app.photos=" + photoRoot,
                "--app.imaging=http://127.0.0.1:" + imaging.getAddress().getPort());
    context.getBean(ScheduledAnnotationBeanPostProcessor.class).destroy();
    json = context.getBean(ObjectMapper.class);
    origin = "http://127.0.0.1:" + context.getEnvironment().getProperty("local.server.port");
    try {
      var accounts = context.getBean(AccountRepository.class);
      if (accounts.findByUsername("persistence-test").isEmpty())
        accounts.saveAndFlush(
            new AccountEntity(
                "persistence-test",
                context
                    .getBean(org.springframework.security.crypto.password.PasswordEncoder.class)
                    .encode(accountPassword)));
      var token =
          json.readTree(
              http.send(
                      HttpRequest.newBuilder(URI.create(origin + "/auth/csrf")).GET().build(),
                      HttpResponse.BodyHandlers.ofByteArray())
                  .body());
      var login =
          http.send(
              HttpRequest.newBuilder(URI.create(origin + "/login"))
                  .header("Accept", "application/json")
                  .header("Content-Type", "application/x-www-form-urlencoded")
                  .header(token.get("headerName").asText(), token.get("token").asText())
                  .POST(
                      HttpRequest.BodyPublishers.ofString(
                          "username=persistence-test&password=" + accountPassword))
                  .build(),
              HttpResponse.BodyHandlers.ofByteArray());
      assertStatus(204, login);
      var session = get("/api/session");
      csrfHeader = session.get("headerName").asText();
      csrfToken = session.get("token").asText();
      accountId = session.get("id").asText();
    } catch (Exception ex) {
      throw new IllegalStateException("Test account login failed", ex);
    }
  }

  @BeforeEach
  void resetImaging() {
    imagingStatus.set(200);
  }

  @AfterAll
  static void stop() {
    if (context != null) context.close();
    if (imaging != null) imaging.stop(0);
  }

  @Test
  void preservesLegacyRowsJsonContractsOrderingAndSchema() throws Exception {
    assertEquals(originalSchema, schema());
    var listed = get("/api/items?trash=false");
    var oldRows = new ArrayList<JsonNode>();
    for (var row : listed) if (legacyIds.contains(row.get("id").asText())) oldRows.add(row);
    assertEquals(json.readTree(json.writeValueAsBytes(legacyList)), json.valueToTree(oldRows));
    for (String id : legacyIds) {
      assertEquals(
          json.readTree(json.writeValueAsBytes(legacyDetails.get(id))), get("/api/items/" + id));
    }
    var trash = get("/api/items?trash=true");
    assertTrue(containsId(trash, legacyIds.getLast()));
    assertFalse(containsId(listed, legacyIds.getLast()));
    assertEquals("UP", get("/actuator/health").get("status").asText());
    assertEquals(200, request("GET", "/leltar", null).statusCode());
    assertEquals(404, request("GET", "/api/items/" + UUID.randomUUID(), null).statusCode());
  }

  @Test
  void keepsPlaceAndActionValidationConflictsAndOrdering() throws Exception {
    String name = "Hely " + UUID.randomUUID();
    assertStatus(200, post("/api/places", Map.of("name", "  " + name + "  ")));
    assertStatus(409, post("/api/places", Map.of("name", name)));
    assertStatus(409, post("/api/places", Map.of("id", UUID.randomUUID(), "name", name + " új")));
    assertStatus(422, post("/api/places", Map.of("name", " ")));
    assertStatus(200, post("/api/places", Map.of("id", placeId(name), "name", name)));
    String actionName = "Művelet " + UUID.randomUUID();
    assertStatus(
        200, post("/api/actions", Map.of("name", actionName, "kind", "CUSTOM", "active", false)));
    var custom = actionNamed(actionName);
    assertTrue(custom.get("active").asBoolean());
    assertStatus(400, post("/api/actions", Map.of("name", actionName, "kind", "MOVE")));
    assertStatus(
        409, post("/api/actions", Map.of("name", actionName, "kind", "MOVE", "active", true)));
    assertStatus(
        422, post("/api/actions", Map.of("name", "Hibás", "kind", "SALE", "active", true)));
    assertStatus(
        409, post("/api/actions", Map.of("id", "missing", "name", "Hiányzik", "active", true)));
    assertStatus(
        200,
        post(
            "/api/actions",
            Map.of(
                "id",
                custom.get("id").asText(),
                "name",
                actionName,
                "kind",
                "SALE",
                "active",
                true)));
    assertEquals("CUSTOM", actionNamed(actionName).get("kind").asText());
    var actions = get("/api/actions");
    assertEquals(
        json.readTree(
            json.writeValueAsBytes(
                rows(
                    "select * from item_action order by FIELD(kind,'KEEP','SALE','MOVE','CUSTOM'),name"))),
        actions);
  }

  @Test
  void preservesInitialVersionDuplicateIdAndForeignKeyConflicts() throws Exception {
    var draft = draft();
    var created = saved(draft);
    assertEquals(1, created.get("version").asLong());
    assertStatus(409, save(draft));
    assertEquals(1, get("/api/items/" + draft.get("id")).get("version").asLong());
    draft.put("version", 1);
    assertEquals(2, saved(draft).get("version").asLong());
    assertStatus(409, save(draft));
    draft.put("id", UUID.randomUUID().toString());
    draft.put("version", 0);
    draft.put("location", UUID.randomUUID().toString());
    assertStatus(409, save(draft));
    assertStatus(404, request("GET", "/api/items/" + draft.get("id"), null));
    draft.put("location", legacyPlace);
    draft.put("actionId", "missing");
    assertStatus(422, save(draft));
    draft.put("actionId", "MOVE");
    draft.put("destination", UUID.randomUUID().toString());
    assertStatus(409, save(draft));
  }

  @Test
  void preservesInactiveActionsAndMoveRules() throws Exception {
    String actionName = "Költözés " + UUID.randomUUID();
    assertStatus(
        200, post("/api/actions", Map.of("name", actionName, "kind", "MOVE", "active", true)));
    String action = actionNamed(actionName).get("id").asText();
    String destination = createPlace();
    var draft = draft();
    draft.put("actionId", action);
    draft.put("destination", legacyPlace);
    assertStatus(422, save(draft));
    draft.put("destination", destination);
    assertEquals(1, saved(draft).get("version").asLong());
    assertStatus(
        200, post("/api/actions", Map.of("id", action, "name", actionName, "active", false)));
    draft.put("version", 1);
    assertEquals(2, saved(draft).get("version").asLong());
    var other = new LinkedHashMap<>(draft);
    other.put("id", UUID.randomUUID().toString());
    other.put("version", 0);
    assertStatus(422, save(other));
    String id = draft.get("id").toString();
    assertStatus(200, post("/api/items/" + id + "/move?version=2", null));
    var moved = get("/api/items/" + id);
    assertEquals(destination, moved.get("location_id").asText());
    assertTrue(moved.get("destination_id").isNull());
    assertTrue(moved.get("action_id").isNull());
    assertEquals(3, moved.get("version").asLong());
    assertStatus(409, post("/api/items/" + id + "/move?version=3", null));
    draft.put("id", UUID.randomUUID().toString());
    draft.put("version", 0);
    draft.put("actionId", "SALE");
    assertStatus(422, save(draft));
  }

  @Test
  void preservesSaleTrashRestoreAndPurgePreconditions() throws Exception {
    var draft = draft();
    String id = draft.get("id").toString();
    saved(draft);
    assertStatus(
        409,
        post(
            "/api/items/" + id + "/sold",
            Map.of("version", 1, "actual", 12, "soldDate", "2026-11-13")));
    draft.put("version", 1);
    draft.put("actionId", "SALE");
    draft.put("status", "WAIT");
    saved(draft);
    assertStatus(
        422,
        post(
            "/api/items/" + id + "/sold",
            Map.of("version", 2, "actual", -1, "soldDate", "2026-11-13")));
    assertStatus(422, post("/api/items/" + id + "/sold", Map.of("version", 2, "actual", 12)));
    assertStatus(
        200,
        post(
            "/api/items/" + id + "/sold",
            Map.of("version", 2, "actual", 12.34, "soldDate", "2026-11-13")));
    var sold = get("/api/items/" + id);
    assertEquals("SOLD", sold.get("status").asText());
    assertEquals("2026-11-13", sold.get("sold_date").asText());
    assertEquals(new BigDecimal("12.34"), sold.get("actual").decimalValue());
    assertStatus(
        409,
        post(
            "/api/items/" + id + "/sold",
            Map.of("version", 3, "actual", 12, "soldDate", "2026-11-13")));
    assertStatus(409, post("/api/items/" + id + "/restore?version=3", null));
    assertStatus(409, post("/api/items/" + id + "/purge?version=3", null));
    assertStatus(422, post("/api/items/" + id + "/unknown?version=3", null));
    assertStatus(200, post("/api/items/" + id + "/trash?version=3", null));
    assertFalse(containsId(get("/api/items"), id));
    assertTrue(containsId(get("/api/items?trash=true"), id));
    draft.put("version", 4);
    assertStatus(409, save(draft));
    assertStatus(409, post("/api/items/" + id + "/trash?version=4", null));
    assertStatus(200, post("/api/items/" + id + "/restore?version=4", null));
    assertStatus(200, post("/api/items/" + id + "/trash?version=5", null));
    assertStatus(200, post("/api/items/" + id + "/purge?version=6", null));
    assertStatus(404, request("GET", "/api/items/" + id, null));
  }

  @Test
  void storesOriginalsOrderedPhotosAndCoverWithoutExtraVersionIncrements() throws Exception {
    var draft = draft();
    draft.put("cover", "new:1");
    var created = saved(draft, upload("egy.jpg"), upload("kettő.jpeg"));
    String id = draft.get("id").toString();
    assertEquals(1, created.get("version").asLong());
    assertEquals(2, created.get("photos").size());
    assertEquals("egy.jpg", created.get("photos").get(0).get("filename").asText());
    String cover = created.get("photos").get(1).get("id").asText();
    assertEquals(cover, created.get("cover").asText());
    for (String size : List.of("original", "small")) {
      var response = request("GET", "/api/photos/" + cover + "/" + size, null);
      assertStatus(200, response);
      assertArrayEquals(jpeg, response.body());
      assertEquals("image/jpeg", response.headers().firstValue("Content-Type").orElseThrow());
    }
    assertStatus(404, request("GET", "/api/photos/" + cover + "/large", null));
    draft.put("version", 1);
    draft.put("cover", created.get("photos").get(0).get("id").asText());
    var updated = saved(draft, upload("három.jpg"));
    assertEquals(2, updated.get("version").asLong());
    assertEquals(3, updated.get("photos").size());
    assertEquals(draft.get("cover"), updated.get("cover").asText());
    int calls = imagingCalls.get();
    assertStatus(409, save(draft, upload("stale.jpg")));
    assertEquals(calls, imagingCalls.get());
    assertEquals(3, get("/api/items/" + id).get("photos").size());
    String orphan = UUID.randomUUID().toString();
    Files.write(photoRoot.resolve(orphan + ".jpg"), jpeg);
    assertStatus(404, request("GET", "/api/photos/" + orphan + "/original", null));
    Files.delete(photoRoot.resolve(cover + ".small.jpg"));
    assertStatus(404, request("GET", "/api/photos/" + cover + "/small", null));
  }

  @Test
  void rollsBackMetadataPhotosAndVersionAfterImagingOrCoverFailures() throws Exception {
    var draft = draft();
    saved(draft);
    String id = draft.get("id").toString();
    draft.put("version", 1);
    draft.put("name", "Nem menthető");
    long before = fileCount();
    draft.put("cover", UUID.randomUUID().toString());
    assertStatus(422, save(draft, upload("visszagörgetés.jpg")));
    assertEquals(before, fileCount());
    draft.remove("cover");
    imagingStatus.set(422);
    assertStatus(422, save(draft, upload("hibás.jpg")));
    imagingStatus.set(500);
    assertStatus(503, save(draft, upload("nem-elérhető.jpg")));
    assertEquals(before, fileCount());
    var unchanged = get("/api/items/" + id);
    assertEquals(1, unchanged.get("version").asLong());
    assertNotEquals("Nem menthető", unchanged.get("name").asText());
    assertTrue(unchanged.get("photos").isEmpty());
    imagingStatus.set(200);
    assertStatus(422, save(draft, upload("hibás.png")));
    assertEquals(2, saved(draft).get("version").asLong());
  }

  @Test
  void sqlFlushFailureCompensatesAlreadyWrittenFiles() throws Exception {
    var ledger = context.getBean(Ledger.class);
    var photos = context.getBean(Photos.class);
    long before = fileCount();
    assertThrows(
        RuntimeException.class,
        () ->
            transaction()
                .executeWithoutResult(
                    status -> {
                      String photo = photos.store(upload("sql-hiba.jpg"));
                      ledger.entities.persist(
                          new PhotoEntity(photo, UUID.randomUUID().toString(), "sql-hiba.jpg", 0));
                      ledger.photoRecords.flush();
                    }));
    assertEquals(before, fileCount());
  }

  @Test
  void purgeQueueSurvivesFileFailureAndApplicationRestart() throws Exception {
    var draft = draft();
    var item = saved(draft, upload("törlendő.jpg"));
    String id = draft.get("id").toString();
    String photo = item.get("cover").asText();
    Path blocked = photoRoot.resolve(photo + ".jpg");
    Files.delete(blocked);
    Files.createDirectory(blocked);
    Files.writeString(blocked.resolve("akadály"), "retry");
    assertStatus(200, post("/api/items/" + id + "/trash?version=1", null));
    assertStatus(200, post("/api/items/" + id + "/purge?version=2", null));
    assertTrue(context.getBean(FileCleanupRepository.class).existsById(photo));
    assertFalse(context.getBean(PhotoRepository.class).existsById(photo));
    context.close();
    startApplication();
    assertTrue(context.getBean(FileCleanupRepository.class).existsById(photo));
    assertStatus(404, request("GET", "/api/items/" + id, null));
    Files.delete(blocked.resolve("akadály"));
    Files.delete(blocked);
    context.getBean(Photos.class).cleanup();
    assertFalse(context.getBean(FileCleanupRepository.class).existsById(photo));
    assertFalse(Files.exists(photoRoot.resolve(photo + ".small.jpg")));
  }

  @Test
  void failedRollbackRemovalIsDurablyQueuedInAnotherTransaction() throws Exception {
    var photos = context.getBean(Photos.class);
    String[] photoId = new String[1];
    assertThrows(
        IllegalStateException.class,
        () ->
            transaction()
                .executeWithoutResult(
                    status -> {
                      try {
                        photoId[0] = photos.store(upload("rollback.jpg"));
                        Path original = photos.path(photoId[0], false);
                        Files.delete(original);
                        Files.createDirectory(original);
                        Files.writeString(original.resolve("akadály"), "retry");
                      } catch (IOException exception) {
                        throw new UncheckedIOException(exception);
                      }
                      throw new IllegalStateException("Kikényszerített rollback");
                    }));
    assertTrue(context.getBean(FileCleanupRepository.class).existsById(photoId[0]));
    Path original = photos.path(photoId[0], false);
    Files.delete(original.resolve("akadály"));
    Files.delete(original);
    photos.cleanup();
    assertFalse(context.getBean(FileCleanupRepository.class).existsById(photoId[0]));
    assertFalse(Files.exists(photos.path(photoId[0], true)));
  }

  @Test
  void concurrentSavesWithSameVersionHaveExactlyOneWinner() throws Exception {
    var draft = draft();
    saved(draft);
    draft.put("version", 1);
    race(() -> saveUnchecked(draft), () -> saveUnchecked(draft));
    assertEquals(2, get("/api/items/" + draft.get("id")).get("version").asLong());
  }

  @Test
  void concurrentAndRepeatedPhotoSaveWithSameOperationAppliesOnce() throws Exception {
    var draft = draft();
    saved(draft);
    draft.put("version", 1);
    String operation = UUID.randomUUID().toString();
    var photo = upload("retry.jpg");
    int callsBefore = imagingCalls.get();
    try (var workers = Executors.newFixedThreadPool(2)) {
      var start = new CountDownLatch(1);
      Callable<HttpResponse<byte[]>> upload =
          () -> {
            await(start);
            return saveWithOperation(draft, operation, photo);
          };
      var first = workers.submit(upload);
      var second = workers.submit(upload);
      start.countDown();
      assertStatus(200, first.get(15, TimeUnit.SECONDS));
      assertStatus(200, second.get(15, TimeUnit.SECONDS));
    }
    var replay = saveWithOperation(draft, operation, photo);
    assertStatus(200, replay);
    var item = json.readTree(replay.body());
    assertEquals(2, item.get("version").asLong());
    assertEquals(1, item.get("photos").size());
    assertStatus(409, saveWithOperation(draft, operation, upload("different.jpg")));
    assertEquals(callsBefore + 1, imagingCalls.get());
  }

  @Test
  void rolledBackPhotoSaveCanBeRetriedWithTheSameOperation() throws Exception {
    var draft = draft();
    String operation = UUID.randomUUID().toString();
    var photo = upload("retry-after-failure.jpg");
    imagingStatus.set(503);
    try {
      assertStatus(503, saveWithOperation(draft, operation, photo));
      assertFalse(containsId(get("/api/items"), draft.get("id").toString()));
    } finally {
      imagingStatus.set(200);
    }
    var retry = saveWithOperation(draft, operation, photo);
    assertStatus(200, retry);
    assertEquals(1, json.readTree(retry.body()).get("photos").size());
  }

  @Test
  void concurrentSaveSaleTrashAndMoveKeepVersionPreconditions() throws Exception {
    var sale = draft();
    sale.put("actionId", "SALE");
    sale.put("status", "LISTED");
    saved(sale);
    sale.put("version", 1);
    race(
        () -> saveUnchecked(sale),
        () ->
            postUnchecked(
                "/api/items/" + sale.get("id") + "/sold",
                Map.of("version", 1, "actual", 1, "soldDate", "2026-11-13")));
    var trash = draft();
    saved(trash);
    trash.put("version", 1);
    race(
        () -> saveUnchecked(trash),
        () -> postUnchecked("/api/items/" + trash.get("id") + "/trash?version=1", null));
    var move = draft();
    move.put("actionId", "MOVE");
    move.put("destination", createPlace());
    saved(move);
    move.put("version", 1);
    race(
        () -> saveUnchecked(move),
        () -> postUnchecked("/api/items/" + move.get("id") + "/move?version=1", null));
    var purge = draft();
    saved(purge);
    String id = purge.get("id").toString();
    assertStatus(200, post("/api/items/" + id + "/trash?version=1", null));
    race(
        () -> postUnchecked("/api/items/" + id + "/restore?version=2", null),
        () -> postUnchecked("/api/items/" + id + "/purge?version=2", null));
  }

  @Test
  void assignmentSharedLockBlocksActionDeactivation() throws Exception {
    String name = "Zárolás " + UUID.randomUUID();
    assertStatus(200, post("/api/actions", Map.of("name", name, "kind", "CUSTOM", "active", true)));
    String id = actionNamed(name).get("id").asText();
    var locked = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var reader =
          workers.submit(
              () ->
                  transaction()
                      .executeWithoutResult(
                          status -> {
                            assertTrue(
                                context
                                    .getBean(ActionRepository.class)
                                    .lockForAssignment(id)
                                    .isPresent());
                            locked.countDown();
                            await(release);
                          }));
      assertTrue(locked.await(5, TimeUnit.SECONDS));
      var writer =
          workers.submit(
              () ->
                  context
                      .getBean(Actions.class)
                      .save(new Actions.Choice(id, name, "CUSTOM", false)));
      try {
        assertThrows(TimeoutException.class, () -> writer.get(250, TimeUnit.MILLISECONDS));
      } finally {
        release.countDown();
      }
      reader.get(5, TimeUnit.SECONDS);
      writer.get(5, TimeUnit.SECONDS);
    } finally {
      release.countDown();
    }
    assertFalse(actionNamed(name).get("active").asBoolean());
  }

  @Test
  void listsAndDetailsHaveBoundedQueryCountsWithoutLazySerialization() throws Exception {
    for (int index = 0; index < 12; index++) saved(draft());
    var statistics =
        context.getBean(EntityManagerFactory.class).unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    var items = context.getBean(Ledger.class).items(false);
    assertTrue(items.size() >= 12);
    assertEquals(1, statistics.getPrepareStatementCount());
    statistics.clear();
    var detail = context.getBean(Ledger.class).item(UUID.fromString(legacyIds.getFirst()));
    assertEquals(2, statistics.getPrepareStatementCount());
    assertEquals(2, detail.photos().size());
    long reads = statistics.getPrepareStatementCount();
    json.writeValueAsString(items);
    json.writeValueAsString(detail);
    assertEquals(reads, statistics.getPrepareStatementCount());
  }

  static void race(Supplier<Integer> first, Supplier<Integer> second) throws Exception {
    var start = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var left =
          workers.submit(
              () -> {
                await(start);
                return first.get();
              });
      var right =
          workers.submit(
              () -> {
                await(start);
                return second.get();
              });
      start.countDown();
      var outcomes =
          new ArrayList<>(List.of(left.get(10, TimeUnit.SECONDS), right.get(10, TimeUnit.SECONDS)));
      Collections.sort(outcomes);
      assertEquals(List.of(200, 409), outcomes);
    }
  }

  static void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(5, TimeUnit.SECONDS));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }

  static int saveUnchecked(Map<String, Object> draft) {
    try {
      return save(draft).statusCode();
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  @Test
  void calendarUsesInclusiveEventDaysAndExclusiveQueryEnd() throws Exception {
    String item = saved(draft()).get("id").asText();
    var event = calendarDraft("MOVE", "2026-12-31", item);
    event.put("plannedEnd", "2027-01-02");
    var created = createCalendar(event);
    String id = created.get("id").asText();
    assertEquals("2027-01-02", created.get("plannedEnd").asText());
    assertTrue(containsId(calendarPeriod("2026-12-31", "2027-01-01", item), id));
    assertTrue(containsId(calendarPeriod("2027-01-01", "2027-01-02", item), id));
    assertTrue(containsId(calendarPeriod("2027-01-02", "2027-01-03", item), id));
    assertFalse(containsId(calendarPeriod("2027-01-03", "2027-01-04", item), id));
    assertFalse(containsId(calendarPeriod("2026-12-30", "2026-12-31", item), id));

    event.put("version", created.get("version").asLong());
    event.put("status", "DONE");
    event.put("actualStart", "2027-01-03");
    assertStatus(422, request("PUT", "/api/calendar/" + id, event));
    event.put("actualEnd", "2027-01-02");
    assertStatus(422, request("PUT", "/api/calendar/" + id, event));
    event.put("actualEnd", "2027-01-04");
    assertStatus(200, request("PUT", "/api/calendar/" + id, event));
    var completed = get("/api/calendar/" + id);
    assertEquals("2026-12-31", completed.get("plannedStart").asText());
    assertFalse(containsId(calendarPeriod("2027-01-01", "2027-01-03", item), id));
    assertTrue(containsId(calendarPeriod("2027-01-04", "2027-01-05", item), id));
    assertStatus(422, request("GET", "/api/calendar?from=2026-01-01&until=2027-01-03", null));
  }

  @Test
  void calendarWarnsOnOverlappingPlansAndKeepsOverdueEventsOpen() throws Exception {
    String item = saved(draft()).get("id").asText();
    String second = saved(draft()).get("id").asText();
    String originLocation = createPlace();
    var move = calendarDraft("MOVE", "2001-02-01", item);
    move.put("plannedEnd", "2001-02-03");
    move.put(
        "items",
        List.of(
            Map.of("itemId", item, "originLocationId", originLocation), Map.of("itemId", second)));
    var savedMove = createCalendar(move);
    String moveId = savedMove.get("id").asText();
    assertTrue(savedMove.get("overdue").asBoolean());
    assertEquals("PLANNED", savedMove.get("status").asText());
    var sale = createCalendar(calendarDraft("SALE", "2001-02-03", item));
    var warning = sale.get("warnings").get(0);
    assertEquals("SALE_MOVE_OVERLAP", warning.get("code").asText());
    assertEquals(moveId, warning.get("eventIds").get(0).asText());
    assertEquals(1, get("/api/calendar/" + moveId).get("warnings").size());
    assertEquals(
        0, createCalendar(calendarDraft("SALE", "2001-02-04", second)).get("warnings").size());
    var filtered =
        get(
            "/api/calendar?from=2001-02-01&until=2001-02-05&type=MOVE"
                + "&status=PLANNED&locationId="
                + originLocation
                + "&itemId="
                + item);
    assertEquals(1, filtered.size());
    assertEquals(moveId, filtered.get(0).get("id").asText());
    assertEquals(2, filtered.get(0).get("items").size());
  }

  @Test
  void calendarSaleReservationSurvivesParallelWritesAndIsReleasedOnCancellation() throws Exception {
    String item = saved(draft()).get("id").asText();
    var first = calendarDraft("SALE", "2027-03-01", item);
    var second = calendarDraft("SALE", "2027-03-03", item);
    var start = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var left =
          workers.submit(
              () -> {
                await(start);
                return postUnchecked("/api/calendar", first);
              });
      var right =
          workers.submit(
              () -> {
                await(start);
                return postUnchecked("/api/calendar", second);
              });
      start.countDown();
      assertEquals(
          List.of(201, 409),
          java.util.stream.Stream.of(
                  left.get(15, TimeUnit.SECONDS), right.get(15, TimeUnit.SECONDS))
              .sorted()
              .toList());
    }
    var listed = calendarPeriod("2027-03-01", "2027-03-05", item);
    assertEquals(1, listed.size());
    var winning = listed.get(0);
    var winningDraft = winning.get("id").asText().equals(first.get("id")) ? first : second;
    var losingDraft = winningDraft == first ? second : first;
    assertStatus(404, request("GET", "/api/calendar/" + losingDraft.get("id"), null));
    var duplicate = post("/api/calendar", losingDraft);
    assertStatus(409, duplicate);
    var conflicting = json.readTree(duplicate.body()).get("conflictingEvents").get(0);
    assertEquals(winning.get("id"), conflicting.get("id"));
    assertEquals(winning.get("title"), conflicting.get("title"));
    assertStatus(409, post("/api/calendar", winningDraft));
    winningDraft.put("version", winning.get("version").asLong());
    winningDraft.put("status", "CANCELLED");
    assertStatus(200, request("PUT", "/api/calendar/" + winningDraft.get("id"), winningDraft));
    assertStatus(201, post("/api/calendar", losingDraft));
    var cancelled = get("/api/calendar/" + winningDraft.get("id"));
    assertEquals("CANCELLED", cancelled.get("status").asText());
    assertEquals(winningDraft.get("plannedStart"), cancelled.get("plannedStart").asText());
  }

  @Test
  void calendarLocationOnlyChangeRevisionsAndMissingLinksAreHandled() throws Exception {
    String item = saved(draft()).get("id").asText();
    String other = saved(draft()).get("id").asText();
    String firstLocation = createPlace();
    String secondLocation = createPlace();
    var event = calendarDraft("MOVE", "2027-05-01", item);
    event.put("locationId", firstLocation);
    event.put(
        "items",
        List.of(
            Map.of("itemId", item, "originLocationId", firstLocation),
            Map.of("itemId", other, "originLocationId", firstLocation)));
    var baseline = createCalendar(event);
    String id = baseline.get("id").asText();
    event.put("version", baseline.get("version").asLong());
    event.put(
        "items",
        List.of(
            Map.of("itemId", item, "originLocationId", secondLocation),
            Map.of("itemId", other, "originLocationId", firstLocation)));
    assertStatus(200, request("PUT", "/api/calendar/" + id, event));
    var updated = get("/api/calendar/" + id);
    assertTrue(updated.get("version").asLong() > baseline.get("version").asLong());
    assertEquals(firstLocation, updated.get("locationId").asText());
    for (var link : updated.get("items")) {
      String expected = item.equals(link.get("itemId").asText()) ? secondLocation : firstLocation;
      assertEquals(expected, link.get("originLocationId").asText());
    }
    event.put("title", "Felülírási kísérlet");
    assertStatus(409, request("PUT", "/api/calendar/" + id, event));
    assertEquals(updated, get("/api/calendar/" + id));

    event.put("version", updated.get("version").asLong());
    event.put("items", List.of(Map.of("itemId", item), Map.of("itemId", item)));
    assertStatus(422, request("PUT", "/api/calendar/" + id, event));
    event.put("items", List.of(Map.of("itemId", UUID.randomUUID().toString())));
    assertStatus(422, request("PUT", "/api/calendar/" + id, event));
    event.put(
        "items", List.of(Map.of("itemId", item, "originLocationId", UUID.randomUUID().toString())));
    assertStatus(422, request("PUT", "/api/calendar/" + id, event));
    assertEquals(updated, get("/api/calendar/" + id));
  }

  @Test
  void calendarAllowsOnlyOneUpdateAtTheSameRevision() throws Exception {
    var first = calendarDraft("OTHER", "2027-06-01", null);
    var before = createCalendar(first);
    first.put("version", before.get("version").asLong());
    first.put("title", "Első változat");
    var second = new LinkedHashMap<>(first);
    second.put("title", "Második változat");
    String route = "/api/calendar/" + first.get("id");
    race(() -> putCalendarUnchecked(route, first), () -> putCalendarUnchecked(route, second));
    var after = get(route);
    assertEquals(before.get("version").asLong() + 1, after.get("version").asLong());
    assertTrue(
        List.of(first.get("title"), second.get("title")).contains(after.get("title").asText()));
  }

  static int putCalendarUnchecked(String route, Map<String, Object> draft) {
    try {
      return request("PUT", route, draft).statusCode();
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  @Test
  void calendarHistorySurvivesTrashAndPermanentDeletion() throws Exception {
    String item = saved(draft()).get("id").asText();
    String location = createPlace();
    var event = calendarDraft("OTHER", null, item);
    event.put("locationId", location);
    event.put("items", List.of(Map.of("itemId", item, "originLocationId", location)));
    event.put("status", "DONE");
    event.put("actualStart", "2024-02-29");
    String id = createCalendar(event).get("id").asText();
    assertStatus(200, post("/api/items/" + item + "/trash?version=1", null));
    assertEquals(1, get("/api/calendar/" + id).get("items").size());
    try (var db = connection();
        var sql = db.prepareStatement("delete from location where id=?")) {
      sql.setString(1, location);
      assertEquals(1, sql.executeUpdate());
    }
    var withoutLocation = get("/api/calendar/" + id);
    assertTrue(withoutLocation.get("locationId").isNull());
    assertTrue(withoutLocation.get("items").get(0).get("originLocationId").isNull());
    assertStatus(200, post("/api/items/" + item + "/purge?version=2", null));
    var history = get("/api/calendar/" + id);
    assertEquals(0, history.get("items").size());
    assertEquals("2024-02-29", history.get("actualStart").asText());
  }

  @Test
  void calendarUsesTheExistingSessionAndCsrfProtection() throws Exception {
    try (var anonymous = HttpClient.newHttpClient()) {
      var list =
          HttpRequest.newBuilder(
                  URI.create(origin + "/api/calendar?from=2027-01-01&until=2027-02-01"))
              .GET()
              .build();
      assertStatus(401, anonymous.send(list, HttpResponse.BodyHandlers.ofByteArray()));
      var write =
          HttpRequest.newBuilder(URI.create(origin + "/api/calendar"))
              .header("Content-Type", "application/json")
              .header("X-Otthontar-Request", "1")
              .POST(HttpRequest.BodyPublishers.ofString("{}"))
              .build();
      assertStatus(401, anonymous.send(write, HttpResponse.BodyHandlers.ofByteArray()));
    }
    var draft = calendarDraft("OTHER", "2027-01-01", null);
    var invalidToken =
        HttpRequest.newBuilder(URI.create(origin + "/api/calendar"))
            .header("Content-Type", "application/json")
            .header("X-Otthontar-Request", "1")
            .header("X-Otthontar-User", accountId)
            .header(csrfHeader, "invalid-csrf")
            .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(draft)))
            .build();
    assertStatus(403, http.send(invalidToken, HttpResponse.BodyHandlers.ofByteArray()));
    assertStatus(404, request("GET", "/api/calendar/" + draft.get("id"), null));
    assertStatus(201, post("/api/calendar", draft));
  }

  static Map<String, Object> calendarDraft(String type, String date, String itemId) {
    var draft = new LinkedHashMap<String, Object>();
    draft.put("id", UUID.randomUUID().toString());
    draft.put("title", "Esemény");
    draft.put("type", type);
    draft.put("status", "PLANNED");
    draft.put("plannedStart", date);
    if (itemId != null) draft.put("items", List.of(Map.of("itemId", itemId)));
    return draft;
  }

  static JsonNode createCalendar(Map<String, Object> draft) throws Exception {
    var saved = post("/api/calendar", draft);
    assertStatus(201, saved);
    return json.readTree(saved.body());
  }

  static JsonNode calendarPeriod(String from, String until, String item) throws Exception {
    return get("/api/calendar?from=" + from + "&until=" + until + "&itemId=" + item);
  }

  static int postUnchecked(String route, Object body) {
    try {
      return post(route, body).statusCode();
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  static Map<String, Object> draft() {
    var draft = new LinkedHashMap<String, Object>();
    draft.put("id", UUID.randomUUID().toString());
    draft.put("version", 0);
    draft.put("name", "Tárgy " + UUID.randomUUID());
    draft.put("description", "Leírás");
    draft.put("location", legacyPlace);
    draft.put("status", "KEEP");
    draft.put("actionId", "KEEP");
    return draft;
  }

  static String createPlace() throws Exception {
    String name = "Hely " + UUID.randomUUID();
    assertStatus(200, post("/api/places", Map.of("name", name)));
    return placeId(name);
  }

  static String placeId(String name) throws Exception {
    for (var place : get("/api/places"))
      if (name.equals(place.get("name").asText())) return place.get("id").asText();
    throw new AssertionError("Hiányzó helyszín: " + name);
  }

  static JsonNode actionNamed(String name) throws Exception {
    for (var action : get("/api/actions"))
      if (name.equals(action.get("name").asText())) return action;
    throw new AssertionError("Hiányzó művelet: " + name);
  }

  static boolean containsId(JsonNode rows, String id) {
    for (var row : rows) if (id.equals(row.get("id").asText())) return true;
    return false;
  }

  static JsonNode saved(Map<String, Object> draft, Upload... files) throws Exception {
    var response = save(draft, files);
    assertStatus(200, response);
    return json.readTree(response.body());
  }

  static HttpResponse<byte[]> save(Map<String, Object> draft, Upload... files) throws Exception {
    return saveWithOperation(draft, null, files);
  }

  static HttpResponse<byte[]> saveWithOperation(
      Map<String, Object> draft, String operation, Upload... files) throws Exception {
    String boundary = UUID.randomUUID().toString();
    var body = new ByteArrayOutputStream();
    body.write(
        ("--"
                + boundary
                + "\r\nContent-Disposition: form-data; name=\"draft\"\r\nContent-Type: application/json\r\n\r\n")
            .getBytes(StandardCharsets.UTF_8));
    body.write(json.writeValueAsBytes(draft));
    for (var file : files) {
      body.write(
          ("\r\n--"
                  + boundary
                  + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\""
                  + file.filename()
                  + "\"\r\nContent-Type: image/jpeg\r\n\r\n")
              .getBytes(StandardCharsets.UTF_8));
      body.write(file.bytes());
    }
    body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
    var request =
        HttpRequest.newBuilder(URI.create(origin + "/api/items"))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .header("X-Otthontar-Request", "1")
            .header(csrfHeader, csrfToken)
            .header("X-Otthontar-User", accountId);
    if (operation != null) request.header("X-Otthontar-Save-Id", operation);
    return http.send(
        request.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build(),
        HttpResponse.BodyHandlers.ofByteArray());
  }

  static JsonNode get(String route) throws Exception {
    var response = request("GET", route, null);
    assertStatus(200, response);
    return json.readTree(response.body());
  }

  static HttpResponse<byte[]> post(String route, Object body) throws Exception {
    return request("POST", route, body);
  }

  static HttpResponse<byte[]> request(String method, String route, Object body) throws Exception {
    var builder =
        HttpRequest.newBuilder(URI.create(origin + route)).timeout(Duration.ofSeconds(15));
    if (body != null) builder.header("Content-Type", "application/json");
    if (!method.equals("GET"))
      builder
          .header("X-Otthontar-Request", "1")
          .header(csrfHeader, csrfToken)
          .header("X-Otthontar-User", accountId);
    return http.send(
        builder
            .method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)))
            .build(),
        HttpResponse.BodyHandlers.ofByteArray());
  }

  static void assertStatus(int expected, HttpResponse<byte[]> response) {
    assertEquals(
        expected, response.statusCode(), () -> new String(response.body(), StandardCharsets.UTF_8));
  }

  static TransactionTemplate transaction() {
    return new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
  }

  static long fileCount() throws IOException {
    try (var paths = Files.list(photoRoot)) {
      return paths.count();
    }
  }

  static Connection connection() throws SQLException {
    return DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
  }

  static List<Map<String, Object>> rows(String query) throws SQLException {
    try (var connection = connection();
        var statement = connection.createStatement();
        var result = statement.executeQuery(query)) {
      var rows = new ArrayList<Map<String, Object>>();
      var metadata = result.getMetaData();
      while (result.next()) {
        var row = new LinkedHashMap<String, Object>();
        for (int column = 1; column <= metadata.getColumnCount(); column++) {
          Object value = result.getObject(column);
          if (value instanceof java.sql.Date date) value = date.toLocalDate().toString();
          row.put(metadata.getColumnLabel(column), value);
        }
        rows.add(row);
      }
      return rows;
    }
  }

  static Map<String, String> schema() throws SQLException {
    var schema = new LinkedHashMap<String, String>();
    try (var connection = connection();
        var statement = connection.createStatement()) {
      for (String table : List.of("location", "item", "photo", "item_action", "file_cleanup")) {
        try (var result = statement.executeQuery("show create table " + table)) {
          assertTrue(result.next());
          schema.put(table, result.getString(2));
        }
      }
    }
    return schema;
  }

  static Upload upload(String filename) {
    return new Upload(filename, jpeg);
  }

  record Upload(String filename, byte[] bytes) implements MultipartFile {
    public String getName() {
      return "files";
    }

    public String getOriginalFilename() {
      return filename;
    }

    public String getContentType() {
      return "image/jpeg";
    }

    public boolean isEmpty() {
      return bytes.length == 0;
    }

    public long getSize() {
      return bytes.length;
    }

    public byte[] getBytes() {
      return bytes;
    }

    public InputStream getInputStream() {
      return new ByteArrayInputStream(bytes);
    }

    public void transferTo(File destination) throws IOException {
      Files.write(destination.toPath(), bytes);
    }
  }
}
