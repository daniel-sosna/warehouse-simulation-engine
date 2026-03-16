package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;

@Slf4j
public class TestEvent extends Event {

    public TestEvent(long simTime) {
        super(simTime);
    }

    @Override
    public void execute(Simulator simulator) {
        log.info("Doing work...");
        log.debug("Debugging...");
    }
}
