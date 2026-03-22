package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;

import java.util.List;
import java.util.Map;

public class BinTransferStartedEvent extends Event {

    private final String shipmentId;
    private final String binId;
    private final String destGridId;

    public BinTransferStartedEvent(long simTime, String shipmentId, String binId, String destGridId) {
        super(simTime);
        this.shipmentId = shipmentId;
        this.binId = binId;
        this.destGridId = destGridId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        Bin bin = simulator.getState().getBin(binId);
        bin.sendOnConveyorBelt();

        double mult = simulator.resolveMultiplier(simulator.getParameters().transfersConveyors().durationRandomness());
        int standardRate = simulator.getParameters().getBaseTransferDurationSeconds(bin.getCurrentGridId(), destGridId);
        long duration = Math.round(standardRate * mult);
        long arrivesAt = getSimTime() + duration;

        return List.of(new BinTransferCompletedEvent(arrivesAt, shipmentId, binId, destGridId));
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
