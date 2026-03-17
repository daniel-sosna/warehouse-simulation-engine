package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;

import java.util.Map;

@Slf4j
public class BinPickCompletedEvent extends Event {

    private final AssignmentDto assignment;
    private final String binId;
    private final String packingGrid;

    public BinPickCompletedEvent(long simTime, AssignmentDto assignment) {
        super(simTime);
        this.assignment = assignment;
        PickDto firstPick = assignment.picks().getFirst();
        this.binId = firstPick.binId();
        this.packingGrid = assignment.packingGrid();
    }

    @Override
    public void execute(Simulator simulator) {
        // For now just logging...

        // Todo:
        // - decrement stock in state
        // - mark shipment Packed if all items picked
        // - set bin status Available
        // - trigger simulator.dispatch() to start next waiting assignment
        // - set port status to idle and then start next shipment again
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "shipmentId", assignment.shipmentId(),
                "binId", binId,
                "grid", packingGrid
        );
    }
}
