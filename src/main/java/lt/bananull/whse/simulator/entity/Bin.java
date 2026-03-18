package lt.bananull.whse.simulator.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lt.bananull.whse.load.dto.BinDto;
import lt.bananull.whse.simulator.enums.BinStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simulation entity representing a physical storage bin.
 */
@Getter
public class Bin {

    private final String id;
    @Getter(AccessLevel.NONE)
    private final Map<String, Integer> stock;
    private String currentGridId;
    private BinStatus status = BinStatus.AVAILABLE;
    private String reservedForPortId;
    @Getter(AccessLevel.NONE)
    private final Map<String, Integer> reservedItems = new HashMap<>();

    private Bin(String id, String currentGridId, Map<String, Integer> stock) {
        this.id = id;
        this.currentGridId = currentGridId;
        this.stock = new HashMap<>(stock);
    }

    public static Bin from(BinDto dto) {
        Map<String, Integer> stock = new HashMap<>();
        dto.itemsInBin().forEach((ean, binItemDto) -> stock.put(ean, binItemDto.quantity()));
        return new Bin(dto.id(), dto.currentGridLocation(), stock);
    }

    public Map<String, Integer> getStock() { return Collections.unmodifiableMap(stock); }

    public Map<String, Integer> getReservedItems() { return Collections.unmodifiableMap(reservedItems); }

    public Map<String, Integer> getAvailableStock() {
        Map<String, Integer> availableStock = new HashMap<>(stock);

        for (Map.Entry<String, Integer> entry : reservedItems.entrySet()) {
            String ean = entry.getKey();
            int remaining = availableStock.get(ean) - entry.getValue();

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

        // Ensure there are enough items in stock
        for (Map.Entry<String, Integer> entry : itemsToDeduct.entrySet()) {
            String ean = entry.getKey();
            Integer qty = entry.getValue();

            if (qty == null || qty <= 0) {
                throw new IllegalArgumentException(
                        "Deduct quantity must be > 0 for EAN %s in bin %s".formatted(ean, id));
            }

            int current = stock.get(ean);
            if (current < qty) {
                throw new IllegalArgumentException(
                        "Bin %s has only %d units of EAN %s; cannot deduct %d".formatted(id, current, ean, qty));
            }
        }

        for (Map.Entry<String, Integer> entry : itemsToDeduct.entrySet()) {
            String ean = entry.getKey();
            int remaining = stock.get(ean) - entry.getValue();

            if (remaining == 0) {
                stock.remove(ean);
            } else {
                stock.put(ean, remaining);
            }
        }
    }

    /**
     * Reserves this bin for a port and records exact item quantities that will be picked.
     *
     * @throws IllegalArgumentException if reserved quantities are invalid or exceed stock.
     */
    public void reserveForPort(String portId, Map<String, Integer> itemsToReserve) {
        // TODO: find out when to reserve a bin. If when shipment goes to port queue (no rerouting), then we need to reserve items earlier
        if (portId == null || portId.isEmpty()) {
            throw new IllegalArgumentException("Port ID must be provided when reserving bin %s".formatted(id));
        }
        if (itemsToReserve == null || itemsToReserve.isEmpty()) {
            throw new IllegalArgumentException("Reserved items must be provided when reserving bin %s".formatted(id));
        }
        if (status == BinStatus.RESERVED) {
            throw new IllegalStateException("Bin %s is already reserved for port %s".formatted(id, reservedForPortId));
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

    public void sendOnConveyorBelt() {
        this.currentGridId = null;
        this.status = BinStatus.OUTSIDE;
    }

    public void arriveAtGrid(String gridId) {
        if (gridId == null || gridId.isEmpty()) {
            throw new IllegalArgumentException("Grid ID must be provided when bin %s arrives".formatted(id));
        }

        this.currentGridId = gridId;
        this.status = BinStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return "Bin{id='%s', grid='%s', status=%s, stock=%s, reservedItems=%s}"
                .formatted(id, currentGridId, status, stock, reservedItems);
    }
}
