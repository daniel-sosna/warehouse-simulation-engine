package lt.bananull.whse.simulator;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Simulation parameters loaded from {@code parameters.json} in the dataset directory.
 * All components default to sensible values so the file is fully optional.
 * Missing JSON fields are null-coalesced to defaults in each record's compact constructor.
 */
public record SimulationParameters(
        @JsonProperty("pickingThroughput") PickingThroughput pickingThroughput,
        @JsonProperty("gridBinDelivery") GridBinDelivery gridBinDelivery,
        @JsonProperty("transfersConveyors") TransfersConveyors transfersConveyors,
        @JsonProperty("truckArrivalSchedules") TruckArrivalSchedules truckArrivalSchedules
) {
    public SimulationParameters {
        if (pickingThroughput == null)   pickingThroughput   = PickingThroughput.defaults();
        if (gridBinDelivery == null)     gridBinDelivery     = GridBinDelivery.defaults();
        if (transfersConveyors == null)  transfersConveyors  = TransfersConveyors.defaults();
        if (truckArrivalSchedules == null) truckArrivalSchedules = TruckArrivalSchedules.defaults();
    }

    /**
     * Returns a {@code SimulationParameters} instance populated entirely with default values.
     * Null arguments are intentional: each nested record's compact constructor fills in its own defaults.
     */
    public static SimulationParameters defaults() {
        return new SimulationParameters(null, null, null, null);
    }

    // -------------------------------------------------------------------------
    // Nested parameter records
    // -------------------------------------------------------------------------

    public record PickingThroughput(
            /** Standard items picked per hour. */
            @JsonProperty("standard") Integer standard,
            /** Fragile items picked per hour. */
            @JsonProperty("fragile") Integer fragile,
            /** Randomness multiplier applied to each pick duration. */
            @JsonProperty("randomness") Randomness randomness
    ) {
        public PickingThroughput {
            if (standard == null)   standard  = 140;
            if (fragile == null)    fragile   = 70;
            if (randomness == null) randomness = new Randomness(0.8, 1.2);
        }

        public static PickingThroughput defaults() {
            return new PickingThroughput(null, null, null);
        }
    }

    public record GridBinDelivery(
            /** Maximum number of shipments that can queue at a port. */
            @JsonProperty("portQueueCapacity") Integer portQueueCapacity,
            /**
             * Average bin delivery time (in seconds) from each grid to its port.
             * Key: grid ID (e.g. "AS1"), value: seconds.
             */
            @JsonProperty("deliveryTimes") Map<String, Integer> deliveryTimes,
            /** Randomness multiplier applied to each bin delivery duration. */
            @JsonProperty("randomness") Randomness randomness
    ) {
        public GridBinDelivery {
            if (portQueueCapacity == null) portQueueCapacity = 20;
            if (deliveryTimes == null)     deliveryTimes     = Map.of("AS1", 6, "AS2", 4, "AS3", 5);
            if (randomness == null)        randomness        = new Randomness(0.8, 1.2);
        }

        public static GridBinDelivery defaults() {
            return new GridBinDelivery(null, null, null);
        }
    }

    public record TransfersConveyors(
            /** Transfer specifications between grid pairs. */
            @JsonProperty("durations") List<TransferDuration> durations,
            /** Randomness multiplier applied to each transfer duration. */
            @JsonProperty("durationRandomness") Randomness durationRandomness,
            /** Randomness multiplier applied to conveyor throughput. */
            @JsonProperty("throughputRandomness") Randomness throughputRandomness
    ) {
        public TransfersConveyors {
            if (durations == null) durations = List.of(
                    new TransferDuration("AS1", "AS2", 60,  1000),
                    new TransferDuration("AS1", "AS3", 240, 800),
                    new TransferDuration("AS2", "AS3", 360, 600)
            );
            if (durationRandomness == null)  durationRandomness  = new Randomness(0.8, 1.2);
            if (throughputRandomness == null) throughputRandomness = new Randomness(0.9, 1.1);
        }

        public static TransfersConveyors defaults() {
            return new TransfersConveyors(null, null, null);
        }
    }

    /** One-way transfer specification between two grids. */
    public record TransferDuration(
            @JsonProperty("from") String from,
            @JsonProperty("to") String to,
            /** Transfer duration in seconds. */
            @JsonProperty("duration") int duration,
            /** Conveyor throughput in bins per hour. */
            @JsonProperty("throughput") int throughput
    ) {}

    public record TruckArrivalSchedules(
            @JsonProperty("schedules") List<TruckArrivalSchedule> schedules
    ) {
        public TruckArrivalSchedules {
            if (schedules == null) schedules = List.of();
        }

        public static TruckArrivalSchedules defaults() {
            return new TruckArrivalSchedules(null);
        }
    }

    public record TruckArrivalSchedule(
            @JsonProperty("sortingDirection") String sortingDirection,
            /** Pull times as "HH:mm" strings. */
            @JsonProperty("pullTimes") List<String> pullTimes,
            /** Weekday names (e.g. "Monday"). */
            @JsonProperty("weekdays") List<String> weekdays
    ) {}

    /** Randomness multiplier range applied to a duration or throughput. */
    public record Randomness(
            @JsonProperty("min") double min,
            @JsonProperty("max") double max
    ) {}
}

