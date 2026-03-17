package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Grid;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.Map;
import java.util.Set;

import static lt.bananull.whse.simulator.enums.PortStatus.IDLE;

public class ShipmentIsReadyEvent extends Event {

    private final String shipmentId;

    public ShipmentIsReadyEvent(long simTime, String shipmentId) {
        super(simTime);
        this.shipmentId = shipmentId;
    }

    @Override
    public void execute(Simulator simulator) {
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.startConsolidation();
        shipment.markReady();

        enqueueShipment(simulator, shipment);
    }

    private void enqueueShipment(Simulator simulator, Shipment shipment) {
        Grid currentGrid = simulator.getState().getGrid(shipment.getAssignedGridId());
        Port availablePort = currentGrid.getAvailablePort();
        if (availablePort != null) {
            availablePort.enqueueShipment(shipmentId, Set.of());
            // TODO: handling flags being passed as empty set because shipment has no field handlingFlags
            if (availablePort.getStatus() == IDLE) {
                simulator.enqueueEvent(new BeginShipmentPickingEvent(getSimTime(), currentGrid.getId(), availablePort.getId()));
            }
        } else {
            currentGrid.enqueueShipment(shipmentId);
        }
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of("shipmentId", shipmentId);
    }

}
