package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;

import java.util.List;
import java.util.Map;

@Slf4j
public class BinArrivesAtPortEvent extends Event {

    private final String gridId;
    private final String binId;
    private final String portId;
    private String shipmentId;
    private Map<String, Integer> binStock;

    public BinArrivesAtPortEvent(long simTime, long duration, String gridId, String portId, String binId) {
        super(simTime, duration);
        this.gridId = gridId;
        this.binId = binId;
        this.portId = portId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        double mult = simulator.resolveMultiplier(simulator.getParameters().pickingThroughput().randomness());
        int standardRate = simulator.getParameters().pickingThroughput().standard();
        long duration = Math.round((3600.0 / standardRate) * mult);
        long pickDoneAt = getSimTime() + duration;

        binStock = simulator.getState().getBin(binId).getStock();
        shipmentId = simulator.getState().getPort(portId).getActiveShipmentId();
        simulator.enqueueEvent(new BinPickCompletedEvent(pickDoneAt, shipmentId, gridId, portId, binId, duration));

        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "binId", binId,
                "gridId", gridId,
                "portId", portId,
                "shipmentId", shipmentId,
                "binStock", binStock
        );
    }
}
