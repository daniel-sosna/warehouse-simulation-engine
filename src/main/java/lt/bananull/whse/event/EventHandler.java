package lt.bananull.whse.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.simulator.Simulator;

import java.util.LinkedHashMap;
import java.util.Map;

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
        log.info(toJson(event));
    }

    private String toJson(Event event) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("simTime", event.getSimTime());
        entry.put("timestamp", simulator.getNow().toString());
        entry.put("event", event.getClass().getSimpleName());
        entry.put("data", event.getData());
        try {
            return MAPPER.writeValueAsString(entry);
        } catch (Exception e) {
            return "{\"error\":\"failed to serialize event\",\"event\":\"" + event.getClass().getSimpleName() + "\",\"cause\":\"" + e.getMessage() + "\"}";
        }
    }

}