package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShipmentReceivedEvent extends Event {

    private final String shipmentId;
    private String sortingDirection;
    private Set<String> handlingFlags;
    private Map<String, Integer> items;

    public ShipmentReceivedEvent(long simTime, String shipmentId) {
        super(simTime);
        this.shipmentId = shipmentId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.markReceived();

        sortingDirection = shipment.getSortingDirection();
        handlingFlags = shipment.getHandlingFlags();
        items = shipment.getItems();

        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "shipmentId", shipmentId,
            "sortingDirection", sortingDirection,
            "handlingFlags", handlingFlags,
            "items", items
        );
    }
}
