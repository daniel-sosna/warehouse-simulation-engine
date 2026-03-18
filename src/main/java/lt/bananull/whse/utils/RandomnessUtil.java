package lt.bananull.whse.utils;

import lt.bananull.whse.simulator.SimulationParameters;

import java.util.Random;

public class RandomnessUtil {

    private RandomnessUtil() {}

    public static double sampleMultiplier(Random rng, SimulationParameters.Randomness r) {
        if (r == null) throw new IllegalArgumentException("Randomness config must not be null");
        if (rng == null) throw new IllegalArgumentException("Random must not be null");
        if (r.max() < r.min()) {
            throw new IllegalArgumentException("Randomness max must be >= min (min=%s, max=%s)".formatted(r.min(), r.max()));
        }
        return r.min() + rng.nextDouble() * (r.max() - r.min());
    }
}
