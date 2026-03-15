package lt.bananull.whse.event;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.simulator.Simulator;

@Slf4j(topic = "EVENT_LOGGER")
public class EventHandler {

    private final Simulator simulator;

    public EventHandler (Simulator simulator) {
        this.simulator = simulator;
    }

    public void handle(Event event) {
        log.info(event.toString());
        event.execute(simulator);
    }

}