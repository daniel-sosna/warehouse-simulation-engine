package lt.bananull.whse.event;

import lombok.Getter;
import lombok.Setter;
import lt.bananull.whse.simulator.Simulator;

import java.util.Map;

public abstract class Event implements Comparable<Event> {

    @Getter
    private final long simTime;

    @Getter
    private final long duration;

    public Event(long simTime, long duration) {
        this.simTime = simTime;
        this.duration = duration;
    }

    public abstract void execute(Simulator simulator);

    public abstract Map<String, Object> getData();

    @Override
    public int compareTo(Event other) {
        return Long.compare(this.simTime, other.simTime);
    }
}
