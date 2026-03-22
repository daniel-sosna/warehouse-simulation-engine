package lt.bananull.whse;

import ch.qos.logback.classic.LoggerContext;
import lt.bananull.whse.config.AppConfig;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.SimulationParameters;
import lt.bananull.whse.load.DataLoader;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.utils.HealthCheckThread;
import lt.bananull.whse.utils.LogFileSorter;
import org.slf4j.LoggerFactory;

public class Main {

    public static void main(String[] args) {
        AppConfig config = AppConfig.fromArgs(args);

        // Read files from dataDir
        DataLoader loader = new DataLoader(config.dataDir());
        SimulationStateDto state = loader.loadAll();
        SimulationParameters parameters = loader.loadParameters();

        // Create router client
        RouterClient routerClient = new RouterClient(config.routerCommand());

        // Main loop
        Simulator simulator = new Simulator(routerClient, state, parameters);

        HealthCheckThread.HealthCheckContext healthCheckContext =
            HealthCheckThread.start(simulator, config.healthCheckInterval());
        try {
            simulator.run();
        } finally {
            HealthCheckThread.stop(healthCheckContext);
        }

        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext loggerContext) {
            loggerContext.stop();
        }

        try {
            LogFileSorter.sortSimulationLogBySimTime(config.eventLogFile());
        } catch (Exception e) {
            System.err.print("Failed to sort log file by simTime");
        }
    }
}
