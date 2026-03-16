package lt.bananull.whse.simulator.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
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
    @Setter
    private PortStatus status;
    @Setter
    private String activeShipmentId;
    @Getter(AccessLevel.NONE)
    private final Queue<String> shipmentQueue;

    public Port(String id, Collection<String> handlingFlags) {
        this.id = id;
        this.handlingFlags = Set.copyOf(handlingFlags);
        this.status = PortStatus.CLOSED;
        this.shipmentQueue = new ArrayDeque<>();
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
        return "Port{id='%s', status=%s, queueSize=%d/%d, active='%s'}"
                .formatted(id, status, shipmentQueue.size(), DEFAULT_QUEUE_CAPACITY, activeShipmentId);
    }
}
