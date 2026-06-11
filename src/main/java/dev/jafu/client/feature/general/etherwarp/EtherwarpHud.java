package dev.jafu.client.feature.general.etherwarp;

import dev.jafu.client.gui.JafuTheme;
import dev.jafu.client.gui.util.GuiDraw;
import dev.jafu.client.gui.util.Rect;
import dev.jafu.client.hud.HudLayoutStore;
import dev.jafu.client.hud.JafuHudElements;
import dev.jafu.client.hud.JafuHudChrome;
import dev.jafu.client.module.JafuModules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;

public final class EtherwarpHud {
    private static final int WIDTH = 190;
    private static final int CONTENT_HEIGHT = 34;
    private static final long CLICK_INTERVAL_MILLIS = 100L;

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!JafuModules.isEnabled(JafuModules.FAST_ETHERWARP_HELPER) || client.player == null || client.options.hudHidden) {
            return;
        }

        EtherwarpHeldItem heldItem = EtherwarpHeldItem.from(client.player);
        if (!heldItem.valid()) {
            return;
        }

        boolean useHeld = client.options.useKey.isPressed();
        TextRenderer textRenderer = client.textRenderer;
        Rect bounds = HudLayoutStore.INSTANCE.bounds(
                JafuHudElements.ETHERWARP_HELPER,
                WIDTH,
                JafuHudChrome.extraHeight() + CONTENT_HEIGHT,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight()
        );
        Rect content = JafuHudChrome.draw(context, textRenderer, bounds, "Etherwarp Helper", 0xFFFFFF55);
        int x = content.x();
        int y = content.y();

        GuiDraw.text(context, textRenderer, heldItem.displayName(), x, y, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, useHeld ? "Use held: manual 10 CPS rhythm" : "Hold right click to time casts", x, y + 12, useHeld ? JafuTheme.GOOD : JafuTheme.TEXT_MUTED);
        drawCadenceBar(context, x, y, useHeld, System.currentTimeMillis());
    }

    private static void drawCadenceBar(DrawContext context, int x, int y, boolean active, long nowMillis) {
        int width = 116;
        int progress = active ? (int) (nowMillis % CLICK_INTERVAL_MILLIS * width / CLICK_INTERVAL_MILLIS) : 0;
        Rect track = new Rect(x, y + 26, width, 5);
        GuiDraw.fill(context, track, JafuTheme.CONTROL);
        if (active) {
            GuiDraw.fill(context, new Rect(track.x(), track.y(), progress, track.height()), JafuTheme.ACCENT);
        }
    }

    private record EtherwarpHeldItem(boolean valid, String displayName) {
        private static EtherwarpHeldItem from(ClientPlayerEntity player) {
            ItemStack stack = player.getMainHandStack();
            if (stack.isEmpty()) {
                return new EtherwarpHeldItem(false, "");
            }

            String name = stack.getName().getString();
            return new EtherwarpHeldItem(isEtherwarpItemName(name), name);
        }

        private static boolean isEtherwarpItemName(String name) {
            return name.contains("Aspect of the End") || name.contains("Aspect of the Void");
        }
    }
}
