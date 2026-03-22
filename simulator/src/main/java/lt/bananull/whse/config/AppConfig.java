package lt.bananull.whse.config;

import java.nio.file.Path;

public record AppConfig(
        Path dataDir,
        String routerCommand,
        Path eventLogFile,
        int healthCheckInterval
) {

    public static final Path DEFAULT_DATA_DIR = Path.of("./data");
    public static final String DEFAULT_ROUTER_COMMAND = "./build/router";
    public static final Path DEFAULT_EVENT_LOG_FILE = Path.of("./simulation.log");
    public static final int DEFAULT_HEALTH_CHECK_INTERVAL = 0;

    public static AppConfig defaults() {
        return new AppConfig(DEFAULT_DATA_DIR, DEFAULT_ROUTER_COMMAND, DEFAULT_EVENT_LOG_FILE, DEFAULT_HEALTH_CHECK_INTERVAL);
    }

    public static AppConfig fromArgs(String[] args) {
        Path dataDir = DEFAULT_DATA_DIR;
        String routerCommand = DEFAULT_ROUTER_COMMAND;
        Path eventLogFile = DEFAULT_EVENT_LOG_FILE;
        int healthCheckInterval = DEFAULT_HEALTH_CHECK_INTERVAL;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dataDir" -> {
                    if (i + 1 >= args.length) { System.err.println("Missing value for --dataDir"); break; }
                    dataDir = Path.of(args[++i]);
                }
                case "--router" -> {
                    if (i + 1 >= args.length) { System.err.println("Missing value for --router"); break; }
                    routerCommand = args[++i];
                }
                case "--eventLogFile" -> {
                    if (i + 1 >= args.length) { System.err.println("Missing value for --eventLogFile"); break; }
                    eventLogFile = Path.of(args[++i]);
                }
                case "--healthCheckInterval" -> {
                    if (i + 1 >= args.length) { System.err.println("Missing value for --healthCheckInterval"); break; }
                    healthCheckInterval = Integer.parseInt(args[++i]);
                }
                case "--debug" -> System.setProperty("logLevel", "DEBUG");
                case "--info" -> System.setProperty("logLevel", "INFO");
                default -> System.err.println("Unknown argument: " + args[i]);
            }
        }

        // Propagate eventLogFile to system property for the logging framework
        System.setProperty("eventLogFile", eventLogFile.toString());

        // Disable long logback message at start
        System.setProperty(
            "logback.statusListenerClass",
            "ch.qos.logback.core.status.NopStatusListener"
        );

        return new AppConfig(dataDir, routerCommand, eventLogFile, healthCheckInterval);
    }
}
