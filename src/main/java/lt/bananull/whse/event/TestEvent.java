package lt.bananull.whse.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestEvent extends Event {

    private static final Logger log = LoggerFactory.getLogger(TestEvent.class);

    public TestEvent(long simTime) {
        super(simTime);
    }

    @Override
    public void execute() {
        log.info("Doing work...");
        log.debug("Debugging...");
    }
}
