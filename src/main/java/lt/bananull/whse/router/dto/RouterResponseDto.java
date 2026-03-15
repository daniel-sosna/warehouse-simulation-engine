package lt.bananull.whse.router.dto;

import java.util.List;

public record RouterResponseDto(
        List<AssignmentDto> assignments
) {
}