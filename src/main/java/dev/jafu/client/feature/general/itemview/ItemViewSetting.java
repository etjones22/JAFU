package dev.jafu.client.feature.general.itemview;

import java.util.List;

public enum ItemViewSetting {
    SIZE("size", "Size", 8.0D, 96.0D, 40.0D, 1.0D),
    OFFSET_X("offset_x", "X", -120.0D, 120.0D, 0.0D, 1.0D),
    OFFSET_Y("offset_y", "Y", -80.0D, 80.0D, 0.0D, 1.0D),
    OFFSET_Z("offset_z", "Z", -200.0D, 200.0D, 0.0D, 1.0D),
    SPEED("speed", "Speed", 0.0D, 2.0D, 1.0D, 0.05D);

    private static final List<ItemViewSetting> VALUES = List.of(values());

    private final String id;
    private final String label;
    private final double min;
    private final double max;
    private final double defaultValue;
    private final double step;

    ItemViewSetting(String id, String label, double min, double max, double defaultValue, double step) {
        this.id = id;
        this.label = label;
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.step = step;
    }

    public static List<ItemViewSetting> all() {
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
}
