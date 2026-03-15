package lt.bananull.whse.simulator.entity;

import lt.bananull.whse.load.dto.BinDto;
import lt.bananull.whse.simulator.enums.BinStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simulation entity representing a physical storage bin.
 */
public class Bin {

    private final String id;
    private String currentGridId;
    private final Map<String, Integer> stock;
    private BinStatus status;
    private String reservedForPortId;
    private final Map<String, Integer> reservedItems;

    public Bin(String id, String currentGridId, Map<String, Integer> stock) {
        this.id = id;
        this.currentGridId = currentGridId;
        this.stock = new HashMap<>(stock);
        this.reservedItems = new HashMap<>();
        this.status = BinStatus.AVAILABLE;
    }

    public static Bin from(BinDto dto) {
        Map<String, Integer> stock = new HashMap<>();
        dto.itemsInBin().forEach((ean, binItem) -> stock.put(ean, binItem.quantity()));
        return new Bin(dto.id(), dto.currentGridLocation(), stock);
    }

    public String getId() { return id; }

    public String getCurrentGridId() { return currentGridId; }

    public Map<String, Integer> getStock() { return Collections.unmodifiableMap(stock); }

    public BinStatus getStatus() { return status; }

    public String getReservedForPortId() { return reservedForPortId; }

    public Map<String, Integer> getReservedItems() { return Collections.unmodifiableMap(reservedItems); }

    public Map<String, Integer> getAvailableStock() {
        Map<String, Integer> availableStock = new HashMap<>(stock);

        for (Map.Entry<String, Integer> entry : reservedItems.entrySet()) {
            String ean = entry.getKey();
            int reservedQty = entry.getValue();
            int remaining = availableStock.getOrDefault(ean, 0) - reservedQty;

            if (remaining <= 0) {
                availableStock.remove(ean);
            } else {
                availableStock.put(ean, remaining);
            }
        }

        return Collections.unmodifiableMap(availableStock);
    }

    /**
     * Deducts multiple EAN quantities from this bin's stock.
     * All deductions are validated first and then applied.
     *
     * @throws IllegalArgumentException if requested quantities are invalid or insufficient stock is present.
     */
    public void deductStock(Map<String, Integer> itemsToDeduct) {
        if (itemsToDeduct == null || itemsToDeduct.isEmpty()) {
            throw new IllegalArgumentException("Items to deduct must be provided for bin %s".formatted(id));
        }

        for (Map.Entry<String, Integer> entry : itemsToDeduct.entrySet()) {
            String ean = entry.getKey();
            Integer qty = entry.getValue();

            if (qty == null || qty <= 0) {
                throw new IllegalArgumentException(
                        "Deduct quantity must be > 0 for EAN %s in bin %s".formatted(ean, id));
            }

            int current = stock.getOrDefault(ean, 0);
            if (current < qty) {
                throw new IllegalArgumentException(
                        "Bin %s has only %d units of EAN %s; cannot deduct %d".formatted(id, current, ean, qty));
            }
        }

        for (Map.Entry<String, Integer> entry : itemsToDeduct.entrySet()) {
            String ean = entry.getKey();
            int qty = entry.getValue();
            int remaining = stock.get(ean) - qty;

            if (remaining == 0) {
                stock.remove(ean);
            } else {
                stock.put(ean, remaining);
            }
        }
    }

    public void setCurrentGridId(String currentGridId) {
        this.currentGridId = currentGridId;
    }

    public void setStatus(BinStatus status) {
        this.status = status;
    }

    /**
     * Reserves this bin for a port and records exact item quantities that will be picked.
     *
     * @throws IllegalArgumentException if reserved quantities are invalid or exceed stock.
     */
    public void reserveForPort(String portId, Map<String, Integer> itemsToReserve) {
        if (itemsToReserve == null || itemsToReserve.isEmpty()) {
            throw new IllegalArgumentException("Reserved items must be provided when reserving bin %s".formatted(id));
        }

        Map<String, Integer> validatedReservedItems = new HashMap<>();
        for (Map.Entry<String, Integer> entry : itemsToReserve.entrySet()) {
            String ean = entry.getKey();
            Integer qty = entry.getValue();

            if (qty == null || qty <= 0) {
                throw new IllegalArgumentException(
                        "Reserved quantity must be > 0 for EAN %s in bin %s".formatted(ean, id));
            }

            int availableQty = stock.getOrDefault(ean, 0);
            if (qty > availableQty) {
                throw new IllegalArgumentException(
                        "Bin %s has only %d units of EAN %s; cannot reserve %d"
                                .formatted(id, availableQty, ean, qty));
            }

            validatedReservedItems.put(ean, qty);
        }

        this.reservedForPortId = portId;
        this.reservedItems.clear();
        this.reservedItems.putAll(validatedReservedItems);
        this.status = BinStatus.RESERVED;
    }

    public void release() {
        this.reservedForPortId = null;
        this.reservedItems.clear();
        this.status = BinStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return "Bin{id='%s', grid='%s', status=%s, stock=%s, reservedItems=%s}"
                .formatted(id, currentGridId, status, stock, reservedItems);
    }
}
