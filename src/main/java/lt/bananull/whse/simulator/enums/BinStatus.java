package lt.bananull.whse.simulator.enums;

/**
 * Physical status of a Bin within the warehouse.
 */
public enum BinStatus {
    /** Bin is stored in its grid and can be requested. */
    AVAILABLE,
    /** Bin has been claimed by a port; other ports enter a FCFS waiting list. */
    RESERVED,
    /** Bin is in transit on a conveyor belt (moving to a port or being transferred between grids). */
    OUTSIDE
}
