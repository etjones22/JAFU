package dev.jafu.client.feature.qol.itemvalue;

import java.util.List;

public enum ItemValueOverlayOption {
    SHOW_BAZAAR("show_bazaar", "Show Bazaar prices", true),
    SHOW_AUCTION("show_auction", "Show AH lowest BIN", true),
    SHOW_STACK_VALUE("show_stack_value", "Show stack value", true),
    SHOW_LOADING_STATUS("show_loading_status", "Show loading/status", true);

    private static final List<ItemValueOverlayOption> VALUES = List.of(values());

    private final String id;
    private final String label;
    private final boolean defaultValue;

    ItemValueOverlayOption(String id, String label, boolean defaultValue) {
        this.id = id;
        this.label = label;
        this.defaultValue = defaultValue;
    }

    public static List<ItemValueOverlayOption> all() {
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
