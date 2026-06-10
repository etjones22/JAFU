package dev.jafu.client.feature.garden.rng;

import java.util.List;

public enum GardenRngStatOption {
    SESSION_TIMER("session_timer", "Session timer"),
    ATTEMPTS("attempts", "Attempts"),
    DRY_STREAK_ODDS("dry_streak_odds", "Dry streak odds"),
    EXPECTED_DROPS("expected_drops", "Expected drops"),
    DROP_RATE("drop_rate", "Drop rate"),
    ACTUAL_DROPS("actual_drops", "Actual drops"),
    VALUE("value", "Profit odds");

    private static final List<GardenRngStatOption> VALUES = List.of(values());

    private final String id;
    private final String label;

    GardenRngStatOption(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static List<GardenRngStatOption> all() {
        return VALUES;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }
}
