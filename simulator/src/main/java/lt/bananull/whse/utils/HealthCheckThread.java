package lt.bananull.whse.utils;

import lt.bananull.whse.simulator.Simulator;

import java.util.concurrent.atomic.AtomicBoolean;

public final class HealthCheckThread {

    private static final String TABLE_SEPARATOR =
        "+--------+------------+------------+----------+------------------+";
    private static final String TABLE_HEADER =
        "| STATUS | SIM TIME S |  SIM TIME  | PROGRESS | SCHEDULED EVENTS |";

    private HealthCheckThread() {
    }

    public static HealthCheckContext start(Simulator simulator, int healthCheckIntervalSeconds) {
        if (healthCheckIntervalSeconds <= 0) {
            return new HealthCheckContext(null, null);
        }

        AtomicBoolean runHealthChecks = new AtomicBoolean(true);
        Thread healthCheckThread = new Thread(() -> {
            printTableHeader();

            try {
                while (runHealthChecks.get()) {
                    printHealthRow(simulator);

                    try {
                        Thread.sleep((long) healthCheckIntervalSeconds * 1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                printHealthRow(simulator);
                printTableFooter();
            }
        }, "simulator-health-check");

        healthCheckThread.setDaemon(true);
        healthCheckThread.start();
        return new HealthCheckContext(healthCheckThread, runHealthChecks);
    }

    private static void printTableHeader() {
        System.out.println(TABLE_SEPARATOR);
        System.out.println(TABLE_HEADER);
        System.out.println(TABLE_SEPARATOR);
    }

    private static void printTableFooter() {
        System.out.println(TABLE_SEPARATOR);
    }

    private static void printHealthRow(Simulator simulator) {
        boolean status = simulator.getState() != null;
        if (status) {
            Simulator.HealthData healthData = simulator.getHealthData();
            printHealthRow("UP", healthData.simTimeSeconds(), healthData.simTimeReadable(),
                String.format("~%.1f%%", healthData.progressPercent()), healthData.scheduledEvents());
        } else {
            printHealthRow("DOWN", 0, "-", "-", 0);
        }
    }

    private static void printHealthRow(String status, long simTimeSeconds, String simTimeReadable,
                                       String progress, int scheduledEvents) {
        System.out.printf("| %-6s | %10d | %10s | %8s | %16d |%n",
            status,
            simTimeSeconds,
            simTimeReadable,
            progress,
            scheduledEvents);
    }

    public static void stop(HealthCheckContext context) {
        if (context == null || context.thread() == null || context.runFlag() == null) {
            return;
        }

        context.runFlag().set(false);
        context.thread().interrupt();
        try {
            context.thread().join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record HealthCheckContext(Thread thread, AtomicBoolean runFlag) {
    }
}
