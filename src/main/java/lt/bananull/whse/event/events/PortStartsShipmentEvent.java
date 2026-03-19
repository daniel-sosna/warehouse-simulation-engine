package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.util.Map;

import static lt.bananull.whse.simulator.enums.BinStatus.AVAILABLE;

public class PortStartsShipmentEvent extends Event {

    private final String gridId;
    private final String portId;

    public PortStartsShipmentEvent(long simTime, String gridId, String portId) {
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
        shipment.startPicking();

        String selectedBinId = shipment.getPicks().stream()
                .map(pickDto -> state.getBin(pickDto.binId()))
                .filter(bin -> bin.getStatus() == AVAILABLE)
                .map(Bin::getId)
                .findFirst()
                .orElse(shipment.getPicks().getFirst().binId());
        PickDto pick = shipment.getPicks().getFirst();
        BinRequestedAtPortEvent event = new BinRequestedAtPortEvent(getSimTime(), selectedBinId, gridId, portId,
            pick.ean(), pick.qty());
        simulator.enqueueEvent(event);
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "gridId", gridId,
                "portId", portId
        );
    }
}
