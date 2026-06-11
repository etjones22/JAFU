package dev.jafu.client.feature.mining.powder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.jafu.client.feature.general.globalsettings.GlobalSettings;
import dev.jafu.client.gui.CleanFont;
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
import net.minecraft.client.render.RenderTickCounter;

public final class PowderChestHud {
    private static final int WIDTH = 244;
    private static final int BASE_LINE_HEIGHT = 10;
    private static final int MAX_DROPS = 6;
    private static final long ANIMATION_MILLIS = 240L;

    private final PowderChestTracker tracker;
    private final Map<String, Double> animatedAmounts = new HashMap<>();
    private final Map<String, Double> animatedRows = new HashMap<>();
    private long lastRenderMillis;

    public PowderChestHud(PowderChestTracker tracker) {
        this.tracker = tracker;
    }

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!JafuModules.isEnabled(JafuModules.POWDER_CHEST_TRACKER) || client.player == null || client.options.hudHidden) {
            return;
        }
        if (!PowderChestHeldItemFilter.shouldShow(client)) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        long nowMillis = System.currentTimeMillis();
        double animationStep = animationStep(nowMillis);
        PowderChestSnapshot snapshot = tracker.snapshot();
        List<PowderChestDrop> drops = snapshot.topDrops(MAX_DROPS);
        boolean animated = PowderChestSettings.INSTANCE.isVisible(PowderChestStatOption.SMOOTH_ITEM_ANIMATION);
        int lineHeight = lineHeight();
        int headerGap = Math.max(12, lineHeight + 2);
        int dropNameWidth = scaledTextWidthLimit(WIDTH - 76);
        int height = JafuHudChrome.extraHeight() + headerGap * 3 + PowderChestSettings.INSTANCE.visibleStatCount() * lineHeight + drops.size() * lineHeight;
        Rect bounds = HudLayoutStore.INSTANCE.bounds(
                JafuHudElements.POWDER_CHEST_TRACKER,
                WIDTH,
                height,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight()
        );
        Rect content = JafuHudChrome.draw(context, textRenderer, bounds, "Powder Chest Tracker", 0xFFFFFF55);
        int x = content.x();
        int y = content.y();

        drawText(context, textRenderer, "Chests:", x, y, JafuTheme.TEXT_MUTED);
        drawText(context, textRenderer, Integer.toString(snapshot.chests()), x + 62, y, JafuTheme.GOOD);
        drawText(context, textRenderer, "Gemstone Powder:", x, y + headerGap, JafuTheme.TEXT_MUTED);
        drawText(context, textRenderer, format(snapshot.gemstonePowder()), x + 116, y + headerGap, 0xFFFFAA00);

        int rowY = y + headerGap * 2 + 4;
        rowY = drawStats(context, textRenderer, snapshot.stats(), x, rowY, lineHeight);
        Set<String> visibleDropNames = new HashSet<>();
        for (PowderChestDrop drop : drops) {
            visibleDropNames.add(drop.name());
            int dropColor = PowderChestDropColors.forName(drop.name());
            double drawRowY = animated ? animatedValue(animatedRows, drop.name(), rowY, animationStep) : rowY;
            long amount = animated ? Math.round(animatedValue(animatedAmounts, drop.name(), drop.amount(), animationStep)) : drop.amount();
            GuiDraw.fill(context, new Rect(x, (int) Math.round(drawRowY) + 2, 3, 6), dropColor);
            drawText(context, textRenderer, format(amount), x + 8, (int) Math.round(drawRowY), dim(dropColor));
            drawText(context, textRenderer, trimToWidth(textRenderer, drop.name(), dropNameWidth), x + 66, (int) Math.round(drawRowY), dropColor);
            rowY += lineHeight;
        }
        animatedAmounts.keySet().removeIf(name -> !visibleDropNames.contains(name));
        animatedRows.keySet().removeIf(name -> !visibleDropNames.contains(name));
    }

    private static String format(long value) {
        return String.format("%,d", value);
    }

    private double animationStep(long nowMillis) {
        if (lastRenderMillis == 0L) {
            lastRenderMillis = nowMillis;
            return 1.0D;
        }

        long deltaMillis = Math.max(0L, nowMillis - lastRenderMillis);
        lastRenderMillis = nowMillis;
        return Math.min(1.0D, deltaMillis / (double) ANIMATION_MILLIS);
    }

    private static double animatedValue(Map<String, Double> values, String key, double target, double step) {
        Double current = values.get(key);
        if (current == null) {
            values.put(key, target);
            return target;
        }

        double next = current + (target - current) * ease(step);
        if (Math.abs(target - next) < 0.05D) {
            next = target;
        }
        values.put(key, next);
        return next;
    }

    private static double ease(double value) {
        double clamped = Math.max(0.0D, Math.min(1.0D, value));
        return 1.0D - Math.pow(1.0D - clamped, 3.0D);
    }

    private static int drawStats(
            DrawContext context,
            TextRenderer textRenderer,
            PowderChestSessionStats stats,
            int x,
            int rowY,
            int lineHeight
    ) {
        rowY = drawStat(context, textRenderer, PowderChestStatOption.SESSION_TIMER, "Time", formatDuration(stats.elapsedMillis()), x, rowY, lineHeight);
        rowY = drawStat(context, textRenderer, PowderChestStatOption.CHESTS_PER_HOUR, "Chests/hr", String.format("%.1f", stats.chestsPerHour()), x, rowY, lineHeight);
        rowY = drawStat(context, textRenderer, PowderChestStatOption.GEMSTONE_POWDER_PER_HOUR, "Powder/hr", format(stats.gemstonePowderPerHour()), x, rowY, lineHeight);
        rowY = drawStat(context, textRenderer, PowderChestStatOption.AVERAGE_POWDER_PER_CHEST, "Avg/chest", format(stats.averageGemstonePowderPerChest()), x, rowY, lineHeight);
        return drawStat(context, textRenderer, PowderChestStatOption.BEST_CHEST, "Best chest", format(stats.bestChestGemstonePowder()), x, rowY, lineHeight);
    }

    private static int drawStat(
            DrawContext context,
            TextRenderer textRenderer,
            PowderChestStatOption option,
            String label,
            String value,
            int x,
            int rowY,
            int lineHeight
    ) {
        if (!PowderChestSettings.INSTANCE.isVisible(option)) {
            return rowY;
        }

        drawText(context, textRenderer, label + ":", x, rowY, JafuTheme.TEXT_MUTED);
        drawText(context, textRenderer, value, x + 82, rowY, JafuTheme.GOOD);
        return rowY + lineHeight;
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

    private static int scaledTextWidthLimit(int width) {
        if (!GlobalSettings.INSTANCE.customFontEnabled()) {
            return width;
        }
        return Math.max(40, (int) Math.floor(width / GlobalSettings.INSTANCE.textScale()));
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
