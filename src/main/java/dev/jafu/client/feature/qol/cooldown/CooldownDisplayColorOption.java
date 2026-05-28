package dev.jafu.client.feature.qol.cooldown;

import java.util.List;

public enum CooldownDisplayColorOption {
    GOLD("gold", "Gold", 0xFFFFAA00),
    AQUA("aqua", "Aqua", 0xFF55FFFF),
    GREEN("green", "Green", 0xFF55FF55),
    PINK("pink", "Pink", 0xFFFF55FF),
    RED("red", "Red", 0xFFFF5555),
    WHITE("white", "White", 0xFFFFFFFF);

    private static final List<CooldownDisplayColorOption> VALUES = List.of(values());

    private final String id;
    private final String label;
    private final int color;

    CooldownDisplayColorOption(String id, String label, int color) {
        this.id = id;
        this.label = label;
        this.color = color;
    }

    public static List<CooldownDisplayColorOption> all() {
        return VALUES;
    }

    public static CooldownDisplayColorOption fromColor(int color) {
        for (CooldownDisplayColorOption option : VALUES) {
            if (option.color == color) {
                return option;
            }
        }
        return GOLD;
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
