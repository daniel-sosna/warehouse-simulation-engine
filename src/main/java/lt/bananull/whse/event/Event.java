package lt.bananull.whse.event;

public abstract class Event implements Comparable<Event> {
    private final long simTime;

    public Event(long simTime) {
        this.simTime = simTime;
    }

    public abstract String getEventName();

    @Override
    public int compareTo(Event other) {
        return Long.compare(this.simTime, other.simTime);
    }

    public long getSimTime() {
        return simTime;
    }
}
