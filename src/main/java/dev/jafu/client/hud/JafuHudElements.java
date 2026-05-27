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
            150,
            156,
            86
    );

    public static final JafuHudElement ETHERWARP_HELPER = new JafuHudElement(
            JafuModules.FAST_ETHERWARP_HELPER,
            JafuModules.FAST_ETHERWARP_HELPER,
            "Etherwarp Helper",
            8,
            126,
            190,
            50,
            132,
            44
    );

    public static final JafuHudElement SACKS_STASH_TRACKER = new JafuHudElement(
            JafuModules.SACKS_STASH_TRACKER,
            JafuModules.SACKS_STASH_TRACKER,
            "Sacks/Stash Tracker",
            8,
            176,
            244,
            90,
            156,
            66
    );

    public static final JafuHudElement ITEM_VIEW = new JafuHudElement(
            JafuModules.ITEM_VIEW,
            JafuModules.ITEM_VIEW,
            "Item View",
            260,
            14,
            150,
            150,
            72,
            72
    );

    private static final List<JafuHudElement> ELEMENTS = List.of(
            POWDER_CHEST_TRACKER,
            ETHERWARP_HELPER,
            SACKS_STASH_TRACKER,
            ITEM_VIEW
    );

    private JafuHudElements() {
    }

    public static List<JafuHudElement> all() {
        return ELEMENTS;
    }
}
