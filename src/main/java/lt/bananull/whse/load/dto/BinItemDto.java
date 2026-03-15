package lt.bananull.whse.load.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BinItemDto(
        @JsonProperty("quantity") int quantity
) {}