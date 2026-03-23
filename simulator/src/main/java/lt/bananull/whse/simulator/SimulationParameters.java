package lt.bananull.whse.simulator;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Simulation parameters loaded from {@code parameters.json} in the dataset directory.
 * All components default to sensible values so the file is fully optional.
 * Missing JSON fields are null-coalesced to defaults in each record's compact constructor.
 */
public record SimulationParameters(
        @JsonProperty("simulationStartTime") Instant simulationStartTime,
        @JsonProperty("simulationEndTime") Instant simulationEndTime,
        @JsonProperty("pickingThroughput") PickingThroughput pickingThroughput,
        @JsonProperty("gridBinDelivery") GridBinDelivery gridBinDelivery,
        @JsonProperty("transfersConveyors") TransfersConveyors transfersConveyors,
        @JsonProperty("truckArrivalSchedules") TruckArrivalSchedules truckArrivalSchedules
) {
    public SimulationParameters {
        if (pickingThroughput == null)     pickingThroughput   = PickingThroughput.defaults();
        if (gridBinDelivery == null)       gridBinDelivery     = GridBinDelivery.defaults();
        if (transfersConveyors == null)    transfersConveyors  = TransfersConveyors.defaults();
        if (truckArrivalSchedules == null) truckArrivalSchedules = TruckArrivalSchedules.defaults();
    }

    /**
     * Returns a {@code SimulationParameters} instance populated entirely with default values.
     * Null arguments are intentional: each nested record's compact constructor fills in its own defaults.
     */
    public static SimulationParameters defaults() {
        return new SimulationParameters(null, null, null, null, null, null);
    }

    // -------------------------------------------------------------------------
    // Nested parameter records
    // -------------------------------------------------------------------------

    /**
     * Picking throughput configuration.
     * {@code standard} — Standard items picked per hour (default 140).
     * {@code fragile} — Fragile items picked per hour (default 70).
     * {@code randomness} — Multiplier range applied to each pick duration (default 0.8–1.2).
     */
    public record PickingThroughput(
            @JsonProperty("standard") Integer standard,
            @JsonProperty("fragile") Integer fragile,
            @JsonProperty("dropp") Integer dropp,
            @JsonProperty("randomness") Randomness randomness
    ) {
        public PickingThroughput {
            if (standard == null)   standard  = 140;
            if (fragile == null)    fragile   = 70;
            if (dropp == null)      dropp     = 100;
            if (randomness == null) randomness = new Randomness(0.8, 1.2);
        }

        public static PickingThroughput defaults() {
            return new PickingThroughput(null, null,null, null);
        }
    }

    /**
     * Grid-to-port bin delivery configuration.
     * {@code portQueueCapacity} — Max shipments that can queue at a port (default 20).
     * {@code deliveryTimes} — Average delivery time in seconds from each grid ID to its port.
     * {@code randomness} — Multiplier range applied to each delivery duration (default 0.8–1.2).
     */
    public record GridBinDelivery(
            @JsonProperty("portQueueCapacity") Integer portQueueCapacity,
            @JsonProperty("deliveryTimes") Map<String, Integer> deliveryTimes,
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

    /**
     * Inter-grid conveyor transfer configuration.
     * {@code durations} — Transfer specs between grid pairs.
     * {@code durationRandomness} — Multiplier range applied to transfer durations (default 0.8–1.2).
     * {@code throughputRandomness} — Multiplier range applied to conveyor throughput (default 0.9–1.1).
     */
    public record TransfersConveyors(
            @JsonProperty("durations") List<TransferDuration> durations,
            @JsonProperty("durationRandomness") Randomness durationRandomness,
            @JsonProperty("throughputRandomness") Randomness throughputRandomness
    ) {
        public TransfersConveyors {
            if (durations == null)            durations = List.of(
                    new TransferDuration("AS1", "AS2", 60,  1000),
                    new TransferDuration("AS1", "AS3", 240, 800),
                    new TransferDuration("AS2", "AS3", 360, 600)
            );
            if (durationRandomness == null)   durationRandomness  = new Randomness(0.8, 1.2);
            if (throughputRandomness == null) throughputRandomness = new Randomness(0.9, 1.1);
        }

        public static TransfersConveyors defaults() {
            return new TransfersConveyors(null, null, null);
        }
    }

    /**
     * Two-way transfer specification between two grids.
     * {@code duration} — Transfer duration in seconds.
     * {@code throughput} — Conveyor throughput in bins per hour.
     */
    public record TransferDuration(
            @JsonProperty("from") String from,
            @JsonProperty("to") String to,
            @JsonProperty("duration") int duration,
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

    /**
     * A single truck arrival schedule entry.
     * {@code pullTimes} — Pull times as "HH:mm" strings.
     * {@code weekdays} — Weekday names (e.g. "Monday") on which this schedule applies.
     */
    public record TruckArrivalSchedule(
            @JsonProperty("sortingDirection") String sortingDirection,
            @JsonProperty("pullTimes") List<String> pullTimes,
            @JsonProperty("weekdays") List<String> weekdays
    ) {}

    /** Randomness multiplier range applied to a duration or throughput. */
    public record Randomness(
            @JsonProperty("min") double min,
            @JsonProperty("max") double max
    ) {}

    public int getBaseTransferDurationSeconds(String fromGridId, String toGridId) {
        if (fromGridId.equals(toGridId)) return 0;

        return transfersConveyors().durations().stream()
            .filter(d ->
                    (d.from().equals(fromGridId) && d.to().equals(toGridId))
                        || (d.from().equals(toGridId) && d.to().equals(fromGridId))
            )
            .findFirst()
            .map(TransferDuration::duration)
            .orElseThrow(() -> new IllegalArgumentException(
                "No transfer duration configured for pair " + fromGridId + " <-> " + toGridId
            ));
    }
}
