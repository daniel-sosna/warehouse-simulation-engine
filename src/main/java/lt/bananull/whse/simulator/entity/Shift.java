package lt.bananull.whse.simulator.entity;

import lombok.Getter;
import lt.bananull.whse.load.dto.BreakDto;
import lt.bananull.whse.load.dto.PortDto;
import lt.bananull.whse.load.dto.ShiftDto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A concrete, time-expanded shift occurrence for the simulator timeline.
 */
@Getter
public class Shift {

    private final Instant startAt;
    private final Instant endAt;

    /** Break windows expanded for this shift occurrence (Instant-based). */
    private final List<BreakOccurrence> breaks;

    /** Ports active during this shift occurrence (IDs only; actual Port entities live in Grid). */
    private final Set<String> portIds;

    private Shift(Instant startAt,
                  Instant endAt,
                  Collection<BreakOccurrence> breaks,
                  Collection<String> portIds) {

        if (startAt == null || endAt == null) throw new IllegalArgumentException("startAt/endAt must not be null");
        if (!endAt.isAfter(startAt)) throw new IllegalArgumentException("endAt must be after startAt");

        this.startAt = startAt;
        this.endAt = endAt;
        this.breaks = List.copyOf(breaks != null ? breaks : List.of());
        this.portIds = Set.copyOf(portIds != null ? portIds : Set.of());
    }

    /** Expand one ShiftDto template into daily occurrences in [startDate, endDate). */
    public static List<Shift> expandRecurring(ShiftDto dto,
                                              LocalDate startDate,
                                              LocalDate endDateExclusive,
                                              ZoneId zone) {
        if (dto == null) throw new IllegalArgumentException("ShiftDto must not be null");
        if (startDate == null || endDateExclusive == null || zone == null) {
            throw new IllegalArgumentException("startDate/endDateExclusive/zone must not be null");
        }
        if (!endDateExclusive.isAfter(startDate)) return List.of();

        List<Shift> result = new ArrayList<>();
        LocalDate date = startDate;
        while (date.isBefore(endDateExclusive)) {
            result.add(from(dto, date, zone));
            date = date.plusDays(1);
        }
        return List.copyOf(result);
    }

    public static Shift from(ShiftDto dto, LocalDate shiftDate, ZoneId zone) {
        Instant startAt = dto.start().atDate(shiftDate).atZone(zone).toInstant();

        boolean overnight = isOvernight(dto.start(), dto.end());
        LocalDate endDate = overnight ? shiftDate.plusDays(1) : shiftDate;
        Instant endAt = dto.end().atDate(endDate).atZone(zone).toInstant();

        Set<String> portIds =
            dto.portConfig() == null ? Set.of()
                : dto.portConfig().stream().map(PortDto::portIndex).collect(Collectors.toSet());

        List<BreakOccurrence> breaks = expandBreaks(dto.breaks(), shiftDate, zone);

        return new Shift(startAt, endAt, breaks, portIds);
    }

    public record BreakOccurrence(Instant startAt, Instant endAt) {
        public BreakOccurrence {
            if (startAt == null || endAt == null) {
                throw new IllegalArgumentException("BreakOccurrence startAt/endAt must not be null");
            }
            if (!endAt.isAfter(startAt)) {
                throw new IllegalArgumentException("BreakOccurrence endAt must be after startAt");
            }
        }
    }

    private static List<BreakOccurrence> expandBreaks(List<BreakDto> breakDtos,
                                                      LocalDate shiftDate,
                                                      ZoneId zone) {
        if (breakDtos == null || breakDtos.isEmpty()) return List.of();

        return breakDtos.stream()
            .map(b -> {
                boolean overnightBreak = isOvernight(b.start(), b.end());
                Instant start = b.start().atDate(shiftDate).atZone(zone).toInstant();
                LocalDate breakEndDate = overnightBreak ? shiftDate.plusDays(1) : shiftDate;
                Instant end = b.end().atDate(breakEndDate).atZone(zone).toInstant();
                return new BreakOccurrence(start, end);
            })
            .toList();
    }

    private static boolean isOvernight(LocalTime start, LocalTime end) {
        return end.isBefore(start);
    }

    public boolean isActiveAt(Instant now) {
        return (now.equals(startAt) || now.isAfter(startAt)) && now.isBefore(endAt);
    }

    public boolean appliesToPort(String portId) {
        return portIds.contains(portId);
    }
}
