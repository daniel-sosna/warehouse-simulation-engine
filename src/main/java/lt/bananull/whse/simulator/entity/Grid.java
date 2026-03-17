package lt.bananull.whse.simulator.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lt.bananull.whse.load.dto.GridDto;
import lt.bananull.whse.load.dto.PortDto;
import lt.bananull.whse.load.dto.ShiftDto;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static lt.bananull.whse.simulator.enums.PortStatus.CLOSED;

/**
 * Simulation entity representing an AutoStore grid (or any self-contained storage area).
 */
@Getter
public class Grid {

    private final String id;
    private final List<ShiftDto> shifts;
    private final Map<String, Port> ports;
    @Getter(AccessLevel.NONE)
    private final Queue<String> shipmentQueue = new ArrayDeque<>();

    public Grid(String id, Collection<ShiftDto> shifts, Map<String, Port> ports) {
        this.id = id;
        this.shifts = List.copyOf(shifts);
        this.ports = Map.copyOf(ports);
    }

    public static Grid from(GridDto dto) {
        Map<String, Port> ports = new HashMap<>();
        for (ShiftDto shift : dto.shifts()) {
            for (PortDto port : shift.portConfig()) {
                ports.put(port.id(), Port.from(port));
            }
        }
        return new Grid(dto.id(), dto.shifts(), ports);
    }

    // TODO: include handling flags filter
    public Port getAvailablePort() {
        for (Port port : ports.values()) {
            if (port.hasCapacity() && port.getStatus() != CLOSED) {
                return port;
            }
        }
        return null;
    }

    public Collection<String> getShipmentQueue() { return Collections.unmodifiableCollection(shipmentQueue); }

    public void enqueueShipment(String shipmentId) {
        shipmentQueue.add(shipmentId);
    }

    public String dequeueShipment() {
        return shipmentQueue.poll();
    }

    @Override
    public String toString() {
        return "Grid{id='%s', shifts=%s, queueSize=%d}".formatted(id, shifts, shipmentQueue.size());
    }
}
