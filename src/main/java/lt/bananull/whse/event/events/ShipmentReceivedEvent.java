package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.List;
import java.util.Map;

public class ShipmentReceivedEvent extends Event {

    private final String shipmentId;

    public ShipmentReceivedEvent(long simTime, String shipmentId) {
        super(simTime);
        this.shipmentId = shipmentId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.markReceived();

        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of("shipmentId", shipmentId);
    }
}
