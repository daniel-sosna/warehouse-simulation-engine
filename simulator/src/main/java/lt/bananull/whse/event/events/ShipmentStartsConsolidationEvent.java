package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.List;
import java.util.Map;

public class ShipmentStartsConsolidationEvent extends Event {

    private final AssignmentDto assignment;

    public ShipmentStartsConsolidationEvent(long simTime, AssignmentDto assignment) {
        super(simTime);
        this.assignment = assignment;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        Shipment shipment = simulator.getState().getShipment(assignment.shipmentId());
        shipment.routeToGrid(assignment.packingGrid(), assignment.picks());

        return List.of(new ShipmentIsReadyEvent(getSimTime(), assignment.shipmentId()));
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
