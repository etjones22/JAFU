package dev.jafu.client.feature.mining.sacks;

import java.util.List;

public enum SacksStashOption {
    SESSION_TOTAL("session_total", "Session total"),
    ITEMS_PER_HOUR("items_per_hour", "Items/hour"),
    RECENT_GAINS("recent_gains", "Recent gains"),
    STASH_TOTAL("stash_total", "Stash total"),
    STASH_TYPES("stash_types", "Stash types");

    private static final List<SacksStashOption> VALUES = List.of(values());

    private final String id;
    private final String label;

    SacksStashOption(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public static List<SacksStashOption> all() {
        return VALUES;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }
}
