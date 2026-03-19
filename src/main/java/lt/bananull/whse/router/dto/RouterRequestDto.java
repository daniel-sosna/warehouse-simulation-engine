package lt.bananull.whse.router.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lt.bananull.whse.load.dto.BreakDto;
import lt.bananull.whse.load.dto.PortDto;
import lt.bananull.whse.load.dto.ShiftDto;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Grid;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static lt.bananull.whse.simulator.enums.ShipmentStatus.RECEIVED;

public record RouterRequestDto(
        @JsonProperty("state") RouterState state
) {

    public static RouterRequestDto from(SimulationState simulationState, Instant simulationNow,
                                        Instant simulationStartTime, Instant simulationEndTime) {
        ZoneId zone = ZoneId.of("UTC");
        LocalDate startDate = LocalDate.ofInstant(simulationStartTime, zone);
        LocalDate endDate = LocalDate.ofInstant(simulationEndTime, zone);

        List<Shipment> filteredShipments = filterShipmentsForRouting(simulationState.shipments().values());
        List<RouterShipment> shipmentsBacklog = mapShipments(filteredShipments);
        List<RouterStockBin> stockBins = mapBins(simulationState.bins().values());
        List<RouterGrid> grids = mapGrids(simulationState.grids().values(), startDate, endDate, zone);

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

    private static List<RouterGrid> mapGrids(Collection<Grid> entities,
                                             LocalDate startDate,
                                             LocalDate endDate,
                                             ZoneId zone) {
        return entities.stream()
                .map(entity -> {
                    List<RouterShift> shifts = entity.getShifts().stream()
                            .flatMap(shiftDto -> expandShift(shiftDto, startDate, endDate, zone).stream())
                            .toList();
                    return new RouterGrid(entity.getId(), shifts);
                })
                .toList();
    }

    private static List<RouterShift> expandShift(ShiftDto dto,
                                                  LocalDate startDate,
                                                  LocalDate endDate,
                                                  ZoneId zone) {
        List<RouterPortConfig> ports =
                dto.portConfig() == null ? List.of()
                        : dto.portConfig().stream()
                        .map(RouterRequestDto::mapPort)
                        .toList();

        boolean overnightShift = isOvernight(dto.start(), dto.end());

        List<RouterShift> result = new ArrayList<>();
        LocalDate date = startDate;
        while (!date.isAfter(endDate)) {
            final LocalDate currentDate = date;
            Instant start = dto.start().atDate(currentDate).atZone(zone).toInstant();
            LocalDate shiftEndDate = overnightShift ? currentDate.plusDays(1) : currentDate;
            Instant end = dto.end().atDate(shiftEndDate).atZone(zone).toInstant();

            List<RouterBreak> breakSchedules =
                    dto.breaks() == null ? List.of()
                            : dto.breaks().stream()
                            .map(b -> mapBreak(b, currentDate, zone))
                            .toList();

            result.add(new RouterShift(start, end, breakSchedules, ports));
            date = date.plusDays(1);
        }
        return result;
    }

    private static RouterBreak mapBreak(BreakDto dto, LocalDate shiftDate, ZoneId zone) {
        boolean overnightBreak = isOvernight(dto.start(), dto.end());
        Instant start = dto.start().atDate(shiftDate).atZone(zone).toInstant();
        LocalDate breakEndDate = overnightBreak ? shiftDate.plusDays(1) : shiftDate;
        Instant end = dto.end().atDate(breakEndDate).atZone(zone).toInstant();
        return new RouterBreak(start, end);
    }

    private static boolean isOvernight(LocalTime start, LocalTime end) {
        return end.isBefore(start);
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
