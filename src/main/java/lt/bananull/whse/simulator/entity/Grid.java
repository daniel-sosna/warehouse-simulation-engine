package lt.bananull.whse.simulator.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lt.bananull.whse.load.dto.GridDto;
import lt.bananull.whse.load.dto.PortDto;
import lt.bananull.whse.load.dto.ShiftDto;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * Simulation entity representing an AutoStore grid (or any self-contained storage area).
 */
@Getter
public class Grid {

    private final String id;
    private final Set<String> portIds;
    @Getter(AccessLevel.NONE)
    private final Queue<String> shipmentQueue;

    public Grid(String id, Collection<String> portIds) {
        this.id = id;
        this.portIds = Set.copyOf(portIds);
        this.shipmentQueue = new ArrayDeque<>();
    }

    public static Grid from(GridDto dto) {
        List<String> portIds = dto.shifts().stream()
                .flatMap(shift -> shift.portConfig().stream())
                .map(PortDto::id)
                .distinct()
                .toList();
        return new Grid(dto.id(), portIds);
    }

    public Collection<String> getShipmentQueue() { return Collections.unmodifiableCollection(shipmentQueue); }

    public void enqueueShipment(String shipmentId) {
        shipmentQueue.add(shipmentId);
    }

    public String dequeueShipment() {
        return shipmentQueue.poll();
    }

    public boolean hasQueuedShipments() {
        return !shipmentQueue.isEmpty();
    }

    @Override
    public String toString() {
        return "Grid{id='%s', ports=%s, queueSize=%d}".formatted(id, portIds, shipmentQueue.size());
    }
}
