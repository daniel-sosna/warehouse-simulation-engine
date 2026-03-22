package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.util.List;
import java.util.Map;

public class PortStartsShipmentEvent extends Event {

    private final String gridId;
    private final String portId;
    private String shipmentId;

    public PortStartsShipmentEvent(long simTime, String gridId, String portId) {
        super(simTime);
        this.gridId = gridId;
        this.portId = portId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        SimulationState state = simulator.getState();

        Port port = state.getPort(portId);
        shipmentId = port.startNextShipment();

        Shipment shipment = state.getShipment(shipmentId);
        shipment.startPicking();

        for (PickDto pick : shipment.getPicks()) {
            state.getBin(pick.binId()).reserveItem(pick.ean(), pick.qty());
        }

        BinRequestedAtPortEvent event = BinRequestedAtPortEvent.scheduleForPort(gridId, portId, getSimTime(), simulator);
        if (event != null) {
            return List.of(event);
        }

        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "gridId", gridId,
                "portId", portId,
                "shipmentId", shipmentId
        );
    }
}
