package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;

import java.util.Map;

@Slf4j
public class BinPickCompletedEvent extends Event {

    private final String binId;
    private final String shipmentId;
    private final long duration;

    public BinPickCompletedEvent(long simTime, String shipmentId, String binId, long duration) {
        super(simTime);
        this.shipmentId = shipmentId;
        this.binId = binId;
        this.duration = duration;
    }

    @Override
    public void execute(Simulator simulator) {
        // For now just logging...

        // Todo:
        // - decrement stock in state
        // - mark shipment Packed if all items picked
        // - set bin status Available
        // - trigger simulator.dispatch() to start next waiting assignment
        // - set port status to idle and then start next shipment again

        Bin bin = simulator.getState().getBin(binId);
        bin.release();
        simulator.enqueueEvent(new ShipmentPackedEvent(getSimTime(), shipmentId, duration));

    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "shipmentId", shipmentId,
                "binId", binId
        );
    }
}
