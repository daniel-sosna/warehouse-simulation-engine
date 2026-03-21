package lt.bananull.whse.service;

import lt.bananull.whse.simulator.entity.Grid;
import lt.bananull.whse.simulator.entity.Shift;

import java.time.Instant;
import java.util.Comparator;

public class PortShiftService {

    public static Shift findCurrentOrNextShift(Grid grid, String portId, Instant now) {
        return grid.getShifts().stream()
            .filter(s -> s.getPortIds().contains(portId))
            .filter(s -> !s.getEndAt().isBefore(now)) // endAt >= now
            .min(Comparator.comparing(Shift::getStartAt))
            .orElse(null);
    }

    public static Shift findNextShiftAfter(Grid grid, String portId, Instant after) {
        return grid.getShifts().stream()
            .filter(s -> s.getPortIds().contains(portId))
            .filter(s -> s.getStartAt().isAfter(after)) // STRICT
            .min(Comparator.comparing(Shift::getStartAt))
            .orElse(null);
    }

}
