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
    private final Instant endsAt;

    public PortClosesEvent(long simTime, Instant endsAt, String gridId, String portId){
        super(simTime);
        this.endsAt = endsAt;
        this.gridId = gridId;
        this.portId = portId;
    }

    @Override
    public void execute(Simulator simulator) {

    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
            "gridId", gridId,
            "portId", portId
        );
    }
}

