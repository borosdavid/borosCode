package hu.boros.inventory;

import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
class Pages {
  private final long uploadLimitMiB;

  Pages(@Value("${spring.servlet.multipart.max-request-size:200MB}") DataSize uploadLimit) {
    uploadLimitMiB = uploadLimit.toMegabytes();
  }

  @GetMapping("/")
  String home() {
    return "redirect:/leltar";
  }

  @GetMapping("/offline.html")
  String offline() {
    return "offline";
  }

  @GetMapping("/leltar/naptar")
  String calendar() {
    return "calendar/index";
  }

  @GetMapping({"/leltar", "/leltar/{page}"})
  String inventory(Model model) {
    model.addAttribute("uploadLimitMiB", uploadLimitMiB);
    return "inventory/index";
  }
}

@RestController
@RequestMapping("/api")
public class Web {
  final Ledger ledger;
  final Photos photos;

  private final Actions actions;
  private final ItemSaves itemSaves;

  Web(Ledger ledger, Photos photos, Actions actions, ItemSaves itemSaves) {
    this.ledger = ledger;
    this.photos = photos;
    this.actions = actions;
    this.itemSaves = itemSaves;
  }

  @GetMapping("/actions")
  Object actions() {
    return actions.all();
  }

  @PostMapping("/actions")
  void actionSettings(@RequestBody Actions.Choice choice) {
    actions.save(choice);
  }

  @GetMapping("/places")
  Object places() {
    return ledger.places();
  }

  @PostMapping("/places")
  void place(@RequestBody Ledger.Place place) {
    ledger.place(place);
  }

  @GetMapping("/items")
  Object items(@RequestParam(defaultValue = "false") boolean trash) {
    return ledger.items(trash);
  }

  @GetMapping("/items/{id}")
  Object item(@PathVariable UUID id) {
    return ledger.item(id);
  }

  @PostMapping(value = "/items", consumes = "multipart/form-data")
  Object save(
      @RequestPart Ledger.Draft draft,
      @RequestPart(required = false) CalendarLocationChanges.Approval calendarApproval,
      @RequestPart(required = false) List<MultipartFile> files,
      @RequestHeader(name = "X-Otthontar-Save-Id", required = false) UUID operation,
      @AuthenticationPrincipal AccountSecurity.LoginUser user) {
    var uploads = files == null ? List.<MultipartFile>of() : files;
    if (operation != null) {
      if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
      return itemSaves.save(user.id, operation, draft, uploads, calendarApproval);
    }
    ledger.save(draft, uploads, calendarApproval);
    return ledger.item(draft.id());
  }

  @PostMapping("/items/{id}/sold")
  void completeSale(@PathVariable UUID id, @RequestBody Ledger.Sale sale) {
    ledger.completeSale(id, sale);
  }

  @PostMapping("/items/{id}/{action}")
  void action(@PathVariable UUID id, @PathVariable String action, @RequestParam long version) {
    ledger.transition(id, version, action);
    if (action.equals("purge")) photos.cleanup();
  }

  @GetMapping(value = "/photos/{id}/{size}", produces = "image/jpeg")
  FileSystemResource photo(@PathVariable UUID id, @PathVariable String size) {
    var path = photos.path(id.toString(), size.equals("small"));
    if (!Set.of("small", "original").contains(size)
        || !Files.exists(path)
        || !photos.exists(id.toString())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return new FileSystemResource(path);
  }

  @ExceptionHandler(OptimisticLockingFailureException.class)
  ProblemDetail staleCalendar() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        "Az érintett naptáresemény időközben megváltozott. Ments újra a friss adatokkal.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> invalid(DataIntegrityViolationException ex) {
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "detail",
                "A név vagy azonosító már foglalt, vagy a hivatkozott adat időközben megváltozott."));
  }
}
