package dev.jafu.client.feature.mining.powder;

import java.util.List;

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

public final class PowderChestHud {
    private static final int WIDTH = 244;
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_DROPS = 6;

    private final PowderChestTracker tracker;

    public PowderChestHud(PowderChestTracker tracker) {
        this.tracker = tracker;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!JafuModules.isEnabled(JafuModules.POWDER_CHEST_TRACKER) || client.player == null || client.options.hudHidden) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        PowderChestSnapshot snapshot = tracker.snapshot();
        List<PowderChestDrop> drops = snapshot.topDrops(MAX_DROPS);
        int height = 50 + drops.size() * LINE_HEIGHT;
        Rect bounds = HudLayoutStore.INSTANCE.bounds(
                JafuHudElements.POWDER_CHEST_TRACKER,
                WIDTH,
                height,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight()
        );
        int x = bounds.x() + 4;
        int y = bounds.y() + 4;

        GuiDraw.fill(context, bounds, 0xAA101218);
        GuiDraw.text(context, textRenderer, "Powder Chest Tracker", x, y, 0xFFFFFF55);
        GuiDraw.text(context, textRenderer, "Chests:", x, y + 12, JafuTheme.TEXT_MUTED);
        GuiDraw.text(context, textRenderer, Integer.toString(snapshot.chests()), x + 62, y + 12, JafuTheme.GOOD);
        GuiDraw.text(context, textRenderer, "Gemstone Powder:", x, y + 24, JafuTheme.TEXT_MUTED);
        GuiDraw.text(context, textRenderer, format(snapshot.gemstonePowder()), x + 116, y + 24, 0xFFFFAA00);

        int rowY = y + 40;
        for (PowderChestDrop drop : drops) {
            int dropColor = PowderChestDropColors.forName(drop.name());
            String amount = format(drop.amount());
            GuiDraw.fill(context, new Rect(x, rowY + 2, 3, 6), dropColor);
            GuiDraw.text(context, textRenderer, amount, x + 8, rowY, dim(dropColor));
            GuiDraw.text(context, textRenderer, trimToWidth(textRenderer, drop.name(), WIDTH - 76), x + 66, rowY, dropColor);
            rowY += LINE_HEIGHT;
        }
    }

    private static String format(long value) {
        return String.format("%,d", value);
    }

    private static int dim(int color) {
        int alpha = color & 0xFF000000;
        int red = ((color >> 16) & 0xFF) * 3 / 4;
        int green = ((color >> 8) & 0xFF) * 3 / 4;
        int blue = (color & 0xFF) * 3 / 4;
        return alpha | red << 16 | green << 8 | blue;
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
