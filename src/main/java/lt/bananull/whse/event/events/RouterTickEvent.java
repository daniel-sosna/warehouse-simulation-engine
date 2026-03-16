package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.RouterRequestDto;
import lt.bananull.whse.router.dto.RouterResponseDto;
import lt.bananull.whse.simulator.Simulator;

@Slf4j
public class RouterTickEvent extends Event {

    public RouterTickEvent(long simTime) {
        super(simTime);
    }

    @Override
    public void execute(Simulator simulator) {
        log.info(">>> RouterTickEvent at simTime={} (now={})", getSimTime(), simulator.getNow());

        RouterClient routerClient = simulator.getRouterClient();
        RouterRequestDto request = RouterRequestDto.from(simulator.getState(), simulator.getNow());

        RouterResponseDto response = routerClient.route(request);

        simulator.getAssignments().addAll(response.assignments());

        simulator.dispatchAll();
    }
}