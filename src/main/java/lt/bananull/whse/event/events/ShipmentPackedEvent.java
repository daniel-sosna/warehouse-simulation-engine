package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.List;
import java.util.Map;

public class ShipmentPackedEvent extends Event {

    private final String shipmentId;
    private final String gridId;
    private final String portId;

    public ShipmentPackedEvent(long simTime, String shipmentId, String gridId, String portId, long duration) {
        super(simTime, duration);
        this.shipmentId = shipmentId;
        this.gridId = gridId;
        this.portId = portId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        Port port = simulator.getState().getPort(portId);
        port.completeActiveShipment();
        shipment.markPacked();

        if (port.getQueueSize() > 0) {
            return List.of(new PortStartsShipmentEvent(getSimTime(), gridId, portId));
        }

        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "shipmentId", shipmentId
        );
    }
}
