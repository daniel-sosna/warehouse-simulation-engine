package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class BinArrivesAtPortEvent extends Event {

    private final String gridId;
    private final String binId;
    private final String portId;

    public BinArrivesAtPortEvent(long simTime, long duration, String gridId, String portId, String binId) {
        super(simTime, duration);
        this.gridId = gridId;
        this.binId = binId;
        this.portId = portId;
    }

    @Override
    public Optional<Event> execute(Simulator simulator) {
        double mult = simulator.resolveMultiplier(simulator.getParameters().pickingThroughput().randomness());
        int standardRate = simulator.getParameters().pickingThroughput().standard();
        long duration = Math.round((3600.0 / standardRate) * mult);
        long pickDoneAt = getSimTime() + duration;

        Port port = simulator.getState().getPort(portId);
        simulator.enqueueEvent(new BinPickCompletedEvent(pickDoneAt, port.getActiveShipmentId(), gridId, portId, binId, duration));

        return Optional.empty();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "binId", binId
        );
    }
}
