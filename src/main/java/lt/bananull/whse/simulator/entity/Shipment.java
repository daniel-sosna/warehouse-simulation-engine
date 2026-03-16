package lt.bananull.whse.simulator.entity;

import lombok.Getter;
import lombok.Setter;
import lt.bananull.whse.load.dto.ShipmentDto;
import lt.bananull.whse.simulator.enums.ShipmentStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Simulation entity representing a customer order.
 * Mutable throughout the shipment lifecycle.
 */
@Getter
public class Shipment {

    private final String id;
    private final Map<String, Integer> items;
    private final Instant shipmentDate;
    @Setter
    private ShipmentStatus status;
    @Setter
    private String assignedGridId;
    @Setter
    private String assignedPortId;

    public Shipment(String id, Map<String, Integer> items, Instant shipmentDate) {
        this.id = id;
        this.items = Map.copyOf(items);
        this.shipmentDate = shipmentDate;
        this.status = ShipmentStatus.RECEIVED;
    }

    public static Shipment from(ShipmentDto dto) {
        return new Shipment(dto.id(), dto.items(), dto.shipmentDate());
    }

    @Override
    public String toString() {
        return "Shipment{id='%s', status=%s, grid='%s', port='%s'}"
                .formatted(id, status, assignedGridId, assignedPortId);
    }
}
