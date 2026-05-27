package dev.jafu.client.feature.mining.sacks;

import java.util.List;

public record SacksStashSnapshot(
        long elapsedMillis,
        long sessionTotal,
        long stashTotal,
        int stashTypes,
        List<SacksStashGain> recentGains
) {
    private static final double MILLIS_PER_HOUR = 3_600_000.0D;

    public long itemsPerHour() {
        if (elapsedMillis <= 0L) {
            return 0L;
        }
        return Math.round(sessionTotal / (elapsedMillis / MILLIS_PER_HOUR));
    }
}
