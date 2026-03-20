package lt.bananull.whse.router.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Grid;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shift;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static lt.bananull.whse.simulator.enums.ShipmentStatus.RECEIVED;

public record RouterRequestDto(
        @JsonProperty("state") RouterState state
) {

    public static RouterRequestDto from(SimulationState simulationState, Instant simulationNow) {

        List<Shipment> filteredShipments = filterShipmentsForRouting(simulationState.shipments().values());
        List<RouterShipment> shipmentsBacklog = mapShipments(filteredShipments);
        List<RouterStockBin> stockBins = mapBins(simulationState.bins().values());
        List<RouterGrid> grids = mapGrids(simulationState.grids().values());

        RouterState routerState = new RouterState(
                simulationNow,
                shipmentsBacklog,
                stockBins,
                grids
        );

        return new RouterRequestDto(routerState);
    }

    private record RouterState(
            @JsonProperty("now") Instant now,
            @JsonProperty("shipments_backlog") List<RouterShipment> shipmentsBacklog,
            @JsonProperty("stock_bins") List<RouterStockBin> stockBins,
            @JsonProperty("grids") List<RouterGrid> grids
    ) {}

    private record RouterShipment(
            @JsonProperty("id") String id,
            @JsonProperty("created_at") Instant createdAt,
            @JsonProperty("items") Map<String, Integer> items
    ) {}

    private record RouterStockBin(
            @JsonProperty("bin_id") String binId,
            @JsonProperty("grid_id") String gridId,
            @JsonProperty("items") Map<String, Integer> items
    ) {}

    private record RouterGrid(
            @JsonProperty("id") String id,
            @JsonProperty("shifts") List<RouterShift> shifts
    ) {}

    private record RouterShift(
            @JsonProperty("start_at") Instant startAt,
            @JsonProperty("end_at") Instant endAt,
            @JsonProperty("break_schedules") List<RouterBreak> breakSchedules,
            @JsonProperty("port_config") List<RouterPortConfig> portConfig
    ) {}

    private record RouterBreak(
            @JsonProperty("start_at") Instant startAt,
            @JsonProperty("end_at") Instant endAt
    ) {}

    private record RouterPortConfig(
            @JsonProperty("port_id") String portId,
            @JsonProperty("handling_flags") List<String> handlingFlags
    ) {}

    private static List<RouterShipment> mapShipments(Collection<Shipment> entities) {
        return entities.stream()
                .map(entity -> new RouterShipment(
                        entity.getId(),
                        entity.getShipmentDate(),
                        entity.getItems()
                ))
                .toList();
    }

    private static List<RouterStockBin> mapBins(Collection<Bin> entities) {
        return entities.stream()
                .map(entity -> new RouterStockBin(
                        entity.getId(),
                        entity.getCurrentGridId(), // TODO: find out whether it might be null or should we store `destinationGrid`
                        entity.getAvailableStock()
                ))
                .toList();
    }

    private static List<RouterGrid> mapGrids(Collection<Grid> entities) {
        return entities.stream()
            .map(grid -> new RouterGrid(grid.getId(), mapShifts(grid)))
            .toList();
    }

    private static List<RouterShift> mapShifts(Grid grid) {
        return grid.getShifts().stream()
            .map(shift -> new RouterShift(
                shift.getStartAt(),
                shift.getEndAt(),
                mapBreaks(shift),
                mapPorts(grid, shift)
            ))
            .toList();
    }

    private static List<RouterBreak> mapBreaks(Shift shift) {
        return shift.getBreaks().stream()
            .map(b -> new RouterBreak(b.startAt(), b.endAt()))
            .toList();
    }

    private static List<RouterPortConfig> mapPorts(Grid grid, Shift shift) {
        return shift.getPortIds().stream()
            .map(portId -> mapPort(grid, portId))
            .toList();
    }

    private static RouterPortConfig mapPort(Grid grid, String portId) {
        Port port = grid.getPorts().get(portId);
        if (port == null) {
            throw new IllegalStateException(
                "Shift references port %s but grid %s does not contain that port"
                    .formatted(portId, grid.getId()));
        }

        return new RouterPortConfig(
            port.getId(),
            port.getHandlingFlags().stream().toList()
        );
    }
    private static List<Shipment> filterShipmentsForRouting(Collection<Shipment> shipments) {
        return shipments.stream()
                .filter(shipment -> shipment.getStatus() == RECEIVED)
                .toList();
    }
}
