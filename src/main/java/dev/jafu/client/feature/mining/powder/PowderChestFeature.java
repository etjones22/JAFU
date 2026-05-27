package dev.jafu.client.feature.mining.powder;

import dev.jafu.client.JafuClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.util.Identifier;

public final class PowderChestFeature {
    private static final PowderChestTracker TRACKER = new PowderChestTracker();
    private static final PowderChestHud HUD = new PowderChestHud(TRACKER);

    private PowderChestFeature() {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> TRACKER.acceptMessage(message.getString()));
        HudElementRegistry.addLast(Identifier.of(JafuClient.MOD_ID, "powder_chest_tracker"), HUD::render);
    }
}
