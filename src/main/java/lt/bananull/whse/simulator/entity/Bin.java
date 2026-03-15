package lt.bananull.whse.simulator.entity;

import lt.bananull.whse.load.dto.BinDto;
import lt.bananull.whse.simulator.enums.BinStatus;

/**
 * Simulation entity representing a physical storage bin.
 */
public class Bin {

    private final String id;
    private String currentGridId;
    private BinStatus status;
    private String reservedForPortId;

    public Bin(String id, String currentGridId) {
        this.id = id;
        this.currentGridId = currentGridId;
        this.status = BinStatus.AVAILABLE;
    }

    public static Bin from(BinDto dto) {
        return new Bin(dto.id(), dto.currentGridLocation());
    }

    public String getId() { return id; }

    public String getCurrentGridId() { return currentGridId; }

    public BinStatus getStatus() { return status; }

    public String getReservedForPortId() { return reservedForPortId; }

    public void setCurrentGridId(String currentGridId) {
        this.currentGridId = currentGridId;
    }

    public void setStatus(BinStatus status) {
        this.status = status;
    }

    public void reserveForPort(String portId) {
        this.reservedForPortId = portId;
        this.status = BinStatus.RESERVED;
    }

    public void release() {
        this.reservedForPortId = null;
        this.status = BinStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return "Bin{id='%s', grid='%s', status=%s}".formatted(id, currentGridId, status);
    }
}
