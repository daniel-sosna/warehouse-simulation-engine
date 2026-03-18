package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
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
        Bin selectedBin = shipment.getPicks().stream()
                .map(pickDto -> state.getBin(pickDto.binId()))
                .filter(bin -> bin.getStatus() == AVAILABLE)
                .findFirst()
                .orElse(state.getBin(shipment.getPicks().getFirst().binId()));
        // TODO: create and call the event BinRequestedAtPort
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "gridId", gridId,
                "portId", portId
        );
    }
}
