package lt.bananull.whse;

import lt.bananull.whse.config.AppConfig;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.SimulationParameters;
import lt.bananull.whse.load.DataLoader;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.utils.DateTimeResolver;

import java.time.Instant;
import java.time.ZoneId;

public class Main {

    public static void main(String[] args) {
        AppConfig config = AppConfig.fromArgs(args);

        // Read files from dataDir
        DataLoader loader = new DataLoader(config.dataDir());
        SimulationStateDto state = loader.loadAll();
        SimulationParameters parameters = loader.loadParameters();

        // Create router client
        RouterClient routerClient = new RouterClient(config.routerCommand());

        // Resolve time
        ZoneId zone = ZoneId.of("UTC");
        Instant startTime = DateTimeResolver.resolveSimulationStart(state, zone);
        Instant endTime = DateTimeResolver.resolveSimulationEnd(state, zone);

        // Main loop
        Simulator simulator = new Simulator(routerClient, state, startTime, endTime, parameters);
        simulator.run();
    }
}