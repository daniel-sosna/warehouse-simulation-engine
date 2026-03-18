package lt.bananull.whse.event;

import java.time.Instant;
import java.util.Map;

public record LogEvent (
    long simTime,
    String timestamp,
    String event,
    Map<String, Object> data
) {
    public LogEvent(Event event, Instant simulationStartTime) {
        this(event.getSimTime(),
            simulationStartTime.plusSeconds(event.getSimTime()).toString(),
            event.getClass().getSimpleName().split("Event")[0],
            event.getData());
    }
}
