package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
        port.startNextShipment();

        Shipment shipment = state.getShipment(port.getActiveShipmentId());
        shipment.startPicking();

        shipmentId = shipment.getId();
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
        return Map.of(
                "gridId", gridId,
                "portId", portId,
                "shipmentId", shipmentId,
                "sortingDirection", sortingDirection,
                "handlingFlags", handlingFlags,
                "shipmentItems", items
        );
    }
}
