package dev.jafu.client.feature.garden.rng;

import java.util.List;

import dev.jafu.client.feature.general.globalsettings.GlobalSettings;
import dev.jafu.client.gui.CleanFont;
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

public final class GardenRngHud {
    private static final int WIDTH = 260;
    private static final int BASE_LINE_HEIGHT = 10;
    private static final int MAX_DROPS = 5;

    private final GardenRngTracker tracker;

    public GardenRngHud(GardenRngTracker tracker) {
        this.tracker = tracker;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!JafuModules.isEnabled(JafuModules.GARDEN_RNG_CALCULATOR) || client.player == null || client.options.hudHidden) {
            return;
        }

        GardenRngSnapshot snapshot = tracker.snapshot();
        int lineHeight = lineHeight();
        int activeDropCount = Math.min(MAX_DROPS, snapshot.drops().size());
        int statLines = visibleStatLines();
        int height = 42 + statLines * lineHeight + activeDropCount * (lineHeight * 2 + 2);
        Rect bounds = HudLayoutStore.INSTANCE.bounds(
                JafuHudElements.GARDEN_RNG_CALCULATOR,
                WIDTH,
                height,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight()
        );
        int x = bounds.x() + 5;
        int y = bounds.y() + 5;

        GuiDraw.fill(context, bounds, 0xB00A1117);
        GuiDraw.horizontalLine(context, bounds.x(), bounds.y(), bounds.width(), 0xFFFFAA00);
        drawText(context, client.textRenderer, "Garden RNG Calculator", x, y, 0xFFFFAA00);

        String crop = snapshot.activeCrop() == null ? "Harvest a crop to start" : snapshot.activeCrop().label();
        drawText(context, client.textRenderer, "Crop:", x, y + lineHeight + 4, JafuTheme.TEXT_MUTED);
        drawText(context, client.textRenderer, crop, x + 42, y + lineHeight + 4, snapshot.activeCrop() == null ? JafuTheme.TEXT_MUTED : JafuTheme.GOOD);

        int rowY = y + lineHeight * 2 + 10;
        rowY = drawSessionStats(context, client.textRenderer, snapshot, x, rowY, lineHeight);

