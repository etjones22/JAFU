package dev.jafu.client;

import dev.jafu.client.command.JafuCommands;
import dev.jafu.client.feature.general.etherwarp.EtherwarpFeature;
import dev.jafu.client.feature.mining.powder.PowderChestFeature;
import dev.jafu.client.feature.mining.sacks.SacksStashFeature;
import net.fabricmc.api.ClientModInitializer;

public final class JafuClient implements ClientModInitializer {
    public static final String MOD_ID = "jafu";

    @Override
    public void onInitializeClient() {
        JafuCommands.register();
        EtherwarpFeature.register();
        PowderChestFeature.register();
        SacksStashFeature.register();
    }
}
