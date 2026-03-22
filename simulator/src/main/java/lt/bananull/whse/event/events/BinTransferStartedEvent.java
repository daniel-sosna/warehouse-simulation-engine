package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;

import java.util.List;
import java.util.Map;

public class BinTransferStartedEvent extends Event {

    public BinTransferStartedEvent(long simTime) {
        super(simTime);
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
