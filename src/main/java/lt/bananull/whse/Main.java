package lt.bananull.whse;

import com.fasterxml.jackson.core.JsonProcessingException;
import lt.bananull.whse.event.EventHandler;
import lt.bananull.whse.event.TestEvent;
import lt.bananull.whse.utils.JacksonMapper;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.load.DataLoader;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.utils.DateTimeResolver;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;

public class Main {

    private static String dataDir = "./data/1"; // TODO change to "./data"
    private static String routerCmd = "./build/router";

    public static void main(String[] args) {
        collectArgs(args);

        // Read files from dataDir
        DataLoader loader = new DataLoader(Path.of(dataDir));
        SimulationStateDto state = loader.loadAll();

        // Create router client
        RouterClient routerClient = new RouterClient(routerCmd);

        // Resolve time
        ZoneId zone = ZoneId.of("UTC");
        Instant startTime = DateTimeResolver.resolveSimulationStart(state, zone);
        Instant endTime = DateTimeResolver.resolveSimulationEnd(state, zone);

        // Main loop
        Simulator simulator = new Simulator(routerClient, state, startTime, endTime);
        simulator.run();
        EventHandler.handle(new TestEvent(100L));
    }

    private static void collectArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dataDir" -> dataDir = args[++i];
                case "--router" -> routerCmd = args[++i];
                case "--eventLogFile" -> System.setProperty("eventLogFile", args[++i]);
                case "--debug" -> System.setProperty("logLevel", "DEBUG");
                default -> System.err.println("Unknown argument: " + args[i]);
            }
        }
    }
}