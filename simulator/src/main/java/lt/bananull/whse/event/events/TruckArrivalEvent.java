package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static lt.bananull.whse.simulator.enums.ShipmentStatus.PACKED;

public class TruckArrivalEvent extends Event {

    private final String sortingDirection;
    private int shippedShipmentCount;

    public TruckArrivalEvent(long simTime, String sortingDirection) {
        super(simTime);
        this.sortingDirection = sortingDirection;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        List<Shipment> shipmentsToShip = simulator.getState().shipments().values().stream()
            .filter(shipment -> shipment.getStatus() == PACKED
                    && Objects.equals(shipment.getSortingDirection(), sortingDirection))
            .toList();
        shippedShipmentCount = shipmentsToShip.size();

        List<Event> events = new ArrayList<>();
        shipmentsToShip.forEach(shipment ->
            events.add(new ShipmentShippedEvent(getSimTime(), shipment.getId(), sortingDirection)));

        return events;
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "shipmentsShipped", shippedShipmentCount,
            "sortingDirection", sortingDirection
        );
    }
}
