package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.Map;

@Slf4j
public class BinArrivesAtPortEvent extends Event {

    private final String gridId;
    private final String binId;
    private final String portId;

    public BinArrivesAtPortEvent(long simTime, String gridId, String portId, String binId) {
        super(simTime);
        this.gridId = gridId;
        this.binId = binId;
        this.portId = portId;
    }

    @Override
    public void execute(Simulator simulator) {
        double mult = simulator.sampleMultiplier(simulator.getParameters().pickingThroughput().randomness());
        int standardRate = simulator.getParameters().pickingThroughput().standard();
        long pickSeconds = Math.round((3600.0 / standardRate) * mult);
        long pickDoneAt = getSimTime() + pickSeconds;
        Port port = simulator.getState().getPort(gridId, portId);
        Shipment shipment = simulator.getState().getShipment(port.)
        simulator.enqueueEvent(new BinPickCompletedEvent(pickDoneAt, , pickSeconds));
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "binId", binId
        );
    }
}
