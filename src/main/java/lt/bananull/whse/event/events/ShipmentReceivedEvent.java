package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;

import java.util.Map;

public class ShipmentReceivedEvent extends Event {

    private final String shipmentId;

    public ShipmentReceivedEvent(long simTime, String shipmentId) {
        super(simTime, 0);
        this.shipmentId = shipmentId;
    }

    @Override
    public void execute(Simulator simulator) {
        simulator.getState().getShipment(shipmentId).markReceived();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of("shipmentId", shipmentId);
    }
}
