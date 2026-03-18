package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;

import java.util.Map;

public class BinRequestedAtPortEvent extends Event {

    private final String binId;
    private final String gridId;
    private final String portId;

    public BinRequestedAtPortEvent(long simTime, String binId, String gridId, String portId) {
        super(simTime);
        this.binId = binId;
        this.gridId = gridId;
        this.portId = portId;
    }

    @Override
    public void execute(Simulator simulator) {
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
