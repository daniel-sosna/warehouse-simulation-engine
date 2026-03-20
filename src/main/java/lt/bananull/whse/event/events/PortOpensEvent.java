package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;

import java.util.Map;

public class PortOpensEvent extends Event {

    public PortOpensEvent(long simTime) {
        super(simTime);
    }

    @Override
    public void execute(Simulator simulator) {

    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
