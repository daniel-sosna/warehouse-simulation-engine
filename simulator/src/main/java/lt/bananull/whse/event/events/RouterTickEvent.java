package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.RouterRequestDto;
import lt.bananull.whse.router.dto.RouterResponseDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lt.bananull.whse.simulator.enums.ShipmentStatus.RECEIVED;

public class RouterTickEvent extends Event {

    private static final int ROUTER_INTERVAL_SECONDS = 900;

    private final RouterClient routerClient;
    private long shipmentsRerouted = 0;

    public RouterTickEvent(long simTime, RouterClient routerClient) {
        super(simTime);
        this.routerClient = routerClient;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        SimulationState state = simulator.getState();
        Instant now = simulator.getSimulationStart().plusSeconds(getSimTime());

        rollbackToReceived(simulator);
        checkForReceivedShipments(simulator, now);
        shipmentsRerouted = state.shipments().values().stream()
                .filter(shipment -> shipment.getStatus() == RECEIVED)
                .count();

        RouterRequestDto request = RouterRequestDto.from(state, now);
        RouterResponseDto response = routerClient.route(request);
        simulator.updateAssignments(response.assignments());

        long nextSimTime = getSimTime() + ROUTER_INTERVAL_SECONDS;
        if (nextSimTime <= simulator.getSimulationDurationSeconds()) {
            simulator.enqueueEvent(new RouterTickEvent(nextSimTime, routerClient));
        }

        List<AssignmentDto> assignments = simulator.pollAssignmentsToDispatch();
        return assignments.stream()
            .map(assignment -> new ShipmentStartsConsolidationEvent(getSimTime(), assignment))
            .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Finds all new shipments, changes their status to RECEIVED and logs ShipmentReceivedEvent
     */
    private void checkForReceivedShipments(Simulator simulator, Instant now) {
        simulator.getState().shipments().values().stream()
                .filter(shipment -> shipment.getStatus() == null &&
                        !shipment.getShipmentDate().isAfter(now))
                .forEach(shipment -> {
                    long shipmentSimTime =
                        shipment.getShipmentDate().getEpochSecond() - simulator.getParameters().simulationStartTime().getEpochSecond();
                    ShipmentReceivedEvent event = new ShipmentReceivedEvent(shipmentSimTime, shipment.getId());
                    simulator.getEventHandler().handle(event); // No need to enqueue cause simTime is not in order
                });
    }

    private void rollbackToReceived(Simulator simulator) {
        simulator.getState().shipments().values().stream()
                .filter(Shipment::isAvailableForRerouting)
                .forEach(Shipment::rollbackToReceived);
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of("shipmentsRerouted", shipmentsRerouted);
    }
}
