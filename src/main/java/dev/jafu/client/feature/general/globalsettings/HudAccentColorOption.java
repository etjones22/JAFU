package dev.jafu.client.feature.general.globalsettings;

import java.util.List;

public enum HudAccentColorOption {
    SKYBLOCK_GOLD("skyblock_gold", "Gold", 0xFFFFAA00),
    LEMON("lemon", "Lemon", 0xFFFFFF55),
    EMERALD("emerald", "Emerald", 0xFF55FF55),
    AQUA("aqua", "Aqua", 0xFF55FFFF),
    ROSE("rose", "Rose", 0xFFFF77B7),
    AMETHYST("amethyst", "Amethyst", 0xFFAA55FF);

    private static final List<HudAccentColorOption> VALUES = List.of(values());

    private final String id;
    private final String label;
    private final int color;

    HudAccentColorOption(String id, String label, int color) {
        this.id = id;
        this.label = label;
        this.color = color;
    }

    public static List<HudAccentColorOption> all() {
        return VALUES;
    }

    public static HudAccentColorOption fromColor(int color) {
        int opaque = color | 0xFF000000;
        for (HudAccentColorOption option : VALUES) {
            if (option.color == opaque) {
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
