package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Shipment;

public class SendShipmentToQueue extends Event {

    private final Shipment shipment;

    public SendShipmentToQueue(long simTime, Shipment shipment) {
        super(simTime);
        this.shipment = shipment;
    }

    @Override
    public void execute(Simulator simulator) {

    }
}
