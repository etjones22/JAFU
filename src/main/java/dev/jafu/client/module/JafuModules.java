package dev.jafu.client.module;

import java.util.List;

import dev.jafu.client.gui.JafuTheme;

public final class JafuModules {
    public static final String POWDER_CHEST_TRACKER = "powder_chest_tracker";
    public static final String FAST_ETHERWARP_HELPER = "fast_etherwarp_helper";

    private static final List<JafuModule> MODULES = List.of(
            new JafuModule(FAST_ETHERWARP_HELPER, JafuCategory.GENERAL, "Fast etherwarp helper", "Shows AOTE/AOTV held-use timing without macros", false, JafuTheme.ACCENT),
            new JafuModule("bazaar_reminders", JafuCategory.SKYBLOCK, "Bazaar reminders", "Price movement and flip alerts", false, JafuTheme.GOOD),
            new JafuModule(POWDER_CHEST_TRACKER, JafuCategory.MINING, "Powder chest tracker", "Tracks Crystal Hollows treasure chest rewards", true, JafuTheme.ACCENT),
            new JafuModule("profile_notes", JafuCategory.GENERAL, "Profile notes", "Quick notes for active SkyBlock goals", false, JafuTheme.WARN),
            new JafuModule("dungeon_checklist", JafuCategory.DUNGEONS, "Dungeon checklist", "Compact ready-check style reminders", false, JafuTheme.GOOD),
            new JafuModule("garden_tracker", JafuCategory.GARDEN, "Garden tracker", "Visitor and crop todo surface", false, JafuTheme.WARN)
    );

    private JafuModules() {
    }

    public static List<JafuModule> byCategory(JafuCategory category) {
        return MODULES.stream()
                .filter(module -> module.category() == category)
                .toList();
    }

    public static boolean isEnabled(String id) {
        return MODULES.stream()
                .filter(module -> module.id().equals(id))
                .findFirst()
                .map(JafuModule::enabled)
                .orElse(false);
    }
}
