package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.utils.DateTimeResolver;

import java.time.Instant;
import java.util.Map;
@Slf4j
public class PortClosesEvent extends Event {

    private final String gridId;
    private final String portId;
    private final long openSimTime;
    private final Instant startsAt;
    private final Instant endsAt;

    public PortClosesEvent(long simTime, String gridId, String portId, long openSimTime, Instant startsAt,
                           Instant endsAt){
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
        this.openSimTime = openSimTime;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    @Override
    public void execute(Simulator simulator) {
        Port port = simulator.getState().getPort(gridId, portId);
        port.requestClose();
        long simEndTime = DateTimeResolver.resolveSimTimeFromTimestamp(simulator.getParameters().simulationEndTime(),
            simulator.getParameters().simulationStartTime());
        if (openSimTime < simEndTime) {
            log.debug("im closing: " + getSimTime());
            Event event = new PortOpensEvent(openSimTime, gridId, portId, startsAt, endsAt);
            simulator.enqueueEvent(event);
        }
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "gridId", gridId,
            "portId", portId
        );
    }
}

