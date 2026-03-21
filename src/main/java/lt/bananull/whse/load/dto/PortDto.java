package lt.bananull.whse.load.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PortDto(
        @JsonProperty("portIndex") String portIndex,
        @JsonProperty("handlingFlags") List<String> handlingFlags
) {}
