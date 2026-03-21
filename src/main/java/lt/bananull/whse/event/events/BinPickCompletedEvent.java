package lt.bananull.whse.event.events;

import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.SimulationState;
import lt.bananull.whse.simulator.entity.Shipment;

import java.util.ArrayList;
import java.util.List;
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
    public List<Event> execute(Simulator simulator) {
        SimulationState state = simulator.getState();
        Shipment shipment = state.getShipment(shipmentId);
        Bin bin = state.getBin(binId);
        Port port = state.getPort(portId);

        shipment.addPickedBin(binId);
        bin.deductStock(shipment.getItemsForBin(binId));
        bin.release();
        port.releaseBin();

        List<Event> events = new ArrayList<>();

        // Check the bin's port queue: poll until a waiting port without an assigned bin is found.
        String nextPortId = bin.pollPort();
        while (nextPortId != null) {
            Port waitingPort = state.getPort(nextPortId);
            if (waitingPort.getCurrentBinId() == null) {
                events.add(new BinRequestedAtPortEvent(getSimTime(), binId, gridId, nextPortId));
                break;
            }
            nextPortId = bin.pollPort();
        }

        // Check the port's shipment progress: if not fully packed, request next bin for this port.
        if (shipment.isFullyPicked()) {
            events.add(new ShipmentPackedEvent(getSimTime(), shipmentId, gridId, portId, getDuration()));
        } else {
            BinRequestedAtPortEvent event = BinRequestedAtPortEvent.scheduleForPort(gridId, portId, getSimTime(), simulator);
            if (event != null) {
                events.add(event);
            }
        }

        return events;
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "shipmentId", shipmentId,
                "binId", binId
        );
    }
}
