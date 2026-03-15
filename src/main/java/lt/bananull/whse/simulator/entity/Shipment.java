package lt.bananull.whse.simulator.entity;

import lt.bananull.whse.load.dto.ShipmentDto;
import lt.bananull.whse.simulator.enums.ShipmentStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simulation entity representing a customer order.
 * Mutable throughout the shipment lifecycle.
 */
public class Shipment {

    private final String id;
    private final Map<String, Integer> items;
    private final Instant shipmentDate;
    private ShipmentStatus status;
    private String assignedGridId;
    private String assignedPortId;

    public Shipment(String id, Map<String, Integer> items, Instant shipmentDate) {
        this.id = id;
        this.items = new HashMap<>(items);
        this.shipmentDate = shipmentDate;
        this.status = ShipmentStatus.RECEIVED;
    }

    public static Shipment from(ShipmentDto dto) {
        return new Shipment(dto.id(), dto.items(), dto.shipmentDate());
    }

    public String getId() { return id; }

    public Map<String, Integer> getItems() { return Collections.unmodifiableMap(items); }

    public Instant getShipmentDate() { return shipmentDate; }

    public ShipmentStatus getStatus() { return status; }

    public String getAssignedGridId() { return assignedGridId; }

    public String getAssignedPortId() { return assignedPortId; }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public void setAssignedGridId(String assignedGridId) {
        this.assignedGridId = assignedGridId;
    }

    public void setAssignedPortId(String assignedPortId) {
        this.assignedPortId = assignedPortId;
    }

    @Override
    public String toString() {
        return "Shipment{id='%s', status=%s, grid='%s', port='%s'}"
                .formatted(id, status, assignedGridId, assignedPortId);
    }
}
