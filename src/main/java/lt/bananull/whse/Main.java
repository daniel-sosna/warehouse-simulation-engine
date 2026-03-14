package lt.bananull.whse;

import lt.bananull.whse.router.RouterClient;

public class Main {
    private static String dataDir = "./data";
    private static String routerCmd = "./build/router";
    private static String eventLogFile = "./simulation.log";

    public static void main(String[] args) {
        collectArgs(args);

        // TODO 1. read files from dataDir

        // Create router client
        RouterClient routerClient = new RouterClient(routerCmd);

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