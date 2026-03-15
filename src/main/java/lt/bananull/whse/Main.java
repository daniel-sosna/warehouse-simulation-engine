package lt.bananull.whse;

import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.load.DataLoader;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;

import java.nio.file.Path;

public class Main {
    private static String dataDir = "./data/1"; // TODO change to "./data"
    private static String routerCmd = "./build/router";
    private static String eventLogFile = "./simulation.log";

    public static void main(String[] args) {
        collectArgs(args);

        // Read files from dataDir
        DataLoader loader = new DataLoader(Path.of(dataDir));
        SimulationStateDto state = loader.loadAll();

        // Create router client
        RouterClient routerClient = new RouterClient(routerCmd);

        // Main loop
        Simulator simulator = new Simulator(routerClient, state);
        simulator.run();
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