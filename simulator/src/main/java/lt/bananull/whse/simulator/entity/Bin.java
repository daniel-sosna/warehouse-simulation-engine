package lt.bananull.whse.simulator.entity;

import lombok.AccessLevel;
import lombok.Getter;
import lt.bananull.whse.load.dto.BinDto;
import lt.bananull.whse.simulator.enums.BinStatus;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Simulation entity representing a physical storage bin.
 */
@Getter
public class Bin {

    private final String id;
    @Getter(AccessLevel.NONE)
    private final Map<String, Integer> stock;
    private String currentGridId;
    private boolean isNeededInCurrentGrid =  false;
    private BinStatus status = BinStatus.AVAILABLE;
    private String reservedForPortId;
    @Getter(AccessLevel.NONE)
    private final Map<String, Integer> reservedItems = new HashMap<>();
    @Getter(AccessLevel.NONE)
    private final Queue<String> portQueue = new ArrayDeque<>();

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
     *
     * @throws IllegalArgumentException if requested quantities are invalid.
     */
    public void deductStock(Map<String, Integer> itemsToDeduct) {
        if (itemsToDeduct == null || itemsToDeduct.isEmpty()) {
            throw new IllegalArgumentException("Items to deduct must be provided for bin %s".formatted(id));
        }

        for (Map.Entry<String, Integer> entry : itemsToDeduct.entrySet()) {
            String ean = entry.getKey();
            int remainingStock = stock.get(ean) - entry.getValue();
            int remainingReserved = reservedItems.getOrDefault(ean, 0) - entry.getValue();

            if (remainingStock == 0) {
                stock.remove(ean);
            } else {
                stock.put(ean, remainingStock);
            }

            if (remainingReserved == 0) {
                reservedItems.remove(ean);
            } else {
                reservedItems.put(ean, remainingReserved);
            }
        }
    }

    /**
     * Reserves a specified quantity of a single EAN in this bin for a port.
     *
     * @throws IllegalArgumentException if the requested reservation is invalid or exceeds available stock.
     */
    public void reserveItem(String ean, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Reserved quantity must be > 0 for EAN %s in bin %s".formatted(ean, id));
        }

        reserve(ean, quantity);
    }

    /**
     * Reserves specified quantities of EANs in this bin for a port.
     *
     * @throws IllegalArgumentException if requested reservations are invalid or exceed available stock.
     */
    public void reserveItems(Map<String, Integer> itemsToReserve) {
        if (itemsToReserve == null || itemsToReserve.isEmpty()) {
            throw new IllegalArgumentException("Reserved items must be provided for bin %s".formatted(id));
        }

        for (Map.Entry<String, Integer> entry : itemsToReserve.entrySet()) {
            String ean = entry.getKey();
            Integer qty = entry.getValue();

            if (qty == null || qty <= 0) {
                throw new IllegalArgumentException(
                        "Reserved quantity must be > 0 for EAN %s in bin %s".formatted(ean, id));
            }

            reserve(ean, qty);
        }
    }

    private void reserve(String ean, int quantity) {
        int availableQty = stock.get(ean);
        int currentlyReserved = reservedItems.getOrDefault(ean, 0);
        if (quantity + currentlyReserved > availableQty) {
            throw new IllegalArgumentException(
                "Bin %s has only %d units of EAN %s; cannot reserve additional %d"
                    .formatted(id, availableQty - currentlyReserved, ean, quantity));
        }

        reservedItems.put(ean, currentlyReserved + quantity);
    }

    public void reserveForPort(String portId) {
        if (portId == null || portId.isEmpty()) {
            throw new IllegalArgumentException("Port ID must be provided when reserving bin %s".formatted(id));
        }
        if (status == BinStatus.RESERVED) {
            throw new IllegalStateException("Bin %s is already reserved for port %s".formatted(id, reservedForPortId));
        }

        this.reservedForPortId = portId;
        this.status = BinStatus.RESERVED;
    }

    public void release() {
        this.reservedForPortId = null;
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

    public void enqueuePort(String portId) {
        portQueue.add(portId);
    }

    public String pollPort() {
        return portQueue.poll();
    }

    @Override
    public String toString() {
        return "Bin{id='%s', grid='%s', status=%s, stock=%s, reservedItems=%s}"
                .formatted(id, currentGridId, status, stock, reservedItems);
    }
}
