package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.List;
import java.util.Map;

public class BinTransferCompletedEvent extends Event {

    private final String shipmentId;
    private final String binId;
    private final String destGridId;

    public BinTransferCompletedEvent(long simTime, String shipmentId, String binId, String destGridId) {
        super(simTime);
        this.shipmentId = shipmentId;
        this.binId = binId;
        this.destGridId = destGridId;
    }

    @Override
    public List<Event> execute(Simulator simulator) {
        Bin bin = simulator.getState().getBin(binId);
        bin.arriveAtGrid(destGridId);
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.addArrivedBin(binId);
        if (shipment.isConsolidationComplete()) {
            shipment.markReady();
            return List.of(new ShipmentIsReadyEvent(getSimTime(), shipmentId));
        }
        return List.of();
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
