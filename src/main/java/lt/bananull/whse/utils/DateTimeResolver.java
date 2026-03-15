package lt.bananull.whse.utils;

import lt.bananull.whse.load.dto.ShiftDto;
import lt.bananull.whse.load.dto.ShipmentDto;
import lt.bananull.whse.load.dto.SimulationStateDto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Comparator;

public class DateTimeResolver {

    private DateTimeResolver() {}

    public static LocalDate resolveSimulationDate(SimulationStateDto state, ZoneId zone) {
        return state.shipments().stream()
                .map(ShipmentDto::shipmentDate)
                .min(Comparator.naturalOrder())
                .map(instant -> instant.atZone(zone).toLocalDate())
                .orElseThrow(() -> new IllegalStateException("No shipments available to resolve simulation date"));
    }

    public static Instant resolveSimulationStart(SimulationStateDto state, ZoneId zone) {
        LocalDate date = resolveSimulationDate(state, zone);
        return resolveSimulationStartTime(state)
                .atDate(date)
                .atZone(zone)
                .toInstant();
    }

    public static Instant resolveSimulationEnd(SimulationStateDto state, ZoneId zone) {
        LocalDate date = resolveSimulationDate(state, zone);
        return resolveSimulationEndTime(state)
                .atDate(date)
                .atZone(zone)
                .toInstant();
    }

    private static LocalTime resolveSimulationStartTime(SimulationStateDto state) {
        return state.grids().stream()
                .flatMap(grid -> grid.shifts().stream())
                .map(ShiftDto::start)
                .min(Comparator.naturalOrder())
                .orElse(LocalTime.MIDNIGHT);
    }

    private static LocalTime resolveSimulationEndTime(SimulationStateDto state) {
        return state.grids().stream()
                .flatMap(grid -> grid.shifts().stream())
                .map(ShiftDto::end)
                .max(Comparator.naturalOrder())
                .orElse(LocalTime.parse("23:59"));
    }
}
