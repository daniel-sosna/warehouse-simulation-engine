package lt.bananull.whse.simulator.enums;

/**
 * Operational status of a Packing Port.
 */
public enum PortStatus {
    /** Port is outside its active shift or on a break; accepts no shipments. */
    CLOSED,
    /** Port is within its active shift and waiting for its next shipment. */
    IDLE,
    /** Port is actively picking items from a bin or packing a shipment. */
    BUSY,
    /** Shift ended or a break started while Busy; port finishes current shipment then closes. */
    PENDING_CLOSE
}
