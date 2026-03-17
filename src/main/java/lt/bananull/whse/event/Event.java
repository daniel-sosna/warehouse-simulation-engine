package lt.bananull.whse.event;

import lombok.Getter;
import lt.bananull.whse.simulator.Simulator;

import java.util.Map;

public abstract class Event implements Comparable<Event> {

    @Getter
    private final long simTime;

    public Event(long simTime) {
        this.simTime = simTime;
    }

    public abstract void execute(Simulator simulator);

    public abstract Map<String, Object> getData();

    @Override
    public int compareTo(Event other) {
        return Long.compare(this.simTime, other.simTime);
    }
}
