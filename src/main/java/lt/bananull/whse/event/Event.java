package lt.bananull.whse.event;

import lt.bananull.whse.simulator.Simulator;

public abstract class Event implements Comparable<Event> {

    private final long simTime;

    public Event(long simTime) {
        this.simTime = simTime;
    }

    public abstract void execute(Simulator simulator);

    @Override
    public int compareTo(Event other) {
        return Long.compare(this.simTime, other.simTime);
    }

    @Override
    public String toString() {
        return "received_event=" + this.getClass().getSimpleName() + ";" + "simulation_time=" + simTime;
    }
}
