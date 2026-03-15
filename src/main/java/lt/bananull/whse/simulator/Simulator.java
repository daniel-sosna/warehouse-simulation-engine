package lt.bananull.whse.simulator;

import com.fasterxml.jackson.core.JsonProcessingException;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.RouterRequestDto;
import lt.bananull.whse.router.dto.RouterResponseDto;
import lt.bananull.whse.utils.JacksonMapper;

import java.time.Instant;
import java.util.PriorityQueue;

public class Simulator {

    private final RouterClient routerClient;
    private Instant now;
    private SimulationStateDto state;
    private PriorityQueue<AssignmentDto> assignments = new PriorityQueue<>();

    public Simulator(RouterClient routerClient, SimulationStateDto initialState) {
        this.routerClient = routerClient;
        this.state = initialState;
    }

    public void run() {
        // TEMP
        RouterRequestDto routerRequest = RouterRequestDto.from(state);
        RouterResponseDto routerResponse = routerClient.route(routerRequest);
        try {
            System.out.println(JacksonMapper.create().writeValueAsString(routerResponse));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Run event loop
    }
}
