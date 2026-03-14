package lt.bananull.whse.dto.dataset;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record BinDto(
        @JsonProperty("id") String id,
        @JsonProperty("currentGridLocation") String currentGridLocation,
        @JsonProperty("itemsInBin") Map<String, BinItemDto> itemsInBin
) {}