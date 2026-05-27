package dev.jafu.client.feature.mining.sacks;

import dev.jafu.client.JafuClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.util.Identifier;

public final class SacksStashFeature {
    private static final SacksStashTracker TRACKER = new SacksStashTracker();
    private static final SacksStashHud HUD = new SacksStashHud(TRACKER);

    private SacksStashFeature() {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> TRACKER.acceptMessage(message.getString()));
        HudElementRegistry.addLast(Identifier.of(JafuClient.MOD_ID, "sacks_stash_tracker"), HUD::render);
    }
}
