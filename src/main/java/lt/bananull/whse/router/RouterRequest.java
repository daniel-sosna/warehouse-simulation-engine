package lt.bananull.whse.router;

import lt.bananull.whse.dto.dataset.BinDto;
import lt.bananull.whse.dto.dataset.ShipmentDto;

import java.time.Instant;
import java.util.List;

public record RouterRequest(State state) {

    public record State(
            Instant now,
            List<ShipmentDto> shipments_backlog,
            List<BinDto> stock_bins,
            List<RouterGrid> grids
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