package lt.bananull.whse.load.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;

public record BreakDto(
        @JsonProperty("start") LocalTime start,
        @JsonProperty("end") LocalTime end
) {}
