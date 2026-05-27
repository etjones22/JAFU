package dev.jafu.client.hud;

import java.util.List;

import dev.jafu.client.module.JafuModules;

public final class JafuHudElements {
    public static final JafuHudElement POWDER_CHEST_TRACKER = new JafuHudElement(
            JafuModules.POWDER_CHEST_TRACKER,
            JafuModules.POWDER_CHEST_TRACKER,
            "Powder Chest Tracker",
            8,
            14,
            244,
            100
    );

    public static final JafuHudElement ETHERWARP_HELPER = new JafuHudElement(
            JafuModules.FAST_ETHERWARP_HELPER,
            JafuModules.FAST_ETHERWARP_HELPER,
            "Etherwarp Helper",
            8,
            126,
            190,
            50
    );

    private static final List<JafuHudElement> ELEMENTS = List.of(
            POWDER_CHEST_TRACKER,
            ETHERWARP_HELPER
    );

    private JafuHudElements() {
    }

    public static List<JafuHudElement> all() {
        return ELEMENTS;
    }
}
