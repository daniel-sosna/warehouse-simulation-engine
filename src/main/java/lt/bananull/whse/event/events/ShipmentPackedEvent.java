package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.Map;

public class ShipmentPackedEvent extends Event {

    private final String shipmentId;
    private final long packingDurationSeconds;

    public ShipmentPackedEvent(long simTime, String shipmentId, long packingDurationSeconds) {
        super(simTime);
        this.shipmentId = shipmentId;
        this.packingDurationSeconds = packingDurationSeconds;
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
                "packingDurationSeconds", packingDurationSeconds
        );
    }
}