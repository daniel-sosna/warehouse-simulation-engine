package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.service.PortShiftService;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shift;
import lt.bananull.whse.utils.DateTimeResolver;

import java.time.Instant;
import java.util.Map;

public class PortOpensEvent extends Event {

    private final String gridId;
    private final String portId;
    private final boolean fromBreak;

    public PortOpensEvent(long simTime, String gridId, String portId, boolean fromBreak) {
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
        this.fromBreak = fromBreak;
    }

    @Override
    public void execute(Simulator simulator) {
        Instant now = simulator.getNow();
        Shift shift = PortShiftService.findCurrentOrNextShift(simulator.getState().getGrid(gridId), portId, now);

        if (shift == null) return; // no more shifts left

        Port port = simulator.getState().getPort(gridId, portId);
        switch (port.getStatus()) {
            case CLOSED -> port.open();
            case PENDING_CLOSE -> port.reopenIfPendingClose(); // guard against pending close
        }

        if (!fromBreak) {
            long closeAt = DateTimeResolver.resolveSimTimeFromTimestamp(shift.getEndAt(),
                simulator.getParameters().simulationStartTime());
            Event next = new PortClosesEvent(closeAt, shift.getEndAt(), gridId, portId, null);
            simulator.enqueueEvent(next);
        }

        // Enqueue next break event
        Shift.BreakOccurrence portBreak = PortShiftService.findNextBreak(shift.getBreaks(), now);

        if (portBreak == null) {
            return;
        }

        long closeForBreakAt = DateTimeResolver.resolveSimTimeFromTimestamp(portBreak.startAt(),
            simulator.getParameters().simulationStartTime());
        simulator.enqueueEvent(new PortClosesEvent(closeForBreakAt, null, gridId, portId, portBreak));
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "gridId", gridId,
            "portId", portId
        );
    }
}
