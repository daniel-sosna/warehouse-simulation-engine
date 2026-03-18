package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.Map;

public class ShipmentPackedEvent extends Event {

    private final String shipmentId;
    private final long duration;

    public ShipmentPackedEvent(long simTime, String shipmentId, long duration) {
        super(simTime);
        this.shipmentId = shipmentId;
        this.duration = duration;
    }

    @Override
    public void execute(Simulator simulator) {
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.markPacked();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "shipmentId", shipmentId,
                "Duration", duration
        );
    }
}
