package lt.bananull.whse.router.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Assignment(
        @JsonProperty("shipment_id") String shipmentId,
        @JsonProperty("priority") int priority,
        @JsonProperty("packing_grid") String packingGrid,
        @JsonProperty("picks") List<Pick> picks
) {
}
