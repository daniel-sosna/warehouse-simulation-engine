package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;

import java.util.Map;

public class PortClosesEvent extends Event {

    public PortClosesEvent(long simTime){
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

