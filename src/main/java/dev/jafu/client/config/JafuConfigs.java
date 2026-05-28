package dev.jafu.client.config;

import dev.jafu.client.feature.general.chat.ChatEnhancementsSettings;
import dev.jafu.client.feature.general.globalsettings.GlobalSettings;
import dev.jafu.client.feature.general.itemview.ItemViewSetting;
import dev.jafu.client.feature.general.itemview.ItemViewSettings;
import dev.jafu.client.feature.general.updater.AutoUpdaterSettings;
import dev.jafu.client.feature.mining.powder.PowderChestSettings;
import dev.jafu.client.feature.mining.sacks.SacksStashOption;
import dev.jafu.client.feature.mining.sacks.SacksStashSettings;
import dev.jafu.client.feature.qol.modhider.ModHiderSettings;
import dev.jafu.client.hud.HudLayoutStore;

public final class JafuConfigs {
    private JafuConfigs() {
    }

    public static void bootstrap() {
        GlobalSettings.INSTANCE.font();
        ChatEnhancementsSettings.INSTANCE.configuredChatScale();
        ItemViewSettings.INSTANCE.value(ItemViewSetting.SIZE);
        AutoUpdaterSettings.INSTANCE.channel();
        PowderChestSettings.INSTANCE.visibleStatCount();
        SacksStashSettings.INSTANCE.isVisible(SacksStashOption.SESSION_TOTAL);
        ModHiderSettings.INSTANCE.enabled();
        HudLayoutStore.INSTANCE.configId();
    }
}
