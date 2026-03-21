package lt.bananull.whse.load.dto;

import java.util.List;

public record SimulationStateDto(
        List<BinDto> bins,
        List<GridDto> grids,
        List<ShipmentDto> shipments
) {}