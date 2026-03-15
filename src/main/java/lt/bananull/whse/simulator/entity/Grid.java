package lt.bananull.whse.simulator.entity;

import lt.bananull.whse.load.dto.GridDto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

/**
 * Simulation entity representing an AutoStore grid (or any self-contained storage area).
 */
public class Grid {

    private final String id;
    private final List<String> portIds;
    private final Queue<String> shipmentQueue;

    public Grid(String id) {
        this.id = id;
        this.portIds = new ArrayList<>();
        this.shipmentQueue = new ArrayDeque<>();
    }

    public static Grid from(GridDto dto) {
        return new Grid(dto.id());
    }

    public String getId() { return id; }

    public List<String> getPortIds() { return Collections.unmodifiableList(portIds); }

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

    public void registerPort(String portId) {
        if (!portIds.contains(portId)) {
            portIds.add(portId);
        }
    }

    @Override
    public String toString() {
        return "Grid{id='%s', ports=%s, queueSize=%d}".formatted(id, portIds, shipmentQueue.size());
    }
}
