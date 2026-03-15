package lt.bananull.whse.simulator;

import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.RouterRequestDto;
import lt.bananull.whse.router.dto.RouterResponseDto;

import java.time.Instant;
import java.util.PriorityQueue;

public class Simulator {

    private final Instant SimulationStartTime;
    private final Instant SimulationEndTime;
    private final RouterClient routerClient;
    private Instant now;
    private SimulationStateDto state;
    private PriorityQueue<AssignmentDto> assignments = new PriorityQueue<>();

    public Simulator(RouterClient routerClient, SimulationStateDto initialState, Instant startTime,  Instant endTime) {
        this.routerClient = routerClient;
        this.state = initialState;
        this.SimulationStartTime = startTime;
        this.SimulationEndTime = endTime;
        this.now = startTime;
    }

    public void run() {
        // TEMP
        RouterRequestDto routerRequest = RouterRequestDto.from(state, now);
        RouterResponseDto routerResponse = routerClient.route(routerRequest);
        assignments.addAll(routerResponse.assignments());
        System.out.println(assignments);

        // Run event loop
    }
}
