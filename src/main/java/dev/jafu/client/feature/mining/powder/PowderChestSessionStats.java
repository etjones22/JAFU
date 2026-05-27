package dev.jafu.client.feature.mining.powder;

public record PowderChestSessionStats(
        long elapsedMillis,
        int chests,
        long gemstonePowder,
        long bestChestGemstonePowder
) {
    private static final double MILLIS_PER_HOUR = 3_600_000.0D;

    public double chestsPerHour() {
        if (elapsedMillis <= 0L) {
            return 0.0D;
        }
        return chests / (elapsedMillis / MILLIS_PER_HOUR);
    }

    public long gemstonePowderPerHour() {
        if (elapsedMillis <= 0L) {
            return 0L;
        }
        return Math.round(gemstonePowder / (elapsedMillis / MILLIS_PER_HOUR));
    }

    public long averageGemstonePowderPerChest() {
        if (chests <= 0) {
            return 0L;
        }
        return Math.round((double) gemstonePowder / chests);
    }
}
