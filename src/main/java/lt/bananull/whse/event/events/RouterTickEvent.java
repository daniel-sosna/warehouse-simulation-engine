package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.RouterRequestDto;
import lt.bananull.whse.router.dto.RouterResponseDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.enums.ShipmentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouterTickEvent extends Event {

    private final RouterClient routerClient;

    public RouterTickEvent(long simTime, RouterClient routerClient) {
        super(simTime);
        this.routerClient = routerClient;
    }

    @Override
    public void execute(Simulator simulator) {
        checkForReceivedShipments(simulator);

        RouterRequestDto request = RouterRequestDto.from(simulator.getState(), simulator.getNow());
        RouterResponseDto response = routerClient.route(request);

        simulator.updateAssignments(response.assignments());

        simulator.dispatchAll();

        long nextSimTime = getSimTime() + simulator.getROUTER_PERIOD();
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
                    shipment.setStatus(ShipmentStatus.RECEIVED);
                    long shipmentSimTime = simulator.getSimulationDurationSeconds() - (simulator.getSimulationEndTime().getEpochSecond() - shipment.getShipmentDate().getEpochSecond());
                    ShipmentReceivedEvent event = new ShipmentReceivedEvent(shipmentSimTime, shipment);
                    Logger eventLogger = LoggerFactory.getLogger("EVENT_LOGGER");
                    eventLogger.info(event.toString());
                });
    }
}