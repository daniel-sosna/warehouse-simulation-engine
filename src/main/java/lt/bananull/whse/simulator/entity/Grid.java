package lt.bananull.whse.simulator.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.load.dto.GridDto;
import lt.bananull.whse.load.dto.PortDto;
import lt.bananull.whse.load.dto.ShiftDto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Simulation entity representing an AutoStore grid (or any self-contained storage area).
 */
@Getter
@Slf4j
public class Grid {

    private final String id;
    private final List<Shift> shifts;
    private final Map<String, Port> ports;
    @Getter(AccessLevel.NONE)
    private final Queue<String> shipmentQueue = new ArrayDeque<>();

    private Grid(String id, Collection<Shift> shifts, Map<String, Port> ports) {
        this.id = id;
        this.shifts = List.copyOf(shifts);
        this.ports = Map.copyOf(ports);
    }

    public static Grid from(GridDto dto,
                            int portQueueCapacity,
                            Instant simulationStartTime,
                            Instant simulationEndTime,
                            ZoneId zone) {
        Map<String, Port> ports = new HashMap<>();
        for (ShiftDto shift : dto.shifts()) {
            for (PortDto port : shift.portConfig()) {
                ports.put(port.id(), Port.from(port, portQueueCapacity));
            }
        }
        LocalDate startDate = LocalDate.ofInstant(simulationStartTime, zone);
        LocalDate endDateExclusive = LocalDate.ofInstant(simulationEndTime, zone).plusDays(1); // before it was not
        // creating any shifts because we only had one day of shifts and the date was exclusive

        List<Shift> expandedShifts = dto.shifts().stream()
            .flatMap(shiftDto -> Shift.expandRecurring(shiftDto, startDate, endDateExclusive, zone).stream())
            .toList();

        return new Grid(dto.id(), expandedShifts, ports);
    }

    public Port getAvailablePort(Collection<String> shipmentHandlingFlags) {
        return ports.values().stream()
                .filter(port -> port.canAcceptShipment(shipmentHandlingFlags))
                .min(Comparator.comparingInt(Port::getQueueSize))
                .orElse(null);
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
