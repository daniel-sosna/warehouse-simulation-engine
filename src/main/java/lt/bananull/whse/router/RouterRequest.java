package lt.bananull.whse.router;

import lt.bananull.whse.dto.dataset.BinDto;
import lt.bananull.whse.dto.dataset.GridDto;
import lt.bananull.whse.dto.dataset.ShiftDto;
import lt.bananull.whse.dto.dataset.ShipmentDto;
import lt.bananull.whse.load.SimulationState;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

public record RouterRequest(State state) {

    public static RouterRequest from(SimulationState state) {
        // TODO should be our simulation time
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        ZoneId zone = ZoneId.of("UTC");
        LocalDate simulationDate = resolveSimulationDateFromShipments(state, zone);
        List<RouterGrid> grids = mapGrids(state.grids(), simulationDate, zone);
        State routerState = new State(
                now,
                state.shipments(),
                state.bins(),
                grids
        );

        return new RouterRequest(routerState);
    }

    public record State(
            Instant now,
            List<ShipmentDto> shipments_backlog,
            List<BinDto> stock_bins,
            List<RouterGrid> grids
    ) {}

    public record RouterGrid(
            String id,
            List<RouterShift> shifts
    ) {}

    public record RouterShift(
            Instant start_at,
            Instant end_at,
            List<RouterPortConfig> port_config
    ) {}

    public record RouterPortConfig(
            String port_id,
            List<String> handling_flags
    ) {}

    private static LocalDate resolveSimulationDateFromShipments(SimulationState state, ZoneId zone) {
        return state.shipments().stream()
                .map(ShipmentDto::shipmentDate)
                .min(Comparator.naturalOrder())
                .map(instant -> LocalDateTime.ofInstant(instant, zone).toLocalDate())
                .orElseGet(() -> LocalDate.now(zone));
    }

    private static List<RouterGrid> mapGrids(List<GridDto> dtos, LocalDate simulationDate, ZoneId zone) {
        return dtos.stream()
                .map(dto -> mapGrid(dto, simulationDate, zone))
                .toList();
    }

    private static RouterGrid mapGrid(GridDto dto, LocalDate simulationDate, ZoneId zone) {
        List<RouterShift> shifts = dto.shifts().stream()
                .map(shiftDto -> mapShift(shiftDto, simulationDate, zone))
                .toList();

        return new RouterGrid(dto.id(), shifts);
    }

    private static RouterShift mapShift(ShiftDto dto, LocalDate simulationDate, ZoneId zone) {
        List<RouterPortConfig> ports =
                dto.portConfig() == null ? List.of() :
                        dto.portConfig().stream()
                                .map(portDto -> new RouterPortConfig(portDto.id(), portDto.handlingFlags()))
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
}