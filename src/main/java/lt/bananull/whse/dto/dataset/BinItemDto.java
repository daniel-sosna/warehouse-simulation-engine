package lt.bananull.whse.dto.dataset;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BinItemDto(
        @JsonProperty("quantity") int quantity
) {}