package lt.bananull.whse.event;

import lt.bananull.whse.simulator.Simulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventHandler {

    private static final Logger eventLogger = LoggerFactory.getLogger("EVENT_LOGGER");

    private final Simulator simulator;

    public EventHandler (Simulator simulator) {
        this.simulator = simulator;
    }

    public void handle(Event event) {
        eventLogger.info(event.toString());
        event.execute(simulator);
    }

}