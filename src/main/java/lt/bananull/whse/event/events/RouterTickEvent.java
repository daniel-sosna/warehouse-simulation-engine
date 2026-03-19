package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.RouterRequestDto;
import lt.bananull.whse.router.dto.RouterResponseDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.Map;

public class RouterTickEvent extends Event {

    private static final int ROUTER_INTERVAL_SECONDS = 900;

    private final RouterClient routerClient;

    public RouterTickEvent(long simTime, RouterClient routerClient) {
        super(simTime, 0);
        this.routerClient = routerClient;
    }

    @Override
    public void execute(Simulator simulator) {
        checkForReceivedShipments(simulator);
        // rollBackToReceived(simulator); // TODO: uncomment when shipment picking is fully implemented

        RouterRequestDto request = RouterRequestDto.from(simulator.getState(), simulator.getNow());
        RouterResponseDto response = routerClient.route(request);

        simulator.updateAssignments(response.assignments());
        simulator.dispatchAll();

        long nextSimTime = getSimTime() + ROUTER_INTERVAL_SECONDS;
        if (nextSimTime <= simulator.getSimulationDurationSeconds()) {
            simulator.enqueueEvent(new RouterTickEvent(nextSimTime, routerClient));
        }
    }

    /**
     * Finds all new shipments, changes their status to RECEIVED and logs ShipmentReceivedEvent
     */
    private void checkForReceivedShipments(Simulator simulator) {
        simulator.getState().shipments().values().stream()
                .filter(shipment -> shipment.getStatus() == null &&
                        !shipment.getShipmentDate().isAfter(simulator.getNow()))
                .forEach(shipment -> {
                    long shipmentSimTime = shipment.getShipmentDate().getEpochSecond() - simulator.getSimulationStartTime().getEpochSecond();
                    ShipmentReceivedEvent event = new ShipmentReceivedEvent(shipmentSimTime, shipment.getId());
                    simulator.getEventHandler().handle(event);
                });
    }

    private void rollbackToReceived(Simulator simulator) {
        simulator.getState().shipments().values().stream()
                .filter(Shipment::isAvailableForRerouting)
                .forEach(Shipment::rollbackToReceived);
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
