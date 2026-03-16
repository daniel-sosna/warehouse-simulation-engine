package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.RouterRequestDto;
import lt.bananull.whse.router.dto.RouterResponseDto;
import lt.bananull.whse.simulator.Simulator;

@Slf4j
public class RouterTickEvent extends Event {

    private final RouterClient routerClient;

    public RouterTickEvent(long simTime, RouterClient routerClient) {
        super(simTime);
        this.routerClient = routerClient;
    }

    @Override
    public void execute(Simulator simulator) {
        RouterRequestDto request = RouterRequestDto.from(simulator.getState(), simulator.getNow());
        RouterResponseDto response = routerClient.route(request);

        simulator.updateAssignments(response.assignments());

        long nextSimTime = getSimTime() + simulator.getROUTER_PERIOD();
        if (nextSimTime <= simulator.getSimulationDurationSeconds()) {
            simulator.enqueueEvent(new RouterTickEvent(nextSimTime, routerClient));
        }
    }
}