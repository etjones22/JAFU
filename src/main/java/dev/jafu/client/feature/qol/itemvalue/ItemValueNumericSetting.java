package dev.jafu.client.feature.qol.itemvalue;

import java.util.List;
import java.util.Locale;

public enum ItemValueNumericSetting {
    REFRESH_MINUTES("refresh_minutes", "Refresh", 1.0D, 30.0D, 5.0D, 1.0D),
    AUCTION_PAGES("auction_pages", "AH pages", 0.0D, 20.0D, 8.0D, 1.0D);

    private static final List<ItemValueNumericSetting> VALUES = List.of(values());

    private final String id;
    private final String label;
    private final double min;
    private final double max;
    private final double defaultValue;
    private final double step;

    ItemValueNumericSetting(String id, String label, double min, double max, double defaultValue, double step) {
        this.id = id;
        this.label = label;
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.step = step;
    }

    public static List<ItemValueNumericSetting> all() {
        return VALUES;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double defaultValue() {
        return defaultValue;
    }

    public double step() {
        return step;
    }

    public String format(double value) {
        if (this == REFRESH_MINUTES) {
            return Math.round(value) + "m";
        }
        return String.format(Locale.ROOT, "%.0f", value);
    }
}
