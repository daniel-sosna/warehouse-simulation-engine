package lt.bananull.whse.simulator.entity;

import lombok.Getter;
import lombok.Setter;
import lt.bananull.whse.load.dto.PortDto;
import lt.bananull.whse.simulator.enums.PortStatus;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Simulation entity representing a packing station.
 */
public class Port {

    public static final int DEFAULT_QUEUE_CAPACITY = 20;

    @Getter
    private final String id;
    @Getter
    private final String gridId;
    private final Set<String> handlingFlags;
    @Getter
    @Setter
    private PortStatus status;
    private final Queue<String> shipmentQueue;
    @Getter
    @Setter
    private String activeShipmentId;

    public Port(String id, String gridId, Set<String> handlingFlags) {
        this.id = id;
        this.gridId = gridId;
        this.handlingFlags = new HashSet<>(handlingFlags);
        this.status = PortStatus.CLOSED;
        this.shipmentQueue = new ArrayDeque<>();
    }

    public static Port from(PortDto dto, String gridId) {
        return new Port(dto.id(), gridId, new HashSet<>(dto.handlingFlags()));
    }

    public Set<String> getHandlingFlags() { return Collections.unmodifiableSet(handlingFlags); }

    public Collection<String> getShipmentQueue() { return Collections.unmodifiableCollection(shipmentQueue); }

    public boolean hasCapacity() {
        return shipmentQueue.size() < DEFAULT_QUEUE_CAPACITY;
    }

    public boolean canHandle(Set<String> shipmentHandlingFlags) {
        return handlingFlags.containsAll(shipmentHandlingFlags);
    }

    /**
     * Enqueues a shipment. Callers must check {@link #hasCapacity()} first.
     *
     * @throws IllegalStateException if the queue is at capacity.
     */
    public void enqueueShipment(String shipmentId) {
        if (!hasCapacity()) {
            throw new IllegalStateException(
                    "Port %s queue is full (%d/%d)".formatted(id, shipmentQueue.size(), DEFAULT_QUEUE_CAPACITY));
        }
        shipmentQueue.add(shipmentId);
    }

    public String dequeueShipment() {
        return shipmentQueue.poll();
    }

    @Override
    public String toString() {
        return "Port{id='%s', grid='%s', status=%s, queueSize=%d/%d, active='%s'}"
                .formatted(id, gridId, status, shipmentQueue.size(), DEFAULT_QUEUE_CAPACITY, activeShipmentId);
    }
}
