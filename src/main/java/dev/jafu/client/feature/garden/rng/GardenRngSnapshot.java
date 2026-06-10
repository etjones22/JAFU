package dev.jafu.client.feature.garden.rng;

import java.util.List;

public record GardenRngSnapshot(
        GardenCrop activeCrop,
        long sessionAttempts,
        long elapsedMillis,
        List<GardenDropState> drops,
        double attemptsPerHour,
        double expectedValuePerHour
) {
    public static GardenRngSnapshot empty() {
        return new GardenRngSnapshot(null, 0L, 0L, List.of(), 0.0D, 0.0D);
    }
}
