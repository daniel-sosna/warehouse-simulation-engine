package lt.bananull.whse.load;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lt.bananull.whse.dto.dataset.BinDto;
import lt.bananull.whse.dto.dataset.GridDto;
import lt.bananull.whse.dto.dataset.ShipmentDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class DataLoader {

    private final Path dataDir;
    private final ObjectMapper objectMapper;

    public DataLoader(Path dataDir, ObjectMapper objectMapper) {
        this.dataDir = dataDir;
        this.objectMapper = objectMapper;
    }

    public DatasetState loadAll() throws IOException {
        List<BinDto> bins = loadBins();
        List<GridDto> grids = loadGrids();
        List<ShipmentDto> shipments = loadShipments();
        return new DatasetState(bins, grids, shipments);
    }

    private List<BinDto> loadBins() throws IOException {
        Path binsPath = dataDir.resolve("bins.json");
        String json = Files.readString(binsPath);
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private List<GridDto> loadGrids() throws IOException {
        Path gridsPath = dataDir.resolve("grids.json");
        String json = Files.readString(gridsPath);
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private List<ShipmentDto> loadShipments() throws IOException {
        Path shipmentsPath = dataDir.resolve("shipments.json");
        String json = Files.readString(shipmentsPath);
        return objectMapper.readValue(json, new TypeReference<>() {});
    }
}