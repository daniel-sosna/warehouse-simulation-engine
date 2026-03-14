package lt.bananull.whse.load;

import lt.bananull.whse.load.dto.BinDto;
import lt.bananull.whse.load.dto.GridDto;
import lt.bananull.whse.load.dto.ShipmentDto;

import java.util.List;

public record SimulationState(
        List<BinDto> bins,
        List<GridDto> grids,
        List<ShipmentDto> shipments
) {}