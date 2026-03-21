package lt.bananull.whse.service;

import lt.bananull.whse.simulator.entity.Grid;
import lt.bananull.whse.simulator.entity.Shift;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public class PortShiftService {

    public static Shift findCurrentOrNextShift(Grid grid, String portId, Instant now) {
        return grid.getShifts().stream()
            .filter(s -> s.getPortIds().contains(portId))
            .filter(s -> s.getEndAt().isAfter(now)) // endAt > now
            .min(Comparator.comparing(Shift::getStartAt))
            .orElse(null);
    }

    public static Shift findNextShiftAfter(Grid grid, String portId, Instant after) {
        return grid.getShifts().stream()
            .filter(s -> s.getPortIds().contains(portId))
            .filter(s -> !s.getStartAt().isBefore(after)) // NOT STRICT
            .min(Comparator.comparing(Shift::getStartAt))
            .orElse(null);
    }

    public static Shift.BreakOccurrence findNextBreak(List<Shift.BreakOccurrence> breaks, Instant now) {
        return breaks.stream()
            .filter(b -> !b.startAt().isBefore(now))
            .min(Comparator.comparing(Shift.BreakOccurrence::startAt))
            .orElse(null);
    }

}
