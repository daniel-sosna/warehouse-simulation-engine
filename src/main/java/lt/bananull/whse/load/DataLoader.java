package lt.bananull.whse.load;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lt.bananull.whse.load.dto.BinDto;
import lt.bananull.whse.load.dto.GridDto;
import lt.bananull.whse.load.dto.ShipmentDto;
import lt.bananull.whse.utils.JacksonMapper;
import lt.bananull.whse.load.dto.SimulationStateDto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DataLoader {

    private final Path dataDir;
    private final ObjectMapper objectMapper;

    public DataLoader(Path dataDir) {
        this.dataDir = dataDir;
        this.objectMapper = JacksonMapper.create();
    }

    public SimulationStateDto loadAll() {
        try {
            List<BinDto> bins = loadBins();
            List<GridDto> grids = loadGrids();
            List<ShipmentDto> shipments = loadShipments();
            return new SimulationStateDto(bins, grids, shipments);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load dataset from " + dataDir, e);
        }
    }

    private List<BinDto> loadBins() throws IOException {
        Path binsPath = dataDir.resolve("bins.json");
        try (InputStream in = Files.newInputStream(binsPath)) {
            return objectMapper.readValue(in, new TypeReference<>() {});
        }
    }

    private List<GridDto> loadGrids() throws IOException {
        Path gridsPath = dataDir.resolve("grids.json");
        try (InputStream in = Files.newInputStream(gridsPath)) {
            return objectMapper.readValue(in, new TypeReference<>() {});
        }
    }

    private List<ShipmentDto> loadShipments() throws IOException {
        Path shipmentsPath = dataDir.resolve("shipments.json");
        try (InputStream in = Files.newInputStream(shipmentsPath)) {
            return objectMapper.readValue(in, new TypeReference<>() {});
        }
    }
}