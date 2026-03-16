package lt.bananull.whse.simulator;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulation parameters loaded from {@code parameters.json} in the dataset directory.
 * All fields carry sensible defaults so the file is fully optional.
 */
@Getter
@Setter
public class SimulationParameters {

    @JsonProperty("pickingThroughput")
    private PickingThroughput pickingThroughput = new PickingThroughput();

    @JsonProperty("gridBinDelivery")
    private GridBinDelivery gridBinDelivery = new GridBinDelivery();

    @JsonProperty("transfersConveyors")
    private TransfersConveyors transfersConveyors = new TransfersConveyors();

    @JsonProperty("truckArrivalSchedules")
    private TruckArrivalSchedules truckArrivalSchedules = new TruckArrivalSchedules();

    // -------------------------------------------------------------------------
    // Nested parameter classes
    // -------------------------------------------------------------------------

    @Getter
    @Setter
    public static class PickingThroughput {

        /** Standard items picked per hour. */
        @JsonProperty("standard")
        private int standard = 140;

        /** Fragile items picked per hour. */
        @JsonProperty("fragile")
        private int fragile = 70;

        /** Randomness multiplier applied to each pick duration. */
        @JsonProperty("randomness")
        private Randomness randomness = new Randomness(0.8, 1.2);
    }

    @Getter
    @Setter
    public static class GridBinDelivery {

        /** Maximum number of shipments that can queue at a port. */
        @JsonProperty("portQueueCapacity")
        private int portQueueCapacity = 20;

        /**
         * Average bin delivery time (in seconds) from each grid to its port.
         * Key: grid ID (e.g. "AS1"), value: seconds.
         */
        @JsonProperty("deliveryTimes")
        private Map<String, Integer> deliveryTimes = defaultDeliveryTimes();

        /** Randomness multiplier applied to each bin delivery duration. */
        @JsonProperty("randomness")
        private Randomness randomness = new Randomness(0.8, 1.2);

        private static Map<String, Integer> defaultDeliveryTimes() {
            Map<String, Integer> m = new HashMap<>();
            m.put("AS1", 6);
            m.put("AS2", 4);
            m.put("AS3", 5);
            return m;
        }
    }

    @Getter
    @Setter
    public static class TransfersConveyors {

        /** Transfer specifications between grid pairs. */
        @JsonProperty("durations")
        private List<TransferDuration> durations = defaultDurations();

        /** Randomness multiplier applied to each transfer duration. */
        @JsonProperty("durationRandomness")
        private Randomness durationRandomness = new Randomness(0.8, 1.2);

        /** Randomness multiplier applied to conveyor throughput. */
        @JsonProperty("throughputRandomness")
        private Randomness throughputRandomness = new Randomness(0.9, 1.1);

        private static List<TransferDuration> defaultDurations() {
            List<TransferDuration> list = new ArrayList<>();
            list.add(new TransferDuration("AS1", "AS2", 60, 1000));
            list.add(new TransferDuration("AS1", "AS3", 240, 800));
            list.add(new TransferDuration("AS2", "AS3", 360, 600));
            return list;
        }
    }

    @Getter
    @Setter
    public static class TransferDuration {

        @JsonProperty("from")
        private String from;

        @JsonProperty("to")
        private String to;

        /** One-way transfer duration in seconds. */
        @JsonProperty("duration")
        private int duration;

        /** Conveyor throughput in bins per hour. */
        @JsonProperty("throughput")
        private int throughput;

        /** No-arg constructor required by Jackson. */
        public TransferDuration() {}

        public TransferDuration(String from, String to, int duration, int throughput) {
            this.from = from;
            this.to = to;
            this.duration = duration;
            this.throughput = throughput;
        }
    }

    @Getter
    @Setter
    public static class TruckArrivalSchedules {

        @JsonProperty("schedules")
        private List<TruckArrivalSchedule> schedules = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class TruckArrivalSchedule {

        @JsonProperty("sortingDirection")
        private String sortingDirection;

        /** Pull times as "HH:mm" strings. */
        @JsonProperty("pullTimes")
        private List<String> pullTimes = new ArrayList<>();

        /** Weekday names (e.g. "Monday"). */
        @JsonProperty("weekdays")
        private List<String> weekdays = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Randomness {

        @JsonProperty("min")
        private double min;

        @JsonProperty("max")
        private double max;

        /** No-arg constructor required by Jackson. */
        public Randomness() {}

        public Randomness(double min, double max) {
            this.min = min;
            this.max = max;
        }
    }
}
