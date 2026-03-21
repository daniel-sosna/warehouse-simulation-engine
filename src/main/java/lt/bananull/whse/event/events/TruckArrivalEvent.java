package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static lt.bananull.whse.simulator.enums.ShipmentStatus.PACKED;

public class TruckArrivalEvent extends Event {

    private final String sortingDirection;
    private int shippedShipmentCount;

    public TruckArrivalEvent(long simTime, String sortingDirection) {
        super(simTime);
        this.sortingDirection = sortingDirection;
    }

    @Override
    public Optional<Event> execute(Simulator simulator) {
        List<Shipment> shipmentsToShip = simulator.getState().shipments().values().stream()
            .filter(shipment -> shipment.getStatus() == PACKED
                    && Objects.equals(shipment.getSortingDirection(), sortingDirection))
            .toList();

        shipmentsToShip.forEach(shipment ->
            simulator.enqueueEvent(new ShipmentShippedEvent(getSimTime(), shipment.getId())));
        shippedShipmentCount = shipmentsToShip.size();

        return Optional.empty();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "shipmentsTakenForShipping", shippedShipmentCount,
            "sortingDirection", sortingDirection
        );
    }
}
