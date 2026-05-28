package dev.jafu.client.feature.qol.cooldown;

import java.util.List;

public enum CooldownDisplayOption {
    USE_PROGRESS_BAR("use_progress_bar", "Use progress bar", false),
    SHOW_ITEM_NAME("show_item_name", "Show item name", true),
    TRACK_RIGHT_CLICK("track_right_click", "Track right-click uses", true),
    TRACK_LEFT_CLICK("track_left_click", "Track left-click attacks", true);

    private static final List<CooldownDisplayOption> VALUES = List.of(values());

    private final String id;
    private final String label;
    private final boolean defaultValue;

    CooldownDisplayOption(String id, String label, boolean defaultValue) {
        this.id = id;
        this.label = label;
        this.defaultValue = defaultValue;
    }

    public static List<CooldownDisplayOption> all() {
        return VALUES;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public boolean defaultValue() {
        return defaultValue;
    }
}
