package dev.jafu.client.feature.garden.rng;

public record GardenDropState(
        GardenDropDefinition definition,
        long sessionAttempts,
        long attemptsSinceDrop,
        long drops,
        double chance,
        double price,
        long lastDropMillis
) {
    public double expectedDrops() {
        return GardenRngMath.expectedDrops(sessionAttempts, chance);
    }

    public double dryChance() {
        return GardenRngMath.dryChance(attemptsSinceDrop, chance);
    }

    public double expectedValue() {
        return expectedDrops() * price;
    }
}
