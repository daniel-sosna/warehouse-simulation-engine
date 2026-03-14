package lt.bananull.whse.router.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Pick(
        @JsonProperty("ean") String ean,
        @JsonProperty("bin_id") String binId,
        @JsonProperty("qty") int qty
) {}
