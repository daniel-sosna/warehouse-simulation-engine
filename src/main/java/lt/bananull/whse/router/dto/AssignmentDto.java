package lt.bananull.whse.router.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AssignmentDto (
        @JsonProperty("shipment_id") String shipmentId,
        @JsonProperty("priority") int priority,
        @JsonProperty("packing_grid") String packingGrid,
        @JsonProperty("picks") List<PickDto> picks
) implements Comparable<AssignmentDto> {

    @Override
    public int compareTo(AssignmentDto other) {
        return Integer.compare(other.priority, this.priority);
    }
}
