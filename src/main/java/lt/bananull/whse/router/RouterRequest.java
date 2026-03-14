package lt.bananull.whse.router;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RouterRequest(State state) {

    public record State(
            Instant now,
            List<RouterShipment> shipments_backlog,
            List<RouterStockBin> stock_bins,
            List<RouterGrid> grids
    ) {}

    public record RouterShipment(
            String id,
            Instant created_at,
            Map<String, Integer> items
    ) {}

    public record RouterStockBin(
            String bin_id,
            String grid_id,
            Map<String, Integer> items
    ) {}

    public record RouterGrid(
            String id,
            List<RouterShift> shifts
    ) {}

    public record RouterShift(
            Instant start_at,
            Instant end_at,
            List<RouterPortConfig> port_config
    ) {}

    public record RouterPortConfig(
            String port_id,
            List<String> handling_flags
    ) {}
}