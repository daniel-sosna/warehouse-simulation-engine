package lt.bananull.whse.simulator.entity;

import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.simulator.SimulationParameters;

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

    private SimulationState(
            Map<String, Shipment> shipments,
            Map<String, Bin> bins,
            Map<String, Grid> grids
    ) {
        this(shipments, bins, grids, flatPorts(grids));
    }

    private static Map<String, Port> flatPorts(Map<String, Grid> grids) {
        Map<String, Port> ports = new HashMap<>();
        grids.values().forEach(g -> ports.putAll(g.getPorts()));
        return ports;
    }

    public static SimulationState from(SimulationStateDto dto, SimulationParameters parameters) {
        Map<String, Shipment> shipments = new HashMap<>();
        dto.shipments().forEach(s -> shipments.put(s.id(), Shipment.from(s)));

        Map<String, Bin> bins = new HashMap<>();
        dto.bins().forEach(b -> bins.put(b.id(), Bin.from(b)));

        Map<String, Grid> grids = new HashMap<>();
        dto.grids().forEach(g -> grids.put(g.id(), Grid.from(g, parameters.gridBinDelivery().portQueueCapacity())));

        return new SimulationState(shipments, bins, grids);
    }

    @Override
    public Map<String, Shipment> shipments() { return Collections.unmodifiableMap(shipments); }

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
        return "SimulationState{shipments=%d, bins=%d, grids=%d}"
                .formatted(shipments.size(), bins.size(), grids.size());
    }
}
