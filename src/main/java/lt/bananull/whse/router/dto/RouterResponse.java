package lt.bananull.whse.router.dto;

import java.util.List;

public record RouterResponse(
        List<Assignment> assignments
) {
}