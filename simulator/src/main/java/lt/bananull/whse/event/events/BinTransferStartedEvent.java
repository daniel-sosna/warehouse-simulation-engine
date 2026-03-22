package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.SimulationParameters;
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
        String currGridId = bin.getCurrentGridId();
        bin.sendOnConveyorBelt();
        double mult = simulator.resolveMultiplier(
            simulator.getParameters().transfersConveyors().durationRandomness()
        );
        long durationSec = simulator.getParameters().getTransferDurationSeconds(currGridId, destGridId, mult);
        long arrivesAt = getSimTime() + durationSec;
        return List.of(new BinTransferCompletedEvent(arrivesAt, shipmentId, binId, destGridId));
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
