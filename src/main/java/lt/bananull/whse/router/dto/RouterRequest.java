package lt.bananull.whse.router.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lt.bananull.whse.load.dto.BinDto;
import lt.bananull.whse.load.dto.GridDto;
import lt.bananull.whse.load.dto.PortDto;
import lt.bananull.whse.load.dto.ShipmentDto;
import lt.bananull.whse.load.dto.ShiftDto;
import lt.bananull.whse.load.SimulationState;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record RouterRequest(
        @JsonProperty("state") State state
) {

    public static RouterRequest from(SimulationState simulationState) {
        ZoneId zone = ZoneId.of("UTC");

        LocalDate simulationDate = resolveSimulationDateFromShipments(simulationState, zone);
        Instant simulationNow = resolveSimulationNow(simulationState, simulationDate, zone);

        List<RouterShipment> shipmentsBacklog = mapShipments(simulationState.shipments());
        List<RouterStockBin> stockBins = mapBins(simulationState.bins());
        List<RouterGrid> grids = mapGrids(simulationState.grids(), simulationDate, zone);

        State routerState = new State(
                simulationNow,
                shipmentsBacklog,
                stockBins,
                grids
        );

        return new RouterRequest(routerState);
    }

    private record State(
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

    private static LocalDate resolveSimulationDateFromShipments(SimulationState state, ZoneId zone) {
        return state.shipments().stream()
                .map(ShipmentDto::shipmentDate)
                .min(Comparator.naturalOrder())
                .map(instant -> LocalDateTime.ofInstant(instant, zone).toLocalDate())
                .orElseGet(() -> LocalDate.now(zone));
    }

    private static Instant resolveSimulationNow(SimulationState state,
                                                LocalDate simulationDate,
                                                ZoneId zone) {
        return state.grids().stream()
                .flatMap(grid -> grid.shifts().stream())
                .map(ShiftDto::start)
                .min(Comparator.naturalOrder())
                .map(firstShiftStart ->
                        firstShiftStart
                                .atDate(simulationDate)
                                .atZone(zone)
                                .toInstant()
                )
                .orElseGet(() ->
                        LocalTime.MIDNIGHT
                                .atDate(simulationDate)
                                .atZone(zone)
                                .toInstant()
                );
    }

    private static List<RouterShipment> mapShipments(List<ShipmentDto> dtos) {
        return dtos.stream()
                .map(dto -> new RouterShipment(
                        dto.id(),
                        dto.shipmentDate(),
                        dto.items()
                ))
                .toList();
    }

    private static List<RouterStockBin> mapBins(List<BinDto> dtos) {
        return dtos.stream()
                .map(dto -> new RouterStockBin(
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

    private static List<RouterGrid> mapGrids(List<GridDto> dtos,
                                             LocalDate simulationDate,
                                             ZoneId zone) {
        return dtos.stream()
                .map(dto -> mapGrid(dto, simulationDate, zone))
                .toList();
    }

    private static RouterGrid mapGrid(GridDto dto,
                                      LocalDate simulationDate,
                                      ZoneId zone) {
        List<RouterShift> shifts = dto.shifts() == null ? List.of()
                : dto.shifts().stream()
                .map(shiftDto -> mapShift(shiftDto, simulationDate, zone))
                .toList();

        return new RouterGrid(dto.id(), shifts);
    }

    private static RouterShift mapShift(ShiftDto dto,
                                        LocalDate simulationDate,
                                        ZoneId zone) {
        List<RouterPortConfig> ports =
                dto.portConfig() == null ? List.of()
                        : dto.portConfig().stream()
                        .map(RouterRequest::mapPort)
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
}