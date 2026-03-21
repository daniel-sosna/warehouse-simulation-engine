package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.Map;
import java.util.Optional;

public class ShipmentReceivedEvent extends Event {

    private final String shipmentId;

    public ShipmentReceivedEvent(long simTime, String shipmentId) {
        super(simTime);
        this.shipmentId = shipmentId;
    }

    @Override
    public Optional<Event> execute(Simulator simulator) {
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.markReceived();

        return Optional.empty();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of("shipmentId", shipmentId);
    }
}
