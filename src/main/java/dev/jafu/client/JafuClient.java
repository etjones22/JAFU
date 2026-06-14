package dev.jafu.client;

import dev.jafu.client.command.JafuCommands;
import dev.jafu.client.config.JafuConfigs;
import dev.jafu.client.feature.general.etherwarp.EtherwarpFeature;
import dev.jafu.client.feature.general.path.PathTracerFeature;
import dev.jafu.client.feature.general.updater.AutoUpdater;
import dev.jafu.client.feature.garden.rng.GardenRngFeature;
import dev.jafu.client.feature.mining.powder.PowderChestFeature;
import dev.jafu.client.feature.mining.sacks.SacksStashFeature;
import dev.jafu.client.feature.qol.cooldown.CooldownDisplayFeature;
import net.fabricmc.api.ClientModInitializer;

public final class JafuClient implements ClientModInitializer {
    public static final String MOD_ID = "jafu";

    @Override
    public void onInitializeClient() {
        JafuConfigs.bootstrap();
        JafuCommands.register();
        EtherwarpFeature.register();
        PathTracerFeature.register();
        PowderChestFeature.register();
        SacksStashFeature.register();
        CooldownDisplayFeature.register();
        GardenRngFeature.register();
        AutoUpdater.register();
    }
}
