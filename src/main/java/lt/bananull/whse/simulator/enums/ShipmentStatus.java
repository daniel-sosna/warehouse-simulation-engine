package lt.bananull.whse.simulator.enums;

/**
 * Lifecycle of a Shipment through the warehouse.
 * RECEIVED → ROUTED → CONSOLIDATION → READY → PICKING → PACKED → SHIPPED
 */
public enum ShipmentStatus {
    /** Initial state — order received, not yet assigned to a grid or port. */
    RECEIVED,
    /** Router has assigned the shipment to a grid with bin picks; quantities are reserved in bins. */
    ROUTED,
    /** Items span multiple grids; bins from source grids are being transferred to the destination grid. */
    CONSOLIDATION,
    /** All required bins are at the destination grid; shipment is awaiting port assignment. */
    READY,
    /** Port is actively picking items from bins one at a time. */
    PICKING,
    /** All items are packed; shipment awaits loading onto a truck. */
    PACKED,
    /** Shipment has been loaded and dispatched. */
    SHIPPED
}
