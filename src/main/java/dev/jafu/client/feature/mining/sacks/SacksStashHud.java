package dev.jafu.client.feature.mining.sacks;

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

public final class SacksStashHud {
    private static final int WIDTH = 244;
    private static final int LINE_HEIGHT = 10;

    private final SacksStashTracker tracker;

    public SacksStashHud(SacksStashTracker tracker) {
        this.tracker = tracker;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!JafuModules.isEnabled(JafuModules.SACKS_STASH_TRACKER) || client.player == null || client.options.hudHidden) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        SacksStashSnapshot snapshot = tracker.snapshot();
        int lineCount = lineCount(snapshot);
        int height = 28 + lineCount * LINE_HEIGHT;
        Rect bounds = HudLayoutStore.INSTANCE.bounds(
                JafuHudElements.SACKS_STASH_TRACKER,
                WIDTH,
                height,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight()
        );
        int x = bounds.x() + 4;
        int y = bounds.y() + 4;

        GuiDraw.fill(context, bounds, 0xAA101218);
        GuiDraw.text(context, textRenderer, "Sacks/Stash Tracker", x, y, 0xFFFFFF55);

        int rowY = y + 16;
        rowY = drawStat(context, textRenderer, SacksStashOption.SESSION_TOTAL, "Session", format(snapshot.sessionTotal()), x, rowY);
        rowY = drawStat(context, textRenderer, SacksStashOption.ITEMS_PER_HOUR, "Items/hr", format(snapshot.itemsPerHour()), x, rowY);
        rowY = drawStat(context, textRenderer, SacksStashOption.STASH_TOTAL, "Stash", format(snapshot.stashTotal()), x, rowY);
        rowY = drawStat(context, textRenderer, SacksStashOption.STASH_TYPES, "Types", Integer.toString(snapshot.stashTypes()), x, rowY);

        if (SacksStashSettings.INSTANCE.isVisible(SacksStashOption.RECENT_GAINS)) {
            drawRecentGains(context, textRenderer, snapshot.recentGains(), x, rowY);
        }
    }

    private static int lineCount(SacksStashSnapshot snapshot) {
        int lines = 0;
        for (SacksStashOption option : SacksStashOption.all()) {
            if (option == SacksStashOption.RECENT_GAINS) {
                continue;
            }
            if (SacksStashSettings.INSTANCE.isVisible(option)) {
                lines++;
            }
        }

        if (SacksStashSettings.INSTANCE.isVisible(SacksStashOption.RECENT_GAINS)) {
            lines += Math.max(1, snapshot.recentGains().size());
        }
        return lines;
    }

    private static int drawStat(
            DrawContext context,
            TextRenderer textRenderer,
            SacksStashOption option,
            String label,
            String value,
            int x,
            int rowY
    ) {
        if (!SacksStashSettings.INSTANCE.isVisible(option)) {
            return rowY;
        }

        GuiDraw.text(context, textRenderer, label + ":", x, rowY, JafuTheme.TEXT_MUTED);
        GuiDraw.text(context, textRenderer, value, x + 64, rowY, JafuTheme.GOOD);
        return rowY + LINE_HEIGHT;
    }

    private static void drawRecentGains(
            DrawContext context,
            TextRenderer textRenderer,
            List<SacksStashGain> recentGains,
            int x,
            int rowY
    ) {
        if (recentGains.isEmpty()) {
            GuiDraw.text(context, textRenderer, "Recent: waiting for sacks", x, rowY, JafuTheme.TEXT_MUTED);
            return;
        }

        for (SacksStashGain gain : recentGains) {
            GuiDraw.fill(context, new Rect(x, rowY + 2, 3, 6), JafuTheme.ACCENT);
            GuiDraw.text(context, textRenderer, "+" + format(gain.amount()), x + 8, rowY, JafuTheme.GOOD);
            GuiDraw.text(context, textRenderer, "Last " + gain.windowLabel(), x + 82, rowY, JafuTheme.TEXT_MUTED);
            rowY += LINE_HEIGHT;
        }
    }

    private static String format(long value) {
        return String.format("%,d", value);
    }
}
