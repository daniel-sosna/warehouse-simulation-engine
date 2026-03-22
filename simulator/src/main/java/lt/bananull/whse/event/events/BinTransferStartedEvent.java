package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;

import java.util.List;
import java.util.Map;

public class BinTransferStartedEvent extends Event {

    private final String shipmentId;
    private final String binId;
    private final String destGridId;

    public BinTransferStartedEvent(long simTime, String shipmentId, String binId, String destGridId) {
        super(simTime);
        this.shipmentId = shipmentId;
        this.binId = binId;
        this.destGridId = destGridId;
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
