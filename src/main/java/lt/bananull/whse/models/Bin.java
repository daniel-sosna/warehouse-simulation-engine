package lt.bananull.whse.models;

import java.util.Map;

public class Bin {
    private final String id;
    private final String gridId;
    private final Map<String, Integer> items;

    public Bin(String id, String gridId, Map<String, Integer> items) {
        this.id = id;
        this.gridId = gridId;
        this.items = items;
    }

    public String getId() {
        return id;
    }

    public String getGridId() {
        return gridId;
    }

    public Map<String, Integer> getItems() {
        return items;
    }
}