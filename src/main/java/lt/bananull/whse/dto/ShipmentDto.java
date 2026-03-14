package lt.bananull.whse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

public record ShipmentDto(
        @JsonProperty("id") String id,
        @JsonProperty("items") Map<String, Integer> items,
        @JsonProperty("shipmentDate") Instant shipmentDate
) {}