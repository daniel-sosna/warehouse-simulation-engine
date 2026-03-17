package lt.bananull.whse.simulator.entity;

import lombok.Getter;
import lombok.Setter;
import lt.bananull.whse.load.dto.ShipmentDto;
import lt.bananull.whse.simulator.enums.ShipmentStatus;

import java.time.Instant;
import java.util.List;
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
    private final List<String> handlingFlags;
    @Setter
    private ShipmentStatus status;
    private String assignedGridId;
    private String assignedPortId;

    public Shipment(String id, Map<String, Integer> items, Instant shipmentDate, List<String> handlingFlags) {
        this.id = id;
        this.items = Map.copyOf(items);
        this.shipmentDate = shipmentDate;
        this.handlingFlags = handlingFlags != null ? List.copyOf(handlingFlags) : List.of();
        this.status = null;
    }

    public static Shipment from(ShipmentDto dto) {
        return new Shipment(dto.id(), dto.items(), dto.shipmentDate(), dto.handlingFlags());
    }

    public boolean isAvailableForRerouting() {
        return (status == ShipmentStatus.ROUTED
                || status == ShipmentStatus.CONSOLIDATION
                || status == ShipmentStatus.READY)
                && assignedPortId == null;
    }

    public void routeToGrid(String gridId) {
        if (status != ShipmentStatus.RECEIVED) {
            // throw new IllegalStateException("Shipment %s cannot be routed from status %s".formatted(id, status));
            // TODO: uncomment this when roll back to received is implemented and used
        }

        this.assignedGridId = gridId;
        this.assignedPortId = null;
        this.status = ShipmentStatus.ROUTED;
    }

    public void startConsolidation() {
        if (status != ShipmentStatus.ROUTED) {
            throw new IllegalStateException(
                    "Shipment %s cannot start consolidation from status %s".formatted(id, status));
        }

        this.status = ShipmentStatus.CONSOLIDATION;
    }

    public void markReady() {
        if (status != ShipmentStatus.CONSOLIDATION) {
            throw new IllegalStateException(
                    "Shipment %s cannot become ready from status %s".formatted(id, status));
        }

        this.status = ShipmentStatus.READY;
    }

    public void assignToPort(String portId) {
        if (status != ShipmentStatus.READY) {
            throw new IllegalStateException(
                    "Shipment %s cannot be assigned to a port from status %s".formatted(id, status));
        }
        if (assignedPortId != null) {
            throw new IllegalStateException(
                    "Shipment %s is already assigned to port %s".formatted(id, assignedPortId));
        }

        this.assignedPortId = portId;
    }

    public void unassignPort() {
        if (status != ShipmentStatus.READY) {
            throw new IllegalStateException(
                    "Shipment %s cannot be unassigned from a port in status %s".formatted(id, status));
        }
        if (assignedPortId == null) {
            throw new IllegalStateException("Shipment %s has no assigned port".formatted(id));
        }

        this.assignedPortId = null;
    }

    public void startPicking() {
        if (status != ShipmentStatus.READY) {
            throw new IllegalStateException(
                    "Shipment %s cannot start picking from status %s".formatted(id, status));
        }
        if (assignedPortId == null) {
            throw new IllegalStateException("Shipment %s must be assigned to a port before picking".formatted(id));
        }

        this.status = ShipmentStatus.PICKING;
    }

    public void markPacked() {
        if (status != ShipmentStatus.PICKING) {
            throw new IllegalStateException(
                    "Shipment %s cannot be packed from status %s".formatted(id, status));
        }

        this.assignedGridId = null;
        this.assignedPortId = null;
        this.status = ShipmentStatus.PACKED;
    }

    public void markShipped() {
        if (status != ShipmentStatus.PACKED) {
            throw new IllegalStateException(
                    "Shipment %s cannot be shipped from status %s".formatted(id, status));
        }

        this.status = ShipmentStatus.SHIPPED;
    }

    public void rollbackToReceived() {
        if (!isAvailableForRerouting()) {
            throw new IllegalStateException(
                    "Shipment %s cannot be rolled back to received. Status: %s, Assigned Port: %s"
                            .formatted(id, status, assignedPortId));
        }

        this.assignedGridId = null;
        this.status = ShipmentStatus.RECEIVED;
    }

    @Override
    public String toString() {
        return "Shipment{id='%s', status=%s, grid='%s', port='%s'}"
                .formatted(id, status, assignedGridId, assignedPortId);
    }
}
