package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.ArrayList;
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
        String packingGrid = assignment.packingGrid();
        Shipment shipment = simulator.getState().getShipment(assignment.shipmentId());
        shipment.routeToGrid(packingGrid, assignment.picks());

        List<Event> events = new ArrayList<>();

        shipment.getPicks().stream()
            .map(PickDto::binId)
            .distinct()
            .forEach(binId -> {
                Bin bin = simulator.getState().getBin(binId);

                if (!bin.getCurrentGridId().equals(packingGrid)) {
                    events.add(new BinTransferStartedEvent(getSimTime(), binId, packingGrid));
                }
                bin.incrementNeededInGrid();
            });

        if (events.isEmpty() && shipment.isConsolidated(simulator.getState())) {
            events.add(new ShipmentIsReadyEvent(getSimTime(), shipment.getId()));
        }

        return events;
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
