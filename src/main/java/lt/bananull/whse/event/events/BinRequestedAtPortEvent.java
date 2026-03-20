package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.util.Map;

import static lt.bananull.whse.simulator.enums.BinStatus.AVAILABLE;
import static lt.bananull.whse.simulator.enums.PortStatus.IDLE;

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

    /**
     * Tries to find an available bin for the next queued shipment of the given port and schedule
     * this event. If no bin is currently available, the port is enqueued on every candidate bin so
     * that it is notified when one becomes free.
     *
     * <p>The port must be {@link lt.bananull.whse.simulator.enums.PortStatus#IDLE} and have at
     * least one shipment in its queue when this method is called.
     *
     * @param gridId  grid the port belongs to.
     * @param portId  port waiting for a bin.
     * @param simTime current simulation time to use when scheduling the event.
     * @param simulator the active simulator instance.
     */
    public static void tryScheduleFor(String gridId, String portId, long simTime, Simulator simulator) {
        SimulationState state = simulator.getState();
        Port port = state.getPort(gridId, portId);
        String nextShipmentId = port.peekNextShipmentId();
        if (nextShipmentId == null) return;

        Shipment shipment = state.getShipment(nextShipmentId);
        String binId = port.findAvailableBin(shipment, state::getBin);

        if (binId != null) {
            PickDto pick = shipment.getPicks().stream()
                    .filter(p -> p.binId().equals(binId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Available bin %s is not associated with picks of shipment %s — potential state inconsistency"
                                    .formatted(binId, nextShipmentId)));
            simulator.enqueueEvent(new BinRequestedAtPortEvent(simTime, binId, gridId, portId, pick.ean(), pick.qty()));
        } else {
            for (PickDto pick : shipment.getPicks()) {
                state.getBin(pick.binId()).enqueuePort(gridId, portId);
            }
        }
    }

    @Override
    public void execute(Simulator simulator) {
        SimulationState state = simulator.getState();
        Bin bin = state.getBin(binId);
        Port port = state.getPort(gridId, portId);

        if (bin.getStatus() == AVAILABLE && port.getStatus() == IDLE) {
            port.startNextShipment();
            Shipment shipment = state.getShipment(port.getActiveShipmentId());
            shipment.startPicking();

            bin.reserveForPort(portId, Map.of(ean, qty));

            double mult = simulator.resolveMultiplier(simulator.getParameters().gridBinDelivery().randomness());
            int standardRate = simulator.getParameters().gridBinDelivery().deliveryTimes().get(gridId);
            long duration = Math.round(standardRate * mult);
            long arriveAt = getSimTime() + duration;

            simulator.enqueueEvent(new BinArrivesAtPortEvent(arriveAt, duration, gridId, portId, binId));
        } else if (bin.getStatus() != AVAILABLE && port.getStatus() == IDLE) {
            // Bin was claimed by another port between scheduling and execution; wait in bin's queue.
            bin.enqueuePort(gridId, portId);
        }
        // else: port is no longer IDLE (it already received another bin) – nothing to do.
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of();
    }
}
