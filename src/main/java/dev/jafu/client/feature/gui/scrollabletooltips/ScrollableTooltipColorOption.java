package dev.jafu.client.feature.gui.scrollabletooltips;

import java.util.List;

public enum ScrollableTooltipColorOption {
    SKYBLOCK_GOLD("skyblock_gold", "Gold", 0xFFFFAA00),
    JAFU_BLUE("jafu_blue", "Blue", 0xFF4EA4FF),
    EMERALD("emerald", "Emerald", 0xFF58D68D),
    ROSE("rose", "Rose", 0xFFFF6B8A),
    VIOLET("violet", "Violet", 0xFFB388FF),
    CLEAN_WHITE("clean_white", "White", 0xFFE8EDF7);

    private static final List<ScrollableTooltipColorOption> VALUES = List.of(values());

    private final String id;
    private final String label;
    private final int color;

    ScrollableTooltipColorOption(String id, String label, int color) {
        this.id = id;
        this.label = label;
        this.color = color;
    }

    public static List<ScrollableTooltipColorOption> all() {
        return VALUES;
    }

    public static ScrollableTooltipColorOption fromColor(int color) {
        for (ScrollableTooltipColorOption option : values()) {
            if (option.color == color) {
                return option;
            }
        }
        return SKYBLOCK_GOLD;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public int color() {
        return color;
    }
}
