package dev.jafu.client.feature.qol.cooldown;

public record CooldownDisplayState(
        boolean active,
        String itemName,
        long remainingMillis,
        long durationMillis
) {
    public static CooldownDisplayState empty() {
        return new CooldownDisplayState(false, "", 0L, 1L);
    }

    public double progress() {
        long duration = Math.max(1L, durationMillis);
        return Math.max(0.0D, Math.min(1.0D, 1.0D - remainingMillis / (double) duration));
    }
}
