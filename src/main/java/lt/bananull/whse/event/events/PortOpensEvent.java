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

    public PortOpensEvent(long simTime, String gridId, String portId) {
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
    }

    @Override
    public void execute(Simulator simulator) {
        Instant now = simulator.getNow();
        Shift shift = PortShiftService.findCurrentOrNextShift(simulator.getState().getGrid(gridId), portId, now);
        if (shift == null) return; // no more shifts left
        if (now.isBefore(shift.getStartAt())) {
            simulator.enqueueEvent(new PortOpensEvent(DateTimeResolver.resolveSimTimeFromTimestamp(shift.getStartAt()
                , simulator.getParameters().simulationStartTime()), gridId, portId));
            return;
        }
        Port port = simulator.getState().getPort(gridId, portId);
        switch (port.getStatus()) {
            case CLOSED -> port.open();
            case PENDING_CLOSE -> port.reopenIfPendingClose(); // guard against pending close
        }
        Event next = new PortClosesEvent(DateTimeResolver.resolveSimTimeFromTimestamp(shift.getEndAt(),
            simulator.getParameters().simulationStartTime()), shift.getEndAt(), gridId, portId);
        simulator.enqueueEvent(next);
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "gridId", gridId,
            "portId", portId
        );
    }
}
