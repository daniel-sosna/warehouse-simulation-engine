package lt.bananull.whse;

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
