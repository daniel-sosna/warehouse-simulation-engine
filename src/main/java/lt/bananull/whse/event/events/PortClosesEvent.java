package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;

import java.util.Map;

public class PortClosesEvent extends Event {

    private final String gridId;
    private final String portId;
    private final long openSimTime;

    public PortClosesEvent(long simTime, String gridId, String portId, long openSimTime){
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
        this.openSimTime = openSimTime;
    }

    @Override
    public void execute(Simulator simulator) {
        Port port = simulator.getState().getPort(gridId, portId);
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}