        List<GardenDropState> drops = snapshot.drops().stream().limit(MAX_DROPS).toList();
        for (GardenDropState drop : drops) {
            int color = drop.definition().color();
            GuiDraw.fill(context, new Rect(x, rowY + 2, 3, 14), color);
            drawText(context, client.textRenderer, trim(client.textRenderer, drop.definition().displayName(), WIDTH - 98), x + 8, rowY, color);
            drawText(context, client.textRenderer, formatAttempts(drop.attemptsSinceDrop()), x + WIDTH - 82, rowY, JafuTheme.TEXT_MUTED);
            rowY += lineHeight;
            drawText(context, client.textRenderer, dropDetail(drop), x + 8, rowY, JafuTheme.TEXT_MUTED);
            rowY += lineHeight + 2;
        }
    }

    private static int drawSessionStats(DrawContext context, TextRenderer textRenderer, GardenRngSnapshot snapshot, int x, int rowY, int lineHeight) {
        rowY = drawStat(context, textRenderer, GardenRngStatOption.SESSION_TIMER, "Time", formatDuration(snapshot.elapsedMillis()), x, rowY, lineHeight);
        rowY = drawStat(context, textRenderer, GardenRngStatOption.ATTEMPTS, "Attempts", format(snapshot.sessionAttempts()), x, rowY, lineHeight);
        if (GardenRngSettings.INSTANCE.showProfit()) {
            rowY = drawStat(context, textRenderer, GardenRngStatOption.VALUE, "EV/hr", formatCoins(snapshot.expectedValuePerHour()), x, rowY, lineHeight);
        }
        return rowY;
    }

    private static int drawStat(DrawContext context, TextRenderer textRenderer, GardenRngStatOption option, String label, String value, int x, int rowY, int lineHeight) {
        if (!GardenRngSettings.INSTANCE.isVisible(option)) {
            return rowY;
        }
        drawText(context, textRenderer, label + ":", x, rowY, JafuTheme.TEXT_MUTED);
        drawText(context, textRenderer, value, x + 74, rowY, JafuTheme.TEXT);
        return rowY + lineHeight;
    }

    private static String dropDetail(GardenDropState drop) {
        StringBuilder builder = new StringBuilder();
        if (GardenRngSettings.INSTANCE.isVisible(GardenRngStatOption.DRY_STREAK_ODDS)) {
            builder.append("dry ").append(formatPercent(drop.dryChance()));
        }
        if (GardenRngSettings.INSTANCE.isVisible(GardenRngStatOption.EXPECTED_DROPS)) {
            appendSeparator(builder).append("EV ").append(String.format("%.2f", drop.expectedDrops()));
        }
        if (GardenRngSettings.INSTANCE.isVisible(GardenRngStatOption.DROP_RATE)) {
            appendSeparator(builder).append("1/").append(formatDenominator(drop.chance()));
        }
        if (GardenRngSettings.INSTANCE.isVisible(GardenRngStatOption.ACTUAL_DROPS)) {
            appendSeparator(builder).append("got ").append(drop.drops());
        }
        if (GardenRngSettings.INSTANCE.isVisible(GardenRngStatOption.VALUE) && drop.price() > 0.0D) {
            appendSeparator(builder).append(formatCoins(drop.expectedValue()));
        }
        return builder.isEmpty() ? "Tracking" : builder.toString();
    }

    private static StringBuilder appendSeparator(StringBuilder builder) {
        if (!builder.isEmpty()) {
            builder.append("  ");
        }
        return builder;
    }

    private static int visibleStatLines() {
        int lines = 0;
        if (GardenRngSettings.INSTANCE.isVisible(GardenRngStatOption.SESSION_TIMER)) {
            lines++;
        }
        if (GardenRngSettings.INSTANCE.isVisible(GardenRngStatOption.ATTEMPTS)) {
            lines++;
        }
        if (GardenRngSettings.INSTANCE.isVisible(GardenRngStatOption.VALUE) && GardenRngSettings.INSTANCE.showProfit()) {
            lines++;
        }
        return lines;
    }

    private static void drawText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
        if (GlobalSettings.INSTANCE.customFontEnabled()) {
            CleanFont.draw(context, textRenderer, text, x, y, color);
            return;
        }
        GuiDraw.text(context, textRenderer, text, x, y, color);
    }

    private static int lineHeight() {
        if (!GlobalSettings.INSTANCE.customFontEnabled()) {
            return BASE_LINE_HEIGHT;
        }
        return Math.max(BASE_LINE_HEIGHT, (int) Math.ceil(BASE_LINE_HEIGHT * GlobalSettings.INSTANCE.textScale()));
    }

    private static String format(long value) {
        return String.format("%,d", value);
    }

    private static String formatAttempts(long value) {
        return format(value) + " dry";
    }

    private static String formatDuration(long elapsedMillis) {
        long totalSeconds = Math.max(0L, elapsedMillis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = totalSeconds % 3600L / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    private static String formatPercent(double value) {
        return String.format("%.1f%%", Math.max(0.0D, Math.min(1.0D, value)) * 100.0D);
    }

    private static String formatDenominator(double chance) {
        if (chance <= 0.0D) {
            return "?";
        }
        return format(Math.round(1.0D / chance));
    }

    private static String formatCoins(double coins) {
        if (coins <= 0.0D) {
            return "0";
        }
        if (coins >= 1_000_000_000.0D) {
            return String.format("%.2fb", coins / 1_000_000_000.0D);
        }
        if (coins >= 1_000_000.0D) {
            return String.format("%.2fm", coins / 1_000_000.0D);
        }
        if (coins >= 1_000.0D) {
            return String.format("%.1fk", coins / 1_000.0D);
        }
        return Long.toString(Math.round(coins));
    }

    private static String trim(TextRenderer textRenderer, String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }
        String trimmed = text;
        while (!trimmed.isEmpty() && textRenderer.getWidth(trimmed + "...") > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + "...";
    }
}
