package lt.bananull.whse.simulator;

import lt.bananull.whse.load.SimulationState;
import lt.bananull.whse.router.dto.Assignment;

import java.time.Instant;
import java.util.List;
import java.util.PriorityQueue;

public class Simulator {

    private PriorityQueue<Assignment> assignments = new PriorityQueue<>();
    private Instant now;
    private SimulationState state;

    public Simulator(List<Assignment> assignments) {
        this.assignments.addAll(assignments);
    }

    public void run() {
        // Run event loop
    }
}
