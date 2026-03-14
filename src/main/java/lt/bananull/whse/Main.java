package lt.bananull.whse;

import lt.bananull.whse.dto.dataset.BinDto;
import lt.bananull.whse.dto.dataset.GridDto;
import lt.bananull.whse.dto.dataset.PortDto;
import lt.bananull.whse.dto.dataset.ShipmentDto;
import lt.bananull.whse.dto.dataset.ShiftDto;
import lt.bananull.whse.load.DataLoader;
import lt.bananull.whse.load.SimulationState;
import lt.bananull.whse.sim.BasicShipmentProcessor;

import java.nio.file.Path;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        String dataDirArg = "./data/1"; // default
        for (int i = 0; i < args.length - 1; i++) {
            if ("--dataDir".equals(args[i])) {
                dataDirArg = args[i + 1];
            }
        }

        System.out.println("Using dataDir: " + dataDirArg);

        Path dataDir = Path.of(dataDirArg);
        DataLoader loader = new DataLoader(dataDir);

        SimulationState state = loader.loadAll();

        System.out.println();
        System.out.println("=== DATASET STATE SUMMARY ===");
        System.out.println("Bins:       " + state.bins().size());
        System.out.println("Grids:      " + state.grids().size());
        System.out.println("Shipments:  " + state.shipments().size());
        System.out.println();

        System.out.println("=== BINS ===");
        for (BinDto bin : state.bins()) {
            System.out.println("Bin ID: " + bin.id()
                    + ", grid: " + bin.currentGridLocation());
            bin.itemsInBin().forEach((ean, itemDto) ->
                    System.out.println("  Item " + ean
                            + " -> qty=" + itemDto.quantity())
            );
            System.out.println();
        }

        System.out.println("=== GRIDS ===");
        for (GridDto grid : state.grids()) {
            System.out.println("Grid ID: " + grid.id());

            if (grid.shifts() != null) {
                for (ShiftDto shift : grid.shifts()) {
                    System.out.println("  Shift: " + shift.start()
                            + " - " + shift.end());

                    if (shift.portConfig() != null) {
                        System.out.println("    Ports:");
                        for (PortDto port : shift.portConfig()) {
                            System.out.println("      Port ID: " + port.id()
                                    + ", flags=" + port.handlingFlags());
                        }
                    }
                }
            }

            System.out.println();
        }

        System.out.println("=== SHIPMENTS ===");
        for (ShipmentDto shipment : state.shipments()) {
            System.out.println("Shipment ID:   " + shipment.id());
            System.out.println("Shipment date: " + shipment.shipmentDate());
            System.out.println("Items:");
            shipment.items().forEach((ean, qty) ->
                    System.out.println("  " + ean + " -> " + qty)
            );
            System.out.println();
        }

        BasicShipmentProcessor processor = new BasicShipmentProcessor();
        BasicShipmentProcessor.Result result = processor.process(state);

        System.out.println("=== LEVEL 1 PROCESSING RESULT ===");
        System.out.println("Packed shipments:            " + result.packedShipments());
        System.out.println("Failed/unprocessed shipments:" + result.failedShipments());
        System.out.println();
        System.out.println("Remaining bin quantities:");
        for (Map.Entry<String, Map<String, Integer>> bin : result.remainingBinQuantities().entrySet()) {
            System.out.println("  Bin " + bin.getKey() + ":");
            for (Map.Entry<String, Integer> item : bin.getValue().entrySet()) {
                System.out.println("    " + item.getKey() + " -> qty=" + item.getValue());
            }
            System.out.println();
        }
    }
}
