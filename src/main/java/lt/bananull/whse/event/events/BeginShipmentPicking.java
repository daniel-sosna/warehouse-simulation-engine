package lt.bananull.whse.event.events;

import lt.bananull.whse.event.Event;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Port;

import java.util.Map;

public class BeginShipmentPicking extends Event {

    private final String portId;
    private final String gridId;

    public BeginShipmentPicking(long simTime, String gridId, String portId) {
        super(simTime);
        this.portId = portId;
        this.gridId = gridId;
    }

    @Override
    public void execute(Simulator simulator) {
        Port port = simulator.getState().getGrid(gridId).getPorts().get(portId);
        port.startNextShipment();
        // TODO: add a way to ge the bins instantly
        Map<String, Integer> items = simulator.getState().getShipment(port.getActiveShipmentId()).getItems();
        for (String item : items.keySet()) {
            Bin bin = simulator.getState().bins().values().stream()
                    .filter(b -> b.getAvailableStock().containsKey(item))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No bin found for EAN %s".formatted(item)));
            String binId = bin.getId();
            // TODO: create and call the event BinRequestedAtPort
            // TODO: also check the quantity and reduce the stock
        }
    }

    @Override
    public Map<String, Object> getData() {
        return Map.of(
                "gridId", gridId,
                "portId", portId
        );
    }
}
