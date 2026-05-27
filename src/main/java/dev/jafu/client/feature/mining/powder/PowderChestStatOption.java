package dev.jafu.client.feature.mining.powder;

import java.util.List;

public enum PowderChestStatOption {
    SESSION_TIMER("session_timer", "Session timer"),
    CHESTS_PER_HOUR("chests_per_hour", "Chests/hour"),
    GEMSTONE_POWDER_PER_HOUR("gemstone_powder_per_hour", "Powder/hour"),
    AVERAGE_POWDER_PER_CHEST("average_powder_per_chest", "Avg powder/chest"),
    BEST_CHEST("best_chest", "Best chest"),
    SMOOTH_ITEM_ANIMATION("smooth_item_animation", "Smooth item animation");

    private static final List<PowderChestStatOption> VALUES = List.of(values());

    private final String id;
    private final String label;

    PowderChestStatOption(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static List<PowderChestStatOption> all() {
        return VALUES;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public boolean addsHudLine() {
        return this != SMOOTH_ITEM_ANIMATION;
    }
}
