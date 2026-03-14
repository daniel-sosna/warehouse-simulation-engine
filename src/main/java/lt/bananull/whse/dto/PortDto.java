package lt.bananull.whse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PortDto(
        @JsonProperty("id") String id,
        @JsonProperty("handlingFlags") List<String> handlingFlags
) {}