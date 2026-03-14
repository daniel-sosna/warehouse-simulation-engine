package lt.bananull.whse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lt.bananull.whse.json.JacksonMapper;
import lt.bananull.whse.load.DataLoader;
import lt.bananull.whse.load.DatasetMapper;
import lt.bananull.whse.dto.dataset.BinDto;
import lt.bananull.whse.dto.dataset.GridDto;
import lt.bananull.whse.dto.dataset.ShipmentDto;
import lt.bananull.whse.models.Bin;
import lt.bananull.whse.models.Grid;
import lt.bananull.whse.models.Shipment;

import java.nio.file.Path;
import java.util.List;

public class ReadInput {

    public static void main(String[] args) throws Exception {
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

        List<BinDto> binDtos = loader.loadBins();
        List<GridDto> gridDtos = loader.loadGrids();
        List<ShipmentDto> shipmentDtos = loader.loadShipments();

        List<Bin> bins = binDtos.stream()
                .map(DatasetMapper::toModel)
                .toList();

        List<Grid> grids = gridDtos.stream()
                .map(DatasetMapper::toModel)
                .toList();

        List<Shipment> shipments = shipmentDtos.stream()
                .map(DatasetMapper::toModel)
                .toList();

        System.out.println("\n=== MODEL BINS ===");
        System.out.println("Bin count: " + bins.size());
        for (Bin bin : bins) {
            System.out.println("Bin ID: " + bin.getId());
            System.out.println("  Grid: " + bin.getGridId());
            bin.getItems().forEach((ean, qty) ->
                    System.out.println("    Item " + ean + " -> " + qty)
            );
        }

        System.out.println("\n=== MODEL GRIDS ===");
        System.out.println("Grid count: " + grids.size());
        for (Grid grid : grids) {
            System.out.println("Grid ID: " + grid.getId());
            for (Grid.Shift shift : grid.getShifts()) {
                System.out.println("  Shift: " + shift.getStart() + " - " + shift.getEnd());
                for (Grid.Port port : shift.getPorts()) {
                    System.out.println("    Port " + port.getId()
                            + " flags=" + port.getHandlingFlags());
                }
            }
        }

        System.out.println("\n=== MODEL SHIPMENTS ===");
        System.out.println("Shipment count: " + shipments.size());
        for (Shipment shipment : shipments) {
            System.out.println("Shipment ID: " + shipment.getId());
            System.out.println("  Date: " + shipment.getShipmentDate());
            shipment.getItems().forEach((ean, qty) ->
                    System.out.println("    Item " + ean + " -> " + qty)
            );
        }
    }
}