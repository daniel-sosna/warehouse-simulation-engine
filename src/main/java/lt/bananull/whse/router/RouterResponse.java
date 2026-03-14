package lt.bananull.whse.router;

import java.util.List;

public record RouterResponse(
        List<Assignment> assignments
) {
}