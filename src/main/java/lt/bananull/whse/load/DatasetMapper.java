package lt.bananull.whse.load;

import lt.bananull.whse.dto.dataset.*;
import lt.bananull.whse.models.Bin;
import lt.bananull.whse.models.Grid;
import lt.bananull.whse.models.Shipment;

import java.util.List;

public class DatasetMapper {

    public static Shipment toModel(ShipmentDto dto) {
        return new Shipment(
                dto.id(),
                dto.items(),
                dto.shipmentDate()
        );
    }

    public static Bin toModel(BinDto dto) {
        var items = dto.itemsInBin().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue().quantity() // pritaikyk pagal BinItemDto laukus
                ));

        return new Bin(
                dto.id(),
                dto.currentGridLocation(),
                items
        );
    }

    public static Grid toModel(GridDto dto) {
        List<Grid.Shift> shifts = dto.shifts().stream()
                .map(DatasetMapper::toModel)
                .toList();

        return new Grid(dto.id(), shifts);
    }

    private static Grid.Shift toModel(ShiftDto dto) {
        List<Grid.Port> ports = dto.portConfig().stream()
                .map(DatasetMapper::toModel)
                .toList();

        return new Grid.Shift(dto.start(), dto.end(), ports);
    }

    private static Grid.Port toModel(PortDto dto) {
        return new Grid.Port(dto.id(), dto.handlingFlags());
    }
}