package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static lt.bananull.whse.simulator.enums.ShipmentStatus.CONSOLIDATION;

public class BinTransferCompletedEvent extends Event {

    private final String binId;
    private final String destGridId;

    public BinTransferCompletedEvent(long simTime, long duration, String binId, String destGridId) {
        super(simTime, duration);
        this.binId = binId;
        this.destGridId = destGridId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        SimulationState state = simulator.getState();
        Bin bin = state.getBin(binId);

        bin.arriveAtGrid();
        List<Event> events = new ArrayList<>();

        state.shipments().values().stream()
            .filter(shipment -> shipment.getStatus() == CONSOLIDATION
                && shipment.getPicks().stream().map(PickDto::binId).anyMatch(binId::equals)
                && shipment.isConsolidated(state))
            .forEach(shipment -> events.add(new ShipmentIsReadyEvent(getSimTime(), shipment.getId())));

        return events;
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "binId", binId,
                "destGridId", destGridId
        );
    }
}
