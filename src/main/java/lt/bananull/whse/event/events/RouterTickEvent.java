package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.RouterRequestDto;
import lt.bananull.whse.router.dto.RouterResponseDto;
import lt.bananull.whse.simulator.Simulator;

public class RouterTickEvent extends Event {

    private static final int ROUTER_INTERVAL_SECONDS = 900;

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

        simulator.dispatchAll();

        long nextSimTime = getSimTime() + ROUTER_INTERVAL_SECONDS;
        if (nextSimTime <= simulator.getSimulationDurationSeconds()) {
            simulator.enqueueEvent(new RouterTickEvent(nextSimTime, routerClient));
        }
    }
}