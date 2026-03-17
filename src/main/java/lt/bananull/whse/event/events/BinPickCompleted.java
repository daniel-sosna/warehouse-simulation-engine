package lt.bananull.whse.event.events;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;

@Slf4j
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
        // For now just logging...

        // Todo:
        // - decrement stock in state
        // - mark shipment Packed if all items picked
        // - set bin status Available
        // - trigger simulator.dispatch() to start next waiting assignment
        // - set port status to idle and then start next shipment again
    }

    @Override
    public String toString() {
        return super.toString()
                + ";shipmentId=" + assignment.shipmentId()
                + ";binId=" + binId
                + ";grid=" + packingGrid;
    }
}
