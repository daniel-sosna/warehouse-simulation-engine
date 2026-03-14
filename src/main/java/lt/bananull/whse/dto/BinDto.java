package lt.bananull.whse.dto.null.whse.dto;

public record BinDto(
        @JsonProperty("id") String id,
        @JsonProperty("currentGridLocation") String gridId,
        @JsonProperty("itemsInBin") Map<String, BinItemDto> items
) {}