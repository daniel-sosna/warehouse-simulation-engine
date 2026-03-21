package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;

import java.util.Map;
import java.util.Optional;

public class PortStartsShipmentEvent extends Event {

    private final String gridId;
    private final String portId;

    public PortStartsShipmentEvent(long simTime, String gridId, String portId) {
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
    }

    @Override
    public Optional<Event> execute(Simulator simulator) {
        BinRequestedAtPortEvent.tryScheduleFor(gridId, portId, getSimTime(), simulator);
        return Optional.empty();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "gridId", gridId,
                "portId", portId
        );
    }
}
