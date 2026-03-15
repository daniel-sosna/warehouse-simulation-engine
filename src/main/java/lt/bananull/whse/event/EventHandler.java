package lt.bananull.whse.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventHandler {

    private static final Logger eventLogger = LoggerFactory.getLogger("EVENT_LOGGER");

    private EventHandler () {}

    public static void handle(Event event) {
        eventLogger.info(event.toString());
        event.execute();
    }

}