package lt.bananull.whse.models;

import java.time.LocalTime;
import java.util.List;
import java.time.LocalTime;

public class Grid {
    private final String id;
    private final List<Shift> shifts;

    public Grid(String id, List<Shift> shifts) {
        this.id = id;
        this.shifts = shifts;
    }

    public String getId() {
        return id;
    }

    public List<Shift> getShifts() {
        return shifts;
    }

    public static class Shift {
        private final LocalTime start;
        private final LocalTime end;
        private final List<Port> ports;

        public Shift(LocalTime start, LocalTime end, List<Port> ports) {
            this.start = start;
            this.end = end;
            this.ports = ports;
        }

        public LocalTime getStart() {
            return start;
        }

        public LocalTime getEnd() {
            return end;
        }

        public List<Port> getPorts() {
            return ports;
        }
    }

    public static class Port {
        private final String id;
        private final List<String> handlingFlags;

        public Port(String id, List<String> handlingFlags) {
            this.id = id;
            this.handlingFlags = handlingFlags;
        }

        public String getId() {
            return id;
        }

        public List<String> getHandlingFlags() {
            return handlingFlags;
        }
    }
}