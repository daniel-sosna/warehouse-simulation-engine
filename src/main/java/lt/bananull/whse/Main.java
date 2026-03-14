package lt.bananull.whse;

import com.fasterxml.jackson.core.JsonProcessingException;
import lt.bananull.whse.json.JacksonMapper;
import lt.bananull.whse.load.DataLoader;
import lt.bananull.whse.load.SimulationState;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.RouterRequest;
import lt.bananull.whse.router.dto.RouterResponse;

import java.nio.file.Path;

public class Main {
    private static String dataDir = "./data/1"; // TODO change to "./data"
    private static String routerCmd = "./build/router";
    private static String eventLogFile = "./simulation.log";

    public static void main(String[] args) throws JsonProcessingException {
        collectArgs(args);

        // TODO 1. read files from dataDir
        DataLoader loader = new DataLoader(Path.of(dataDir));
        SimulationState state = loader.loadAll();

        // Create router client
        RouterClient routerClient = new RouterClient(routerCmd);

        // TEMP
        RouterRequest routerRequest = RouterRequest.from(state);
        RouterResponse routerResponse = routerClient.route(routerRequest);
        System.out.println(JacksonMapper.create().writeValueAsString(routerResponse));

        // TODO 2. main while loop
    }

    private static void collectArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dataDir" -> dataDir = args[++i];
                case "--router" -> routerCmd = args[++i];
                case "--eventLogFile" -> eventLogFile = args[++i];
                default -> System.err.println("Unknown argument: " + args[i]);
            }
        }
    }
}