package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.List;
import java.util.Map;

public class ShipmentShippedEvent extends Event {

    private final String shipmentId;
    private final String sortingDirection;

    public ShipmentShippedEvent(long simTime, String shipmentId, String sortingDirection) {
        super(simTime);
        this.shipmentId = shipmentId;
        this.sortingDirection = sortingDirection;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.markShipped();

        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "shipmentId", shipmentId,
            "sortingDirection", sortingDirection
        );
    }
}
