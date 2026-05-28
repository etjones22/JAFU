package dev.jafu.client.module;

import java.util.List;

import dev.jafu.client.feature.gui.scrollabletooltips.ScrollableTooltipsModule;
import dev.jafu.client.feature.qol.cooldown.CooldownDisplayModule;
import dev.jafu.client.feature.qol.itemvalue.ItemValueOverlayModule;
import dev.jafu.client.feature.qol.modhider.ModHiderSettings;
import dev.jafu.client.feature.skyblock.storage.StorageIndexerModule;
import dev.jafu.client.gui.JafuTheme;

public final class JafuModules {
    public static final String POWDER_CHEST_TRACKER = "powder_chest_tracker";
    public static final String SACKS_STASH_TRACKER = "sacks_stash_tracker";
    public static final String FAST_ETHERWARP_HELPER = "fast_etherwarp_helper";
    public static final String ITEM_VIEW = "item_view";
    public static final String CHAT_ENHANCEMENTS = "chat_enhancements";
    public static final String AUTO_UPDATER = "auto_updater";
    public static final String GLOBAL_SETTINGS = "global_settings";
    public static final String GUI_SETTINGS = "gui_settings";
    public static final String FULLBRIGHT = "fullbright";
    public static final String MOD_HIDER = "mod_hider";
    public static final String ITEM_VALUE_OVERLAY = "item_value_overlay";
    public static final String COOLDOWN_DISPLAY = "cooldown_display";
    public static final String SCROLLABLE_TOOLTIPS = "scrollable_tooltips";
    public static final String STORAGE_INDEXER = "storage_indexer";

    private static final List<JafuModule> MODULES = List.of(
            new JafuModule(GLOBAL_SETTINGS, JafuCategory.GENERAL, "Global settings", "Shared font and text defaults", true, JafuTheme.ACCENT),
            new JafuModule(GUI_SETTINGS, JafuCategory.GENERAL, "GUI settings", "Customizes the JAFU menu appearance", true, JafuTheme.GOOD),
            new JafuModule(FAST_ETHERWARP_HELPER, JafuCategory.GENERAL, "Fast etherwarp helper", "Shows AOTE/AOTV held-use timing without macros", false, JafuTheme.ACCENT),
            new JafuModule(FULLBRIGHT, JafuCategory.QOL, "Fullbright", "Keeps world lighting fully visible", false, JafuTheme.ACCENT),
            new JafuModule(ITEM_VIEW, JafuCategory.GENERAL, "Item view", "Customizes the first-person held item model", false, JafuTheme.WARN),
            new JafuModule(CHAT_ENHANCEMENTS, JafuCategory.GENERAL, "Chat enhancements", "Smooth chat and clean typography", false, JafuTheme.GOOD),
            new JafuModule(AUTO_UPDATER, JafuCategory.GENERAL, "Auto updater", "Installs stable or snapshot releases on next launch", true, JafuTheme.ACCENT),
            new JafuModule(MOD_HIDER, JafuCategory.QOL, "Mod Hider", "Blocks selected client mod announcers", ModHiderSettings.INSTANCE.enabled(), JafuTheme.ACCENT),
            new CooldownDisplayModule(),
            new ItemValueOverlayModule(),
            new ScrollableTooltipsModule(),
            new StorageIndexerModule(),
            new JafuModule("bazaar_reminders", JafuCategory.SKYBLOCK, "Bazaar reminders", "Price movement and flip alerts", false, JafuTheme.GOOD),
            new JafuModule(POWDER_CHEST_TRACKER, JafuCategory.MINING, "Powder chest tracker", "Tracks Crystal Hollows treasure chest rewards", true, JafuTheme.ACCENT),
            new JafuModule(SACKS_STASH_TRACKER, JafuCategory.MINING, "Sacks/stash tracker", "Tracks sack gains and material stash totals", true, JafuTheme.GOOD),
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
