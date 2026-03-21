package lt.bananull.whse.load.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ShipmentDto(
        @JsonProperty("id") String id,
        @JsonProperty("items") Map<String, Integer> items,
        @JsonProperty("shipmentDate") Instant shipmentDate,
        @JsonProperty("handlingFlags") List<String> handlingFlags,
        @JsonProperty("sortingDirection") String sortingDirection
) {}
