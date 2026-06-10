package dev.jafu.client.feature.garden.rng;

import dev.jafu.client.JafuClient;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class GardenRngFeature {
    private static final GardenRngTracker TRACKER = new GardenRngTracker();
    private static final GardenRngHud HUD = new GardenRngHud(TRACKER);

    private GardenRngFeature() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> TRACKER.tick());
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> TRACKER.acceptMessage(message.getString()));
        HudElementRegistry.addLast(Identifier.of(JafuClient.MOD_ID, "garden_rng_calculator"), HUD::render);
    }

    public static void recordHarvestAttempt(BlockState state, BlockPos pos) {
        TRACKER.recordHarvestAttempt(state, pos);
    }

    public static GardenRngSnapshot snapshot() {
        return TRACKER.snapshot();
    }

    public static void resetAll() {
        TRACKER.resetAll();
    }

    public static void resetActiveDrops() {
        TRACKER.resetActiveDrops();
    }

    public static void resetDrop(String id) {
        TRACKER.resetDrop(id);
    }
}
