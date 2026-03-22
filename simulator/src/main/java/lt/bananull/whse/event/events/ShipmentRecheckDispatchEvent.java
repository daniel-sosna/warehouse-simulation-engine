package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;

import java.util.List;
import java.util.Map;

public class ShipmentRecheckDispatchEvent extends Event {
    private final String shipmentId;

    public ShipmentRecheckDispatchEvent(long simTime, String shipmentId) {
        super(simTime);
        this.shipmentId = shipmentId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        // Try to dispatch this shipment NOW (reserve remaining bins / start transfers / ready)
        // Essentially: the single-shipment version of your dispatch logic.
        simulator.tryDispatchShipment(shipmentId);
        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of("shipmentId", shipmentId);
    }
}
