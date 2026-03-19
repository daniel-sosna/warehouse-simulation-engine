package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;

import java.util.Map;

@Slf4j
public class BinPickCompletedEvent extends Event {

    private final String gridId;
    private final String portId;
    private final String binId;
    private final String shipmentId;
    private final long duration;

    public BinPickCompletedEvent(long simTime, String shipmentId, String gridId,  String portId, String binId, long duration) {
        super(simTime);
        this.shipmentId = shipmentId;
        this.binId = binId;
        this.portId = portId;
        this.gridId = gridId;
        this.duration = duration;
    }

    @Override
    public void execute(Simulator simulator) {

        // Todo:
        // - decrement stock in state
        // - mark shipment Packed if all items picked

        Bin bin = simulator.getState().getBin(binId);
        bin.release();
        simulator.enqueueEvent(new ShipmentPackedEvent(getSimTime(), shipmentId, gridId, portId, duration));

    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "shipmentId", shipmentId,
                "binId", binId
        );
    }
}
