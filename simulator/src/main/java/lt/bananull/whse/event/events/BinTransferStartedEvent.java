package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;

import java.util.List;
import java.util.Map;

public class BinTransferStartedEvent extends Event {

    private final String shipmentId;

    public BinTransferStartedEvent(long simTime, String shipmentId) {
        super(simTime);
        this.shipmentId = shipmentId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        // pseudocode:
        // something along the lines of
        // get the shipment by id
        // get all the bins associated
        // call them to the grid, set their status to outside
        // however dont know what happens if they are reserved
        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
