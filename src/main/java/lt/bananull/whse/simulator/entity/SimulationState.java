package lt.bananull.whse.simulator.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Live simulation state: indexed maps for O(1) lookup of every entity by ID.
 * This is the single source of truth used by all simulator components.
 */
public record SimulationState(
        Map<String, Shipment> shipments,
        Map<String, Bin> bins,
        Map<String, Grid> grids
) {

    public SimulationState(
            Map<String, Shipment> shipments,
            Map<String, Bin> bins,
            Map<String, Grid> grids
    ) {
        this.shipments = new HashMap<>(shipments);
        this.bins = new HashMap<>(bins);
        this.grids = new HashMap<>(grids);
    }

    // Accessors - return unmodifiable views; mutate the entity objects, not these maps

    @Override
    public Map<String, Shipment> shipments() { return Collections.unmodifiableMap(shipments); }

    @Override
    public Map<String, Bin> bins() { return Collections.unmodifiableMap(bins); }

    @Override
    public Map<String, Grid> grids() { return Collections.unmodifiableMap(grids); }

    public Shipment getShipment(String id) {
        return shipments.get(id);
    }

    public Bin getBin(String id) {
        return bins.get(id);
    }

    public Grid getGrid(String id) {
        return grids.get(id);
    }

    @Override
    public String toString() {
        return "SimulationState{shipments=%d, bins=%d, grids=%d}"
                .formatted(shipments.size(), bins.size(), grids.size());
    }
}
