package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Grid;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static lt.bananull.whse.simulator.enums.PortStatus.IDLE;

public class ShipmentIsReadyEvent extends Event {

    private final String shipmentId;
    private String sortingDirection;
    private Set<String> handlingFlags;
    private Map<String, Integer> items;

    public ShipmentIsReadyEvent(long simTime, String shipmentId) {
        super(simTime);
        this.shipmentId = shipmentId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.startConsolidation();
        shipment.markReady();

        sortingDirection = shipment.getSortingDirection();
        handlingFlags = shipment.getHandlingFlags();
        items = shipment.getItems();

        Grid currentGrid = simulator.getState().getGrid(shipment.getAssignedGridId());
        Set<String> handlingFlags = shipment.getHandlingFlags();
        Port availablePort = currentGrid.getAvailablePort(handlingFlags);

        if (availablePort != null) {
            shipment.assignToPort(availablePort.getPortIndex());
            availablePort.enqueueShipment(shipmentId, handlingFlags);
            if (availablePort.getStatus() == IDLE) {
                return List.of(new PortStartsShipmentEvent(getSimTime(), currentGrid.getId(), availablePort.getPortIndex()));
            }
        } else {
            currentGrid.enqueueShipment(shipmentId);
        }

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
