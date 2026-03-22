package lt.bananull.whse.utils;

import lt.bananull.whse.simulator.Simulator;

import java.util.concurrent.atomic.AtomicBoolean;

public final class HealthCheckThread {

    private HealthCheckThread() {
    }

    public static HealthCheckContext start(Simulator simulator, int healthCheckIntervalSeconds) {
        if (healthCheckIntervalSeconds <= 0) {
            return new HealthCheckContext(null, null);
        }

        AtomicBoolean runHealthChecks = new AtomicBoolean(true);
        Thread healthCheckThread = new Thread(() -> {
            while (runHealthChecks.get()) {
                boolean status = simulator.getState() != null;
                System.out.printf("Health check (%s): %s%n", status ? "UP" : "DOWN", simulator.getHealthString());

                try {
                    Thread.sleep((long) healthCheckIntervalSeconds * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "simulator-health-check");

        healthCheckThread.setDaemon(true);
        healthCheckThread.start();
        return new HealthCheckContext(healthCheckThread, runHealthChecks);
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
