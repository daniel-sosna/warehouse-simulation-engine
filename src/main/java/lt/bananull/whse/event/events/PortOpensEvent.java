package lt.bananull.whse.event.events;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Grid;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shift;
import lt.bananull.whse.utils.DateTimeResolver;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
@Slf4j
public class PortOpensEvent extends Event {

    private final String gridId;
    private final String portId;

    public PortOpensEvent(long simTime, String gridId, String portId) {
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
    }

    @Override
    public void execute(Simulator simulator) {
        Instant now = simulator.getNow();
        Shift shift = findCurrentOrNextShift(simulator.getState().getGrid(gridId), portId, now);
        if (shift == null) return; // no more shifts left
        if (now.isBefore(shift.getStartAt())) {
            simulator.enqueueEvent(new PortOpensEvent(DateTimeResolver.resolveSimTimeFromTimestamp(shift.getStartAt()
                , simulator.getParameters().simulationStartTime()), gridId, portId));
            return;
        }
        Port port = simulator.getState().getPort(gridId, portId);
        port.open();
        Event next = new PortClosesEvent(DateTimeResolver.resolveSimTimeFromTimestamp(shift.getEndAt(),
            simulator.getParameters().simulationStartTime()), shift.getEndAt(), gridId, portId);
        simulator.enqueueEvent(next);
    }

    private static Shift findCurrentOrNextShift(Grid grid, String portId, Instant now) {
        return grid.getShifts().stream()
            .filter(s -> s.getPortIds().contains(portId))
            .filter(s -> !s.getEndAt().isBefore(now)) // endAt >= now
            .min(Comparator.comparing(Shift::getStartAt))
            .orElse(null);
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "gridId", gridId,
            "portId", portId
        );
    }
}
