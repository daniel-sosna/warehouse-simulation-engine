package lt.bananull.whse.simulator.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lt.bananull.whse.load.dto.PortDto;
import lt.bananull.whse.simulator.enums.PortStatus;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;

/**
 * Simulation entity representing a packing station.
 */
@Getter
public class Port {

    public static final int DEFAULT_QUEUE_CAPACITY = 20;

    private final String id;
    private final Set<String> handlingFlags;
    private PortStatus status;
    private String activeShipmentId;
    @Getter(AccessLevel.NONE)
    private final Queue<String> shipmentQueue = new ArrayDeque<>();

    public Port(String id, Collection<String> handlingFlags) {
        this.id = id;
        this.handlingFlags = Set.copyOf(handlingFlags);
        this.status = PortStatus.CLOSED;
    }

    public static Port from(PortDto dto) {
        return new Port(dto.id(), dto.handlingFlags());
    }

    public Collection<String> getShipmentQueue() { return Collections.unmodifiableCollection(shipmentQueue); }

    public boolean hasCapacity() {
        return shipmentQueue.size() < DEFAULT_QUEUE_CAPACITY;
    }

    public boolean canHandle(Set<String> shipmentHandlingFlags) {
        return handlingFlags.containsAll(shipmentHandlingFlags);
    }

    public void open() {
        if (status != PortStatus.CLOSED) {
            throw new IllegalStateException("Port %s cannot open from status %s".formatted(id, status));
        }

        this.status = PortStatus.IDLE;
    }

    public void requestClose() {
        if (status != PortStatus.IDLE && status != PortStatus.BUSY) {
            throw new IllegalStateException("Port %s cannot be closed from status %s".formatted(id, status));
        }

        this.status = status == PortStatus.BUSY ? PortStatus.PENDING_CLOSE : PortStatus.CLOSED;
    }

    /**
     * Starts processing the next queued shipment.
     *
     * @return shipment ID that became active.
     */
    public String startNextShipment() {
        if (status != PortStatus.IDLE) {
            throw new IllegalStateException(
                    "Port %s cannot start a shipment from status %s".formatted(id, status));
        }

        String nextShipmentId = shipmentQueue.poll();
        if (nextShipmentId == null) {
            throw new IllegalStateException("Port %s has no queued shipments to start".formatted(id));
        }

        this.activeShipmentId = nextShipmentId;
        this.status = PortStatus.BUSY;
        return nextShipmentId;
    }

    /**
     * Completes the currently active shipment and transitions the port back to the next valid status.
     *
     * @return completed shipment ID.
     */
    public String completeActiveShipment() {
        if (status != PortStatus.BUSY && status != PortStatus.PENDING_CLOSE) {
            throw new IllegalStateException(
                    "Port %s cannot complete shipment from status %s".formatted(id, status));
        }

        String completedShipmentId = activeShipmentId;
        this.activeShipmentId = null;
        this.status = status == PortStatus.PENDING_CLOSE ? PortStatus.CLOSED : PortStatus.IDLE;
        return completedShipmentId;
    }

    public void enqueueShipment(String shipmentId, Set<String> shipmentHandlingFlags) {
        if (status != PortStatus.IDLE && status != PortStatus.BUSY) {
            throw new IllegalStateException(
                    "Port %s cannot accept new shipments from status %s".formatted(id, status));
        }
        if (!hasCapacity()) {
            throw new IllegalStateException(
                    "Port %s queue is full (%d/%d)".formatted(id, shipmentQueue.size(), DEFAULT_QUEUE_CAPACITY));
        }
        if (!canHandle(shipmentHandlingFlags)) {
            throw new IllegalStateException("Port %s cannot handle shipment %s with flags %s".formatted(id, shipmentId, shipmentHandlingFlags));
        }

        shipmentQueue.add(shipmentId);
    }

    @Override
    public String toString() {
        return "Port{id='%s', status=%s, queueSize=%d/%d, active='%s'}"
                .formatted(id, status, shipmentQueue.size(), DEFAULT_QUEUE_CAPACITY, activeShipmentId);
    }
}
