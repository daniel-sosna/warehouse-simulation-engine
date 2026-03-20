package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.Map;

@Slf4j
public class BinPickCompletedEvent extends Event {

    private final String gridId;
    private final String portId;
    private final String binId;
    private final String shipmentId;

    public BinPickCompletedEvent(long simTime, String shipmentId, String gridId,  String portId, String binId, long duration) {
        super(simTime, duration);
        this.shipmentId = shipmentId;
        this.binId = binId;
        this.portId = portId;
        this.gridId = gridId;
    }

    @Override
    public void execute(Simulator simulator) {

        // Todo:
        // - decrement stock in state
        // - mark shipment Packed if all items picked

        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.incrementPickIndex();
        Bin bin = simulator.getState().getBin(binId);
        bin.release();

        if (!shipment.isFullyPicked()) {
            PickDto nextAvailablePick = shipment.getNextUnpickedPick(simulator.getState());
            if (nextAvailablePick != null) {
                simulator.enqueueEvent(new BinRequestedAtPortEvent(getSimTime(),
                    nextAvailablePick.binId(), gridId, portId, nextAvailablePick.ean(), nextAvailablePick.qty(), shipmentId));
            }
        } else {
            simulator.enqueueEvent(new ShipmentPackedEvent(
                getSimTime(), shipmentId, gridId, portId, getDuration()));
        }

    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "shipmentId", shipmentId,
                "binId", binId
        );
    }
}
