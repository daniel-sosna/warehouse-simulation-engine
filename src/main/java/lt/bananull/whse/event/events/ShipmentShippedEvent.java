package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.Map;

public class ShipmentShippedEvent extends Event {

    private final String shipmentId;

    public ShipmentShippedEvent(long simTime, String shipmentId) {
        super(simTime);
        this.shipmentId = shipmentId;
    }

    @Override
    public void execute(Simulator simulator) {
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.markShipped();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "shipmentId", shipmentId
        );
    }
}
