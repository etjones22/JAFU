package dev.jafu.client.feature.general.etherwarp;

import dev.jafu.client.JafuClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.util.Identifier;

public final class EtherwarpFeature {
    private static final EtherwarpHud HUD = new EtherwarpHud();

    private EtherwarpFeature() {
    }

    public static void register() {
        HudElementRegistry.addLast(Identifier.of(JafuClient.MOD_ID, "etherwarp_helper"), HUD::render);
    }
}
