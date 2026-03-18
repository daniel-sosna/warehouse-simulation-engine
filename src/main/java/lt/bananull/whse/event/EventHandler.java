package lt.bananull.whse.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.simulator.Simulator;

@Slf4j(topic = "EVENT_LOGGER")
public class EventHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static EventHandler instance;

    private final Simulator simulator;

    public static EventHandler getInstance(Simulator simulator) {
        if (instance == null) {
            instance = new EventHandler(simulator);
        }
        return instance;
    }

    private EventHandler (Simulator simulator) {
        this.simulator = simulator;
    }

    public void handle(Event event) {
        event.execute(simulator);
        log.debug(toJson(event));
    }

    private String toJson(Event event) {
        LogEvent logEvent = new LogEvent(event, simulator.getSimulationStartTime());
        try {
            return MAPPER.writeValueAsString(logEvent);
        } catch (Exception e) {
            return "{\"error\":\"failed to serialize event\",\"event\":\"" + event.getClass().getSimpleName()
                    + "\",\"cause\":\"" + e.getMessage() + "\"}";
        }
    }

}
