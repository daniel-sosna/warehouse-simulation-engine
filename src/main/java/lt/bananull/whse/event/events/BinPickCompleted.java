package lt.bananull.whse.event.events;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;

public class BinPickCompleted extends Event {

    private final AssignmentDto assignment;
    private final String binId;
    private final String packingGrid;

    public BinPickCompleted(long simTime, AssignmentDto assignment) {
        super(simTime);
        this.assignment = assignment;
        PickDto firstPick = assignment.picks().getFirst();
        this.binId = firstPick.binId();
        this.packingGrid = assignment.packingGrid();
    }

    @Override
    public void execute(Simulator simulator) {

    }
}
