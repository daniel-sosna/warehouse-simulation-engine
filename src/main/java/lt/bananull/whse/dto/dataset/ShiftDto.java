package lt.bananull.whse.dto.dataset;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;
import java.util.List;

public record ShiftDto(
        @JsonProperty("start") LocalTime start,          // "07:00"
        @JsonProperty("end") LocalTime end,              // "10:00"
        @JsonProperty("portConfig") List<PortDto> portConfig
) {}
