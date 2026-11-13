package hu.boros.inventory;

import java.nio.file.*;
import java.util.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
class Pages {
  @GetMapping("/")
  String home() {
    return "redirect:/leltar";
  }

  @GetMapping({"/leltar", "/leltar/{page}"})
  String inventory() {
    return "inventory/index";
  }
}

@RestController
@RequestMapping("/api")
public class Web {
  final Ledger ledger;
  final Photos photos;

  private final Actions actions;

  Web(Ledger ledger, Photos photos, Actions actions) {
    this.ledger = ledger;
    this.photos = photos;
    this.actions = actions;
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
      @RequestPart Ledger.Draft draft, @RequestPart(required = false) List<MultipartFile> files) {
    ledger.save(draft, files == null ? List.of() : files);
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
        || photos.db.queryForObject(
                "SELECT COUNT(*) FROM photo WHERE id=?", Integer.class, id.toString())
            == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return new FileSystemResource(path);
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
