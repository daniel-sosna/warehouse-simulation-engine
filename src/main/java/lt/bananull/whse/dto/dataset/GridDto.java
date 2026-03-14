package lt.bananull.whse.dto.dataset;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GridDto(
        @JsonProperty("id") String id,
        @JsonProperty("shifts") List<ShiftDto> shifts
) {}