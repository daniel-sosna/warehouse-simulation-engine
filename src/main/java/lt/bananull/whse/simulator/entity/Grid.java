package lt.bananull.whse.simulator.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Comparator;

import static lt.bananull.whse.simulator.enums.PortStatus.CLOSED;

/**
 * Simulation entity representing an AutoStore grid (or any self-contained storage area).
 */
@Getter
@Slf4j
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

    public static Grid from(GridDto dto, int portQueueCapacity) {
        Map<String, Port> ports = new HashMap<>();
        for (ShiftDto shift : dto.shifts()) {
            for (PortDto port : shift.portConfig()) {
                ports.put(port.id(), Port.from(port, portQueueCapacity));
            }
        }
        return new Grid(dto.id(), dto.shifts(), ports);
    }


    // TODO: include handling flags filter
    public Port getAvailablePort() {
        Port chosen = ports.values().stream()
                .filter(port -> port.hasCapacity() && port.getStatus() != CLOSED)
                .min(Comparator.comparingInt(Port::getQueueSize))
                .orElse(null);

        log.info("Grid {} chose port {} among: {}",
                id,
                chosen != null ? chosen.getId() : "none",
                ports.values().stream()
                        .map(p -> "%s(queue=%d,status=%s)".formatted(p.getId(), p.getQueueSize(), p.getStatus()))
                        .toList()
        );

        return chosen;
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
