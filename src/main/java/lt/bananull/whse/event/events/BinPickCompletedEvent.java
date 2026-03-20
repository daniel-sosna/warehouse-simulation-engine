package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.SimulationState;

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

        // Todo:
        // - decrement stock in state
        // - mark shipment Packed if all items picked

        SimulationState state = simulator.getState();
        Bin bin = state.getBin(binId);
        bin.release();

        // Check the bin's port queue: poll until an IDLE port is found; skip non-IDLE ports.
        while (bin.hasPortsInQueue()) {
            Bin.PortRef portRef = bin.pollPort();
            Port waitingPort = state.getPort(portRef.portId());
            if (waitingPort.getStatus() == IDLE) {
                BinRequestedAtPortEvent.tryScheduleFor(portRef.gridId(), portRef.portId(), getSimTime(), simulator);
                break;
            }
            // Port is no longer IDLE (already being served or closed) – drop it.
        }

        simulator.enqueueEvent(new ShipmentPackedEvent(getSimTime(), shipmentId, gridId, portId, getDuration()));
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "shipmentId", shipmentId,
                "binId", binId
        );
    }
}
