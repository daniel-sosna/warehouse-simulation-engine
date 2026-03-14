package lt.bananull.whse.router;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RouterResponse(
        List<Assignment> assignments
) {
    public record Assignment(
            @JsonProperty("shipment_id") String shipmentId,
            int priority,
            @JsonProperty("packing_grid") String packingGrid,
            List<Pick> picks
    ) {
        public record Pick(
                String ean,
                @JsonProperty("bin_id") String binId,
                int qty
        ) {}
    }
}