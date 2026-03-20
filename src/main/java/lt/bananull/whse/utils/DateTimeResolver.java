package lt.bananull.whse.utils;

import java.time.Instant;

public class DateTimeResolver {

    private DateTimeResolver() {}

    public static long resolveSimTimeFromTimestamp(Instant timestamp, Instant simulationStart) {
        if (timestamp.getEpochSecond() < simulationStart.getEpochSecond()) {
            throw new IllegalArgumentException("Timestamp can't be older than simulation start");
        }

        return timestamp.getEpochSecond() - simulationStart.getEpochSecond();
    }
}
