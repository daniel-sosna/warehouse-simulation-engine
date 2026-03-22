package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShipmentPackedEvent extends Event {

    private final String shipmentId;
    private final String gridId;
    private final String portId;
    private String sortingDirection;
    private Set<String> handlingFlags ;
    private Map<String, Integer> items;

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

        sortingDirection = shipment.getSortingDirection();
        handlingFlags = shipment.getHandlingFlags();
        items = shipment.getItems();

        if (port.getQueueSize() > 0) {
            return List.of(new PortStartsShipmentEvent(getSimTime(), gridId, portId));
        }

        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Stream.of(
                new AbstractMap.SimpleEntry<>("shipmentId", shipmentId),
                new AbstractMap.SimpleEntry<>("gridId", gridId),
                new AbstractMap.SimpleEntry<>("portId", portId),
                new AbstractMap.SimpleEntry<>("sortingDirection", sortingDirection),
                new AbstractMap.SimpleEntry<>("handlingFlags", handlingFlags),
                new AbstractMap.SimpleEntry<>("items", items)
            )
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    }
}
