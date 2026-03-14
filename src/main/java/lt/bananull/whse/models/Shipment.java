package lt.bananull.whse.models;

import java.time.Instant;
import java.util.Map;

public class Shipment {
    private final String id;
    private final Map<String, Integer> items;
    private final Instant shipmentDate;

    public Shipment(String id, Map<String, Integer> items, Instant shipmentDate) {
        this.id = id;
        this.items = items;
        this.shipmentDate = shipmentDate;
    }

    public String getId() {
        return id;
    }

    public Map<String, Integer> getItems() {
        return items;
    }

    public Instant getShipmentDate() {
        return shipmentDate;
    }
}