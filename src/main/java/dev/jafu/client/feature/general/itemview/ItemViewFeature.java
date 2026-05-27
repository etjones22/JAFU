package dev.jafu.client.feature.general.itemview;

import dev.jafu.client.JafuClient;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.util.Identifier;

public final class ItemViewFeature {
    private static final ItemViewHud HUD = new ItemViewHud();

    private ItemViewFeature() {
    }

    public static void register() {
        HudElementRegistry.addLast(Identifier.of(JafuClient.MOD_ID, "item_view"), HUD::render);
    }
}
