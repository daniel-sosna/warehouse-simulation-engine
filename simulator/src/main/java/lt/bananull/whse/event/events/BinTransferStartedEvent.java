package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;

import java.util.List;
import java.util.Map;

public class BinTransferStartedEvent extends Event {

    private final String binId;
    private final String destGridId;

    public BinTransferStartedEvent(long simTime, String binId, String destGridId) {
        super(simTime);
        this.binId = binId;
        this.destGridId = destGridId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        Bin bin = simulator.getState().getBin(binId);

        double mult = simulator.resolveMultiplier(simulator.getParameters().transfersConveyors().durationRandomness());
        int standardRate = simulator.getParameters().getBaseTransferDurationSeconds(bin.getCurrentGridId(), destGridId);
        long duration = Math.round(standardRate * mult);
        long arrivesAt = getSimTime() + duration;

        bin.sendOnConveyorBelt(destGridId);
        simulator.enqueueEvent(new BinTransferCompletedEvent(arrivesAt, duration, binId, destGridId));

        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "binId", binId,
                "destGridId", destGridId
        );
    }
}
