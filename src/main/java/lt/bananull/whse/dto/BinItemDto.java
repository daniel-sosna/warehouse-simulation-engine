package lt.bananull.whse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BinItemDto(
        @JsonProperty("quantity") int quantity
) {}