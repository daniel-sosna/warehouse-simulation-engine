package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.service.PortShiftService;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shift;
import lt.bananull.whse.utils.DateTimeResolver;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class PortOpensEvent extends Event {

    private final String gridId;
    private final String portId;
    private final boolean fromBreak;
    private Port port;

    public PortOpensEvent(long simTime, String gridId, String portId, boolean fromBreak) {
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
        this.fromBreak = fromBreak;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        port = simulator.getState().getPort(portId);
        port.open();

        Instant now = simulator.getSimulationStart().plusSeconds(getSimTime());
        Shift shift = PortShiftService.findCurrentOrNextShift(simulator.getState().getGrid(gridId), portId, now);
        if (shift == null) return List.of(); // no more shifts left

        if (!fromBreak) {
            long closeAt = DateTimeResolver.resolveSimTimeFromTimestamp(shift.getEndAt(),
                simulator.getParameters().simulationStartTime());
            Event next = new PortClosesEvent(closeAt, shift.getEndAt(), gridId, portId, null);
            simulator.enqueueEvent(next);
        }

        // Enqueue next break event
        Shift.BreakOccurrence portBreak = PortShiftService.findNextBreak(shift.getBreaks(), now);

        if (portBreak != null) {
            long closeForBreakAt = DateTimeResolver.resolveSimTimeFromTimestamp(portBreak.startAt(),
                simulator.getParameters().simulationStartTime());
            simulator.enqueueEvent(new PortClosesEvent(closeForBreakAt, null, gridId, portId, portBreak));
        }

        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "gridId", gridId,
            "portId", portId,
            "fromBreak", fromBreak,
            "handlingFlags", port.getHandlingFlags()
        );
    }
}
