package lt.bananull.whse.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.utils.JacksonMapper;

import java.util.Optional;

@Slf4j(topic = "EVENT_LOGGER")
public class EventHandler {

    private static final ObjectMapper MAPPER = JacksonMapper.create();

    private final Simulator simulator;

    public EventHandler(Simulator simulator) {
        this.simulator = simulator;
    }

    public void handle(Event event) {
        Optional<Event> result = event.execute(simulator);
        log.debug(toJson(event));
        result.ifPresent(this::handle);
    }

    private String toJson(Event event) {
        LogEvent logEvent = new LogEvent(event, simulator.getParameters().simulationStartTime());
        try {
            return MAPPER.writeValueAsString(logEvent);
        } catch (Exception e) {
            return "{\"error\":\"failed to serialize event\",\"event\":\"" + event.getClass().getSimpleName()
                    + "\",\"cause\":\"" + e.getMessage() + "\"}";
        }
    }

}
