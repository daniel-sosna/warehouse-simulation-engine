package lt.bananull.whse.router.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lt.bananull.whse.load.dto.BinDto;
import lt.bananull.whse.load.dto.GridDto;
import lt.bananull.whse.load.dto.PortDto;
import lt.bananull.whse.load.dto.ShipmentDto;
import lt.bananull.whse.load.dto.ShiftDto;
import lt.bananull.whse.load.dto.SimulationStateDto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record RouterRequestDto(
        @JsonProperty("state") RouterState state
) {

    public static RouterRequestDto from(SimulationStateDto simulationState, Instant simulationNow) {
        ZoneId zone = ZoneId.of("UTC");
        LocalDate simulationDate = LocalDate.ofInstant(simulationNow, zone);

        List<RouterShipment> shipmentsBacklog = mapShipments(simulationState.shipments());
        List<RouterStockBin> stockBins = mapBins(simulationState.bins());
        List<RouterGrid> grids = mapGrids(simulationState.grids(), simulationDate, zone);

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
}