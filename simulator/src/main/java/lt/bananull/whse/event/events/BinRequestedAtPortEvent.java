package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.util.List;
import java.util.Map;

import static lt.bananull.whse.simulator.enums.BinStatus.AVAILABLE;

public class BinRequestedAtPortEvent extends Event {

    private final String binId;
    private final String gridId;
    private final String portId;

    public BinRequestedAtPortEvent(long simTime, String binId, String gridId, String portId) {
        super(simTime);
        this.binId = binId;
        this.gridId = gridId;
        this.portId = portId;
    }

    public static BinRequestedAtPortEvent scheduleForPort(String gridId, String portId, long simTime, Simulator simulator) {
        SimulationState state = simulator.getState();
        Port port = state.getPort(portId);
        Shipment shipment = state.getShipment(port.getActiveShipmentId());

        String binId = shipment.getPicks().stream()
                .map(pick -> state.getBin(pick.binId()))
                .filter(bin -> bin.getStatus() != AVAILABLE)
                .map(Bin::getId)
                .findFirst()
                .orElse(null);

        if (binId != null) {
            return new BinRequestedAtPortEvent(simTime, binId, gridId, portId);
        } else {
            for (PickDto pick : shipment.getPicks()) {
                state.getBin(pick.binId()).enqueuePort(portId);
            }
        }

        return null;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        SimulationState state = simulator.getState();
        Port port = state.getPort(portId);

        port.assignBin(binId);

        double mult = simulator.resolveMultiplier(simulator.getParameters().gridBinDelivery().randomness());
        int standardRate = simulator.getParameters().gridBinDelivery().deliveryTimes().get(gridId);
        long duration = Math.round(standardRate * mult);
        long arriveAt = getSimTime() + duration;

        return List.of(new BinArrivesAtPortEvent(arriveAt, duration, gridId, portId, binId));
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "binId", binId,
                "gridId", gridId,
                "portId", portId
        );
    }
}
