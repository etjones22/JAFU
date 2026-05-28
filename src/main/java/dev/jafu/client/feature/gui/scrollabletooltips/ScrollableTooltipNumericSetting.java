package dev.jafu.client.feature.gui.scrollabletooltips;

import java.util.List;

public enum ScrollableTooltipNumericSetting {
    MAX_HEIGHT_PERCENT("max_height_percent", "Max height", 25.0D, 95.0D, 65.0D, 1.0D),
    SCROLL_SPEED("scroll_speed", "Scroll speed", 6.0D, 80.0D, 28.0D, 1.0D),
    BAR_WIDTH("bar_width", "Bar width", 3.0D, 8.0D, 4.0D, 1.0D),
    ANIMATION_DURATION("animation_duration_ms", "Animation", 0.0D, 500.0D, 150.0D, 10.0D),
    BAR_OPACITY("bar_opacity", "Bar opacity", 0.15D, 1.0D, 0.78D, 0.05D);

    private static final List<ScrollableTooltipNumericSetting> VALUES = List.of(values());

    private final String id;
    private final String label;
    private final double min;
    private final double max;
    private final double defaultValue;
    private final double step;

    ScrollableTooltipNumericSetting(String id, String label, double min, double max, double defaultValue, double step) {
        this.id = id;
        this.label = label;
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.step = step;
    }

    public static List<ScrollableTooltipNumericSetting> all() {
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
        return switch (this) {
            case MAX_HEIGHT_PERCENT -> Integer.toString((int) Math.round(value)) + "%";
            case SCROLL_SPEED -> Integer.toString((int) Math.round(value)) + "px";
            case BAR_WIDTH -> Integer.toString((int) Math.round(value)) + "px";
            case ANIMATION_DURATION -> Integer.toString((int) Math.round(value)) + "ms";
            case BAR_OPACITY -> Integer.toString((int) Math.round(value * 100.0D)) + "%";
        };
    }
}
