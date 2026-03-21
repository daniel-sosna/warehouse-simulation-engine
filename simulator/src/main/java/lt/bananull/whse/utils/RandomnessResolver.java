package lt.bananull.whse.utils;

import lt.bananull.whse.simulator.SimulationParameters;

import java.util.Objects;
import java.util.random.RandomGenerator;

public class RandomnessResolver {

    private final RandomGenerator randomGenerator;

    public RandomnessResolver(RandomGenerator randomGenerator) {
        this.randomGenerator = Objects.requireNonNull(randomGenerator, "randomGenerator must not be null");
    }

    public double resolveMultiplier(SimulationParameters.Randomness randomness) {

        if (randomness == null) {
            return 1.0;
        }

        double min = randomness.min();
        double max = randomness.max();

        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        if (Double.compare(min, max) == 0) {
            return min;
        }

        return randomGenerator.nextDouble(min, max);
    }
}