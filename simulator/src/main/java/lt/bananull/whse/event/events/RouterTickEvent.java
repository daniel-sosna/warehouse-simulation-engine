package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.RouterRequestDto;
import lt.bananull.whse.router.dto.RouterResponseDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
        Instant now = simulator.getSimulationStart().plusSeconds(getSimTime());
        rollbackToReceived(simulator);
        checkForReceivedShipments(simulator, now);
        shipmentsRerouted = simulator.getState().shipments().values().stream()
                .filter(shipment -> shipment.getStatus() == RECEIVED)
                .count();

        RouterRequestDto request = RouterRequestDto.from(simulator.getState(), now);
        RouterResponseDto response = routerClient.route(request);
        simulator.updateAssignments(response.assignments());
        simulator.dispatchAll();

        long nextSimTime = getSimTime() + ROUTER_INTERVAL_SECONDS;
        if (nextSimTime <= simulator.getSimulationDurationSeconds()) {
            simulator.enqueueEvent(new RouterTickEvent(nextSimTime, routerClient));
        }

        return List.of();
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
