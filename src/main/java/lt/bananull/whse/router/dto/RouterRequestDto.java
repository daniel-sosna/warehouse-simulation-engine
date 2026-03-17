package lt.bananull.whse.router.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lt.bananull.whse.load.dto.PortDto;
import lt.bananull.whse.load.dto.ShiftDto;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Grid;
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
        ZoneId zone = ZoneId.of("UTC");
        LocalDate simulationDate = LocalDate.ofInstant(simulationNow, zone);

        List<Shipment> filteredShipments = filterShipmentsForRouting(simulationState.shipments().values());
        List<RouterShipment> shipmentsBacklog = mapShipments(filteredShipments);
        List<RouterStockBin> stockBins = mapBins(simulationState.bins().values());
        List<RouterGrid> grids = mapGrids(simulationState.grids().values(), simulationDate, zone);

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
            @JsonProperty("port_config") List<RouterPortConfig> portConfig
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

    private static List<RouterGrid> mapGrids(Collection<Grid> entities,
                                             LocalDate simulationDate,
                                             ZoneId zone) {
        return entities.stream()
                .map(entity -> {
                    List<RouterShift> shifts = entity.getShifts().stream()
                            .map(shiftDto -> mapShift(shiftDto, simulationDate, zone))
                            .toList();
                    return new RouterGrid(entity.getId(), shifts);
                })
                .toList();
    }

    private static RouterShift mapShift(ShiftDto dto,
                                        LocalDate simulationDate,
                                        ZoneId zone) {
        List<RouterPortConfig> ports =
                dto.portConfig() == null ? List.of()
                        : dto.portConfig().stream()
                        .map(RouterRequestDto::mapPort)
                        .toList();

        Instant start = dto.start()
                .atDate(simulationDate)
                .atZone(zone)
                .toInstant();

        Instant end = dto.end()
                .atDate(simulationDate)
                .atZone(zone)
                .toInstant();

        return new RouterShift(start, end, ports);
    }

    private static RouterPortConfig mapPort(PortDto dto) {
        return new RouterPortConfig(
                dto.id(),
                dto.handlingFlags()
        );
    }

    private static List<Shipment> filterShipmentsForRouting(Collection<Shipment> shipments) {
        return shipments.stream()
                .filter(shipment -> shipment.getStatus() == RECEIVED)
                .toList();
    }
}