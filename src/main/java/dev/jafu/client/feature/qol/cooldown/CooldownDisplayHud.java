package dev.jafu.client.feature.qol.cooldown;

import dev.jafu.client.gui.JafuTheme;
import dev.jafu.client.gui.util.GuiDraw;
import dev.jafu.client.gui.util.Rect;
import dev.jafu.client.hud.HudLayoutStore;
import dev.jafu.client.hud.JafuHudElements;
import dev.jafu.client.module.JafuModules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public final class CooldownDisplayHud {
    private static final int WIDTH = 176;
    private static final int TEXT_HEIGHT = 44;
    private static final int BAR_HEIGHT = 54;

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!JafuModules.isEnabled(JafuModules.COOLDOWN_DISPLAY) || client.player == null || client.options.hudHidden) {
            return;
        }

        CooldownDisplayState state = CooldownDisplayFeature.state();
        if (!state.active()) {
            return;
        }

        boolean progressBar = CooldownDisplaySettings.INSTANCE.isEnabled(CooldownDisplayOption.USE_PROGRESS_BAR);
        int height = progressBar ? BAR_HEIGHT : TEXT_HEIGHT;
        Rect bounds = HudLayoutStore.INSTANCE.bounds(
                JafuHudElements.COOLDOWN_DISPLAY,
                WIDTH,
                height,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight()
        );
        TextRenderer textRenderer = client.textRenderer;
        int textColor = CooldownDisplaySettings.INSTANCE.textColor();
        int x = bounds.x() + 6;
        int y = bounds.y() + 5;

        GuiDraw.fill(context, bounds, 0xAA101218);
        GuiDraw.text(context, textRenderer, "Cooldown", x, y, textColor);
        if (CooldownDisplaySettings.INSTANCE.isEnabled(CooldownDisplayOption.SHOW_ITEM_NAME)) {
            GuiDraw.text(context, textRenderer, trimToWidth(textRenderer, state.itemName(), bounds.width() - 12), x, y + 12, JafuTheme.TEXT);
        }

        if (progressBar) {
            drawProgressBar(context, textRenderer, bounds, state, textColor);
            return;
        }

        GuiDraw.text(context, textRenderer, "Ready in " + formatSeconds(state.remainingMillis()), x, y + 26, textColor);
    }

    private static void drawProgressBar(DrawContext context, TextRenderer textRenderer, Rect bounds, CooldownDisplayState state, int color) {
        int x = bounds.x() + 6;
        int barY = bounds.bottom() - 15;
        Rect track = new Rect(x, barY, bounds.width() - 12, 6);
        int fillWidth = (int) Math.round(track.width() * state.progress());

        GuiDraw.fill(context, track, JafuTheme.CONTROL);
        GuiDraw.fill(context, new Rect(track.x(), track.y(), fillWidth, track.height()), color);
        GuiDraw.text(context, textRenderer, formatSeconds(state.remainingMillis()), bounds.right() - 42, bounds.y() + 5, color);
    }

    private static String formatSeconds(long remainingMillis) {
        return String.format("%.1fs", Math.max(0.0D, remainingMillis / 1000.0D));
    }

    private static String trimToWidth(TextRenderer textRenderer, String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = textRenderer.getWidth(ellipsis);
        String trimmed = text;
        while (!trimmed.isEmpty() && textRenderer.getWidth(trimmed) + ellipsisWidth > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }
}
