package dev.jafu.client.feature.garden.rng;

public final class GardenRngMath {
    private GardenRngMath() {
    }

    public static double chanceAtLeastOne(long attempts, double chance) {
        if (attempts <= 0L || chance <= 0.0D) {
            return 0.0D;
        }
        if (chance >= 1.0D) {
            return 1.0D;
        }
        return 1.0D - Math.pow(1.0D - chance, attempts);
    }

    public static double dryChance(long attempts, double chance) {
        return 1.0D - chanceAtLeastOne(attempts, chance);
    }

    public static double expectedDrops(long attempts, double chance) {
        return Math.max(0.0D, attempts) * Math.max(0.0D, chance);
    }
}
