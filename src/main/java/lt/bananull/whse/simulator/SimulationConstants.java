package lt.bananull.whse.simulator;

public final class SimulationConstants {

    private SimulationConstants() {}

    /** Fallback bin-delivery time (seconds) used when a grid ID is not found in the configured delivery times. */
    public static final int DEFAULT_DELIVERY_SECONDS = 6;

    /** How often (in simulation seconds) the router is invoked. */
    public static final int ROUTER_INTERVAL_SECONDS = 900;
}
