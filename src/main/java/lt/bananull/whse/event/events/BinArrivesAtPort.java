package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;

import java.util.Map;

@Slf4j
public class BinArrivesAtPort extends Event {

    private final AssignmentDto assignment;
    private final String binId;
    private final String packingGrid;

    public BinArrivesAtPort(long simTime, AssignmentDto assignment) {
        super(simTime);
        this.assignment = assignment;
        PickDto firstPick = assignment.picks().getFirst();
        this.binId = firstPick.binId();
        this.packingGrid = assignment.packingGrid();
    }

    @Override
    public void execute(Simulator simulator) {
        int standardRate = simulator.getParameters().pickingThroughput().standard();
        long pickSeconds = (long) (3600.0 / standardRate);
        long pickDoneAt = getSimTime() + pickSeconds;
        simulator.enqueueEvent(new BinPickCompleted(pickDoneAt, assignment));
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
