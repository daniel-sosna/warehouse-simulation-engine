package lt.bananull.whse.event;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.simulator.Simulator;

@Slf4j(topic = "EVENT_LOGGER")
public class EventHandler {

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
        log.info(event.toString());
    }

}