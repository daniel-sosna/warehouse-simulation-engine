package lt.bananull.whse.router;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Assignment(
        @JsonProperty("shipment_id") String shipmentId,
        @JsonProperty("priority") int priority,
        @JsonProperty("packing_grid") String packingGrid,
        @JsonProperty("picks") List<Pick> picks
) {
    public record Pick(
            @JsonProperty("ean") String ean,
            @JsonProperty("bin_id") String binId,
            @JsonProperty("qty") int qty
    ) {}
}