package lt.bananull.whse.router;

import lt.bananull.whse.dto.dataset.BinDto;
import lt.bananull.whse.dto.dataset.GridDto;
import lt.bananull.whse.dto.dataset.ShipmentDto;
import lt.bananull.whse.dto.dataset.ShiftDto;
import lt.bananull.whse.dto.dataset.PortDto;
import lt.bananull.whse.load.SimulationState;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RouterRequestBuilder {

    public RouterRequest build(SimulationState state) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        ZoneId zone = ZoneId.of("UTC");
        LocalDate simulationDate = resolveSimulationDateFromShipments(state, zone);

        List<RouterRequest.RouterShipment> shipmentsBacklog = mapShipments(state.shipments());
        List<RouterRequest.RouterStockBin> stockBins = mapBins(state.bins());
        List<RouterRequest.RouterGrid> grids = mapGrids(state.grids(), simulationDate, zone);

        RouterRequest.State routerState = new RouterRequest.State(
                now,
                shipmentsBacklog,
                stockBins,
                grids
        );

        return new RouterRequest(routerState);
    }

    private LocalDate resolveSimulationDateFromShipments(SimulationState state, ZoneId zone) {
        return state.shipments().stream()
                .map(ShipmentDto::shipmentDate)
                .min(Comparator.naturalOrder())
                .map(instant -> LocalDateTime.ofInstant(instant, zone).toLocalDate())
                .orElseGet(() -> LocalDate.now(zone));
    }

    private List<RouterRequest.RouterShipment> mapShipments(List<ShipmentDto> dtos) {
        return dtos.stream()
                .map(dto -> new RouterRequest.RouterShipment(
                        dto.id(),
                        dto.shipmentDate(),
                        dto.items()
                ))
                .toList();
    }

    private List<RouterRequest.RouterStockBin> mapBins(List<BinDto> dtos) {
        return dtos.stream()
                .map(dto -> new RouterRequest.RouterStockBin(
                        dto.id(),
                        dto.currentGridLocation(),
                        dto.itemsInBin().entrySet().stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        e -> e.getValue().quantity()
                                ))
                ))
                .toList();
    }

    private List<RouterRequest.RouterGrid> mapGrids(List<GridDto> dtos,
                                                    LocalDate simulationDate,
                                                    ZoneId zone) {
        return dtos.stream()
                .map(dto -> mapGrid(dto, simulationDate, zone))
                .toList();
    }

    private RouterRequest.RouterGrid mapGrid(GridDto dto,
                                             LocalDate simulationDate,
                                             ZoneId zone) {
        List<RouterRequest.RouterShift> shifts = dto.shifts().stream()
                .map(shiftDto -> mapShift(shiftDto, simulationDate, zone))
                .toList();

        return new RouterRequest.RouterGrid(dto.id(), shifts);
    }

    private RouterRequest.RouterShift mapShift(ShiftDto dto,
                                               LocalDate simulationDate,
                                               ZoneId zone) {
        List<RouterRequest.RouterPortConfig> ports =
                dto.portConfig() == null ? List.of() :
                        dto.portConfig().stream()
                                .map(this::mapPort)
                                .toList();

        Instant start = dto.start()
                .atDate(simulationDate)
                .atZone(zone)
                .toInstant();

        Instant end = dto.end()
                .atDate(simulationDate)
                .atZone(zone)
                .toInstant();

        return new RouterRequest.RouterShift(
                start,
                end,
                ports
        );
    }

    private RouterRequest.RouterPortConfig mapPort(PortDto dto) {
        return new RouterRequest.RouterPortConfig(
                dto.id(),
                dto.handlingFlags()
        );
    }
}