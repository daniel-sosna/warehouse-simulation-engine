package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.service.PortShiftService;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shift;
import lt.bananull.whse.utils.DateTimeResolver;

import java.time.Instant;
import java.util.Map;

public class PortClosesEvent extends Event {

    private final String gridId;
    private final String portId;
    private final Instant endsAt;
    private final Shift.BreakOccurrence portBreak;

    public  PortClosesEvent(long simTime, Instant endsAt, String gridId, String portId,
                            Shift.BreakOccurrence portBreak){
        super(simTime);
        this.endsAt = endsAt;
        this.gridId = gridId;
        this.portId = portId;
        this.portBreak = portBreak;
    }

    @Override
    public void execute(Simulator simulator) {
        Port port = simulator.getState().getPort(gridId, portId);
        port.requestClose();

        if (portBreak != null) {
            handleBreak(simulator);
        } else {
            handleShift(simulator);
        }
    }

    private void handleShift(Simulator simulator) {
        Shift nextShift = PortShiftService.findNextShiftAfter(
            simulator.getState().getGrid(gridId),
            portId,
            endsAt
        );
        if (nextShift == null) return; // no more shifts
        long openAt = DateTimeResolver.resolveSimTimeFromTimestamp(
            nextShift.getStartAt(),
            simulator.getParameters().simulationStartTime()
        );
        Event next =  new PortOpensEvent(openAt, gridId, portId, false);
        simulator.enqueueEvent(next);
    }

    private void handleBreak(Simulator simulator) {
        long openAt = DateTimeResolver.resolveSimTimeFromTimestamp(
            portBreak.endAt(),
            simulator.getParameters().simulationStartTime()
        );
        simulator.enqueueEvent(new PortOpensEvent(openAt, gridId, portId, true));
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "gridId", gridId,
            "portId", portId
        );
    }
}

