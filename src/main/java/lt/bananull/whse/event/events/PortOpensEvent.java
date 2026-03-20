package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;

import java.util.Map;

public class PortOpensEvent extends Event {

    private final String gridId;
    private final String portId;
    private final long durationOfOpen;
    private final long simSecsInADay = 86400;

    public PortOpensEvent(long simTime, String gridId, String portId, long durationOfOpen) {
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
        this.durationOfOpen = durationOfOpen;
    }

    @Override
    public void execute(Simulator simulator) {
        Port port = simulator.getState().getPort(gridId, portId);
        port.open();
        long closingSimTime = getSimTime() + durationOfOpen;
        long nextDayOpen = getSimTime() + simSecsInADay;
        Event event = new PortClosesEvent(closingSimTime, gridId, portId, nextDayOpen, durationOfOpen);
        simulator.enqueueEvent(event);
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "gridId", gridId,
            "portId", portId
        );
    }
}
