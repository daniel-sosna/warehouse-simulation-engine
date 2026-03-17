package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Grid;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;

public class ShipmentIsReady extends Event {

    private final String shipmentId;

    public ShipmentIsReady(long simTime, String shipmentId) {
        super(simTime);
        this.shipmentId = shipmentId;
    }

    @Override
    public void execute(Simulator simulator) {
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.startConsolidation();
        shipment.markReady();


    }

    private void enqueueShipment(Simulator simulator, Shipment shipment) {
        Grid currentGrid = simulator.getState().getGrid(shipment.getAssignedGridId());
        for (Port port : currentGrid.getPorts()) {
    }
}
