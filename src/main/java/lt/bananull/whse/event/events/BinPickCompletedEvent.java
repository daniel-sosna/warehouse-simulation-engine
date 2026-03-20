package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.SimulationState;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.Map;

import static lt.bananull.whse.simulator.enums.PortStatus.IDLE;

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
        Shipment shipment = simulator.getState().getShipment(shipmentId);
        shipment.addPickedBin(binId);

        SimulationState state = simulator.getState();
        Bin bin = state.getBin(binId);
        bin.deductStock(bin.getReservedItems());
        bin.release();
        if (shipment.isFullyPicked()) {
            simulator.enqueueEvent(new ShipmentPackedEvent(getSimTime(), shipmentId, gridId, portId, getDuration()));
        }

        // Check the bin's port queue: poll until an IDLE port is found; skip non-IDLE ports.
        String nextPortId = bin.pollPort();
        while (nextPortId != null) {
            Port waitingPort = state.getPort(nextPortId);
            if (waitingPort.getStatus() == IDLE) {
                BinRequestedAtPortEvent.tryScheduleFor(gridId, nextPortId, getSimTime(), simulator);
                break;
            }
            nextPortId = bin.pollPort();
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
