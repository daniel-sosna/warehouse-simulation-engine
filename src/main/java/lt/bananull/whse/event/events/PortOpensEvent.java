package lt.bananull.whse.event.events;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.utils.DateTimeResolver;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
@Slf4j
public class PortOpensEvent extends Event {

    private final String gridId;
    private final String portId;
    private final Instant startsAt;
    private final Instant endsAt;

    public PortOpensEvent(long simTime, String gridId, String portId, Instant startsAt, Instant endsAt) {
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    @Override
    public void execute(Simulator simulator) {
        log.debug("sim time of open: " + getSimTime());
        Port port = simulator.getState().getPort(gridId, portId);
        port.open();
        long closingSimTime = DateTimeResolver.resolveSimTimeFromTimestamp(endsAt, simulator.getParameters().simulationStartTime());
        long nextDayOpen = DateTimeResolver.resolveSimTimeFromTimestamp(startsAt.plus(1, ChronoUnit.DAYS),
            simulator.getParameters().simulationStartTime());
        Event event = new PortClosesEvent(closingSimTime, gridId, portId, nextDayOpen, startsAt.plus(1,
            ChronoUnit.DAYS), endsAt.plus(1, ChronoUnit.DAYS));
        simulator.enqueueEvent(event);
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "gridId", gridId,
            "portId", portId
        );
    }
}
