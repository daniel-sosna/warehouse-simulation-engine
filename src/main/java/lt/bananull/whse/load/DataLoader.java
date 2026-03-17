package lt.bananull.whse.load;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.load.dto.BinDto;
import lt.bananull.whse.load.dto.GridDto;
import lt.bananull.whse.load.dto.ShipmentDto;
import lt.bananull.whse.simulator.SimulationParameters;
import lt.bananull.whse.utils.JacksonMapper;
import lt.bananull.whse.load.dto.SimulationStateDto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class DataLoader {

    private static final String BINS_FILE = "bins.json";
    private static final String GRIDS_FILE = "grids.json";
    private static final String SHIPMENTS_FILE = "shipments.json";
    private static final String PARAMETERS_FILE = "parameters.json";

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

    /**
     * Loads simulation parameters from {@code parameters.json} in the dataset directory.
     * If the file does not exist, default parameter values are returned.
     * Values present in the file override the corresponding defaults.
     */
    public SimulationParameters loadParameters() {
        Path parametersPath = dataDir.resolve(PARAMETERS_FILE);
        if (!Files.exists(parametersPath)) {
            log.info("No {} found in {}; using default simulation parameters.", PARAMETERS_FILE, dataDir);
            return SimulationParameters.defaults();
        }

        try (InputStream in = Files.newInputStream(parametersPath)) {
            return objectMapper.readValue(in, SimulationParameters.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + PARAMETERS_FILE + " from " + dataDir, e);
        }
    }

    private List<BinDto> loadBins() throws IOException {
        Path binsPath = dataDir.resolve(BINS_FILE);
        try (InputStream in = Files.newInputStream(binsPath)) {
            return objectMapper.readValue(in, new TypeReference<>() {});
        }
    }

    private List<GridDto> loadGrids() throws IOException {
        Path gridsPath = dataDir.resolve(GRIDS_FILE);
        try (InputStream in = Files.newInputStream(gridsPath)) {
            return objectMapper.readValue(in, new TypeReference<>() {});
        }
    }

    private List<ShipmentDto> loadShipments() throws IOException {
        Path shipmentsPath = dataDir.resolve(SHIPMENTS_FILE);
        try (InputStream in = Files.newInputStream(shipmentsPath)) {
            return objectMapper.readValue(in, new TypeReference<>() {});
        }
    }
}