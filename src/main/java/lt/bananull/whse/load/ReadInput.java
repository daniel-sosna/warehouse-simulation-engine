package lt.bananull.whse.load;

import com.fasterxml.jackson.databind.ObjectMapper;
import lt.bananull.whse.dto.dataset.BinDto;
import lt.bananull.whse.dto.dataset.GridDto;
import lt.bananull.whse.dto.dataset.ShipmentDto;
import lt.bananull.whse.dto.dataset.PortDto;
import lt.bananull.whse.dto.dataset.ShiftDto;
import lt.bananull.whse.json.JacksonMapper;

import java.nio.file.Path;

public class ReadInput {

    public static void main(String[] args) throws Exception {
        // 1) Perskaitom --dataDir argumentą (ar naudojam default)
        String dataDirArg = "./data/1"; // default
        for (int i = 0; i < args.length - 1; i++) {
            if ("--dataDir".equals(args[i])) {
                dataDirArg = args[i + 1];
            }
        }

        System.out.println("Using dataDir: " + dataDirArg);

        Path dataDir = Path.of(dataDirArg);
        ObjectMapper mapper = JacksonMapper.create();
        DataLoader loader = new DataLoader(dataDir, mapper);

        DatasetState state = loader.loadAll();

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
    }
}