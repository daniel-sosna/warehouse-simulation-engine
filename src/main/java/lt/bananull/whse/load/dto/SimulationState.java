package lt.bananull.whse.load.dto;

import java.util.List;

public record SimulationState(
        List<BinDto> bins,
        List<GridDto> grids,
        List<ShipmentDto> shipments
) {}