package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.event.EventHandler;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.util.Map;

import static lt.bananull.whse.simulator.enums.BinStatus.AVAILABLE;

public class BeginShipmentPickingEvent extends Event {

    private final String gridId;
    private final String portId;

    public BeginShipmentPickingEvent(long simTime, String gridId, String portId) {
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
    }

    @Override
    public void execute(Simulator simulator) {
        SimulationState state = simulator.getState();

        Port port = state.getPort(gridId, portId);
        port.startNextShipment();

        Shipment shipment = state.getShipment(port.getActiveShipmentId());
        String selectedBinId = shipment.getPicks().stream()
                .map(pickDto -> state.getBin(pickDto.binId()))
                .filter(bin -> bin.getStatus() == AVAILABLE)
                .map(Bin::getId)
                .findFirst()
                .orElse(shipment.getPicks().getFirst().binId());
        BinRequestedAtPortEvent event = new BinRequestedAtPortEvent(getSimTime(), selectedBinId, gridId, portId);
        EventHandler.getInstance(simulator).handle(event);
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "gridId", gridId,
                "portId", portId
        );
    }
}
