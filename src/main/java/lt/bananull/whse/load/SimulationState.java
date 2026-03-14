package lt.bananull.whse.load;

import lt.bananull.whse.dto.dataset.BinDto;
import lt.bananull.whse.dto.dataset.GridDto;
import lt.bananull.whse.dto.dataset.ShipmentDto;

import java.util.List;

public record SimulationState(
        List<BinDto> bins,
        List<GridDto> grids,
        List<ShipmentDto> shipments
) {}