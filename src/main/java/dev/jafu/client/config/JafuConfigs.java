package dev.jafu.client.config;

import dev.jafu.client.feature.general.chat.ChatEnhancementsSettings;
import dev.jafu.client.feature.general.globalsettings.GlobalSettings;
import dev.jafu.client.feature.general.itemview.ItemViewSetting;
import dev.jafu.client.feature.general.itemview.ItemViewSettings;
import dev.jafu.client.feature.general.updater.AutoUpdaterSettings;
import dev.jafu.client.feature.garden.rng.GardenRngSettings;
import dev.jafu.client.feature.gui.scrollabletooltips.ScrollableTooltipNumericSetting;
import dev.jafu.client.feature.gui.scrollabletooltips.ScrollableTooltipsSettings;
import dev.jafu.client.feature.mining.powder.PowderChestSettings;
import dev.jafu.client.feature.mining.sacks.SacksStashOption;
import dev.jafu.client.feature.mining.sacks.SacksStashSettings;
import dev.jafu.client.feature.qol.cooldown.CooldownDisplaySettings;
import dev.jafu.client.feature.qol.itemvalue.ItemValueOverlaySettings;
import dev.jafu.client.feature.qol.modhider.ModHiderSettings;
import dev.jafu.client.feature.qol.tokenguard.TokenGuardSettings;
import dev.jafu.client.feature.skyblock.storage.StorageIndexStore;
import dev.jafu.client.feature.skyblock.storage.StorageIndexerSettings;
import dev.jafu.client.hud.HudLayoutStore;

public final class JafuConfigs {
    private JafuConfigs() {
    }

    public static void bootstrap() {
        GlobalSettings.INSTANCE.font();
        ChatEnhancementsSettings.INSTANCE.configuredChatScale();
        ItemViewSettings.INSTANCE.value(ItemViewSetting.SIZE);
        AutoUpdaterSettings.INSTANCE.channel();
        ScrollableTooltipsSettings.INSTANCE.value(ScrollableTooltipNumericSetting.MAX_HEIGHT_PERCENT);
        StorageIndexerSettings.INSTANCE.enabled();
        StorageIndexStore.INSTANCE.configId();
        GardenRngSettings.INSTANCE.enabled();
        PowderChestSettings.INSTANCE.visibleStatCount();
        SacksStashSettings.INSTANCE.isVisible(SacksStashOption.SESSION_TOTAL);
        ModHiderSettings.INSTANCE.enabled();
        TokenGuardSettings.INSTANCE.enabled();
        ItemValueOverlaySettings.INSTANCE.enabled();
        CooldownDisplaySettings.INSTANCE.enabled();
        HudLayoutStore.INSTANCE.configId();
    }
}
