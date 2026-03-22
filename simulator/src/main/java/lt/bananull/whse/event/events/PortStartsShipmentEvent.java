package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PortStartsShipmentEvent extends Event {

    private final String gridId;
    private final String portId;
    private String shipmentId;
    private String sortingDirection;
    private Set<String> handlingFlags;
    private Map<String, Integer> items;

    public PortStartsShipmentEvent(long simTime, String gridId, String portId) {
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        SimulationState state = simulator.getState();

        Port port = state.getPort(portId);
        shipmentId = port.startNextShipment();

        Shipment shipment = state.getShipment(shipmentId);
        shipment.startPicking();

        sortingDirection = shipment.getSortingDirection();
        handlingFlags = shipment.getHandlingFlags();
        items = shipment.getItems();

        for (PickDto pick : shipment.getPicks()) {
            state.getBin(pick.binId()).reserveItem(pick.ean(), pick.qty());
        }

        BinRequestedAtPortEvent event = BinRequestedAtPortEvent.scheduleForPort(gridId, portId, getSimTime(), simulator);
        if (event != null) {
            return List.of(event);
        }

        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Stream.of(
                new AbstractMap.SimpleEntry<>("gridId", gridId),
                new AbstractMap.SimpleEntry<>("portId", portId),
                new AbstractMap.SimpleEntry<>("shipmentId", shipmentId),
                new AbstractMap.SimpleEntry<>("sortingDirection", sortingDirection),
                new AbstractMap.SimpleEntry<>("handlingFlags", handlingFlags),
                new AbstractMap.SimpleEntry<>("shipmentItems", items)
            )
            .filter(e -> e.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
