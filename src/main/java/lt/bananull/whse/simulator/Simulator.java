package lt.bananull.whse.simulator;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.EventHandler;
import lt.bananull.whse.event.TestEvent;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.RouterRequestDto;
import lt.bananull.whse.router.dto.RouterResponseDto;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.time.Instant;
import java.util.PriorityQueue;

@Slf4j
public class Simulator {

    private final Instant simulationStartTime;
    private final Instant simulationEndTime;
    private final RouterClient routerClient;
    private long simTime = 0;
    private Instant now;
    private SimulationState state;
    private PriorityQueue<AssignmentDto> assignments = new PriorityQueue<>();

    public Simulator(RouterClient routerClient, SimulationStateDto initialState, Instant startTime,  Instant endTime) {
        this.routerClient = routerClient;
        this.state = SimulationState.from(initialState);
        this.simulationStartTime = startTime;
        this.simulationEndTime = endTime;
        this.now = startTime;
    }

    public void run() {
        // TEMP
        RouterRequestDto routerRequest = RouterRequestDto.from(state, now);
        RouterResponseDto routerResponse = routerClient.route(routerRequest);
        assignments.addAll(routerResponse.assignments());
        log.info(assignments.toString());

        EventHandler eventHandler = new EventHandler(this);
        eventHandler.handle(new TestEvent(100L));
        // Run event loop
    }
}
