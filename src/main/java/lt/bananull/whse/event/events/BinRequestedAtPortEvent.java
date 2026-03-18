package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;

import java.util.Map;

import static lt.bananull.whse.simulator.enums.BinStatus.AVAILABLE;

public class BinRequestedAtPortEvent extends Event {

    private final String binId;
    private final String gridId;
    private final String portId;
    private final String ean;
    private final int qty;

    public BinRequestedAtPortEvent(long simTime, String binId, String gridId, String portId, String ean, int qty) {
        super(simTime);
        this.binId = binId;
        this.gridId = gridId;
        this.portId = portId;
        this.ean = ean;
        this.qty = qty;
    }

    @Override
    public void execute(Simulator simulator) {
        Bin bin = simulator.getState().getBin(binId);
        if (bin.getStatus() == AVAILABLE) {
            bin.reserveForPort(portId, Map.of(ean, qty));
            double mult = simulator.sampleMultiplier(simulator.getParameters().gridBinDelivery().randomness());
            long estSimTime = getSimTime() +
                Math.round((3600.0 / simulator.getParameters().gridBinDelivery().deliveryTimes().get(gridId).doubleValue()) * mult);
            simulator.enqueueEvent(new BinArrivesAtPortEvent(estSimTime, gridId, portId, binId));

        }
        // else: TODO: put into a queue of the bin

    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
