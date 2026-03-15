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
        Map<String, Grid> grids,
        Map<String, Port> ports
) {

    public SimulationState(
            Map<String, Shipment> shipments,
            Map<String, Bin> bins,
            Map<String, Grid> grids,
            Map<String, Port> ports
    ) {
        this.shipments = new HashMap<>(shipments);
        this.bins = new HashMap<>(bins);
        this.grids = new HashMap<>(grids);
        this.ports = new HashMap<>(ports);
    }

    // Accessors - return unmodifiable views; mutate the entity objects, not these maps

    @Override
    public Map<String, Shipment> shipments() { return Collections.unmodifiableMap(shipments); }

    @Override
    public Map<String, Bin> bins() { return Collections.unmodifiableMap(bins); }

    @Override
    public Map<String, Grid> grids() { return Collections.unmodifiableMap(grids); }

    @Override
    public Map<String, Port> ports() { return Collections.unmodifiableMap(ports); }

    public Shipment getShipment(String id) {
        return shipments.get(id);
    }

    public Bin getBin(String id) {
        return bins.get(id);
    }

    public Grid getGrid(String id) {
        return grids.get(id);
    }

    public Port getPort(String id) {
        return ports.get(id);
    }

    @Override
    public String toString() {
        return "SimulationState{shipments=%d, bins=%d, grids=%d, ports=%d}"
                .formatted(shipments.size(), bins.size(), grids.size(), ports.size());
    }
}
