package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

public class ShipmentReceivedEvent extends Event {

    private final Shipment shipment;

    public ShipmentReceivedEvent(long simTime, Shipment shipment) {
        super(simTime);
        this.shipment = shipment;
    }

    @Override
    public void execute(Simulator simulator) {
        // This event only used as wrapper for good toString() method
    }

    @Override
    public String toString() {
        // TODO logs fixed in other issue
        return super.toString() + ";shipment_id=" + shipment.getId();
    }
}
