package hu.boros.inventory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

final class InventoryViews {
  record PlaceView(String id, String name) {}

  record PhotoView(String id, String filename) {}

  record ItemView(
      String id,
      long version,
      String name,
      String description,
      @JsonProperty("location_id") String locationId,
      String status,
      BigDecimal asking,
      String link,
      BigDecimal actual,
      @JsonProperty("sold_date") LocalDate soldDate,
      String cover,
      boolean trashed,
      @JsonProperty("action_id") String actionId,
      @JsonProperty("destination_id") String destinationId,
      @JsonInclude(JsonInclude.Include.NON_NULL) String place,
      @JsonInclude(JsonInclude.Include.NON_NULL) List<PhotoView> photos) {}

  static ItemView item(ItemEntity item, String place, List<PhotoView> photos) {
    return new ItemView(
        item.id,
        item.version,
        item.name,
        item.description,
        item.locationId,
        item.status,
        item.asking,
        item.link,
        item.actual,
        item.soldDate,
        item.cover,
        item.trashed,
        item.actionId,
        item.destinationId,
        place,
        photos);
  }

  private InventoryViews() {}
}
