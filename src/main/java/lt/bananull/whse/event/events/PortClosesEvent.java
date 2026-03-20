package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.utils.DateTimeResolver;

import java.util.Map;

public class PortClosesEvent extends Event {

    private final String gridId;
    private final String portId;
    private final long openSimTime;
    private final long durationOfOpen;

    public PortClosesEvent(long simTime, String gridId, String portId, long openSimTime, long durationOfOpen){
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
        this.openSimTime = openSimTime;
        this.durationOfOpen = durationOfOpen;
    }

    @Override
    public void execute(Simulator simulator) {
        Port port = simulator.getState().getPort(gridId, portId);
        port.requestClose();
        long simEndTime = DateTimeResolver.resolveSimTimeFromTimestamp(simulator.getParameters().simulationEndTime(),
            simulator.getParameters().simulationStartTime());
        if (openSimTime < simEndTime) {
            Event event = new PortOpensEvent(openSimTime, gridId, portId, durationOfOpen);
            simulator.enqueueEvent(event);
        }
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "gridId", gridId,
            "portId", portId
        );
    }
}

