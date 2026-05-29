package dev.jafu.client.feature.mining.powder;

import dev.jafu.client.JafuClient;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class PowderChestFeature {
    private static final PowderChestTracker TRACKER = new PowderChestTracker();
    private static final PowderChestHud HUD = new PowderChestHud(TRACKER);

    private PowderChestFeature() {
    }

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> TRACKER.acceptMessage(message.getString()));
        ClientTickEvents.END_CLIENT_TICK.register(PowderChestFeature::tick);
        HudElementRegistry.addLast(Identifier.of(JafuClient.MOD_ID, "powder_chest_tracker"), HUD::render);
    }

    public static void resetTracker() {
        TRACKER.reset();
    }

    private static void tick(MinecraftClient client) {
        TRACKER.tick(PowderChestFeature::sendChatMessage);
    }

    private static void sendChatMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[JAFU] " + message), false);
        }
    }
}
