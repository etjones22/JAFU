package dev.jafu.client.feature.mining.powder;

import java.util.Locale;

import dev.jafu.client.gui.JafuTheme;

public final class PowderChestDropColors {
    private static final int POWDER = 0xFFFF55FF;
    private static final int RUBY = 0xFFFF5555;
    private static final int AMETHYST = 0xFFFF55FF;
    private static final int JADE = 0xFF55FF55;
    private static final int AMBER = 0xFFFFAA00;
    private static final int SAPPHIRE = 0xFF55FFFF;
    private static final int TOPAZ = 0xFFFFFF55;
    private static final int GOLD = 0xFFFFAA00;
    private static final int DIAMOND = 0xFF55FFFF;
    private static final int SLUDGE = 0xFF55AA55;
    private static final int TREASURE = 0xFFFFFF55;
    private static final int UTILITY = 0xFFAAAAAA;

    private PowderChestDropColors() {
    }

    public static int forName(String name) {
        String normalized = normalize(name);

        if (normalized.contains("gemstone powder")) {
            return POWDER;
        }
        if (normalized.contains("ruby gemstone")) {
            return RUBY;
        }
        if (normalized.contains("amethyst gemstone")) {
            return AMETHYST;
        }
        if (normalized.contains("jade gemstone")) {
            return JADE;
        }
        if (normalized.contains("amber gemstone")) {
            return AMBER;
        }
        if (normalized.contains("sapphire gemstone")) {
            return SAPPHIRE;
        }
        if (normalized.contains("topaz gemstone")) {
            return TOPAZ;
        }
        if (normalized.contains("gold essence")) {
            return GOLD;
        }
        if (normalized.contains("diamond essence")) {
            return DIAMOND;
        }
        if (normalized.contains("sludge juice") || normalized.contains("jungle heart") || normalized.contains("yoggie") || normalized.contains("goblin egg")) {
            return SLUDGE;
        }
        if (normalized.contains("prehistoric egg") || normalized.contains("pickonimbus") || normalized.contains("treasurite")) {
            return TREASURE;
        }
        if (normalized.contains("wishing compass") || normalized.contains("ascension rope") || normalized.contains("oil barrel")) {
            return UTILITY;
        }

        return JafuTheme.TEXT;
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
