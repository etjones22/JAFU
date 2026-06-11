package dev.jafu.client.hud;

import dev.jafu.client.feature.general.globalsettings.GlobalSettings;
import dev.jafu.client.feature.general.globalsettings.HudStylePreset;
import dev.jafu.client.gui.util.GuiDraw;
import dev.jafu.client.gui.util.Rect;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

public final class JafuHudChrome {
    private static final int BACKGROUND_RGB = 0x0A1117;
    private static final int CLEAN_BACKGROUND_RGB = 0x0E1218;
    private static final int MINIMAL_BACKGROUND_RGB = 0x070A10;
    private static final int LINE_SEGMENTS = 18;

    private JafuHudChrome() {
    }

    public static Rect draw(DrawContext context, TextRenderer textRenderer, Rect bounds, String title) {
        return draw(context, textRenderer, bounds, title, GlobalSettings.INSTANCE.hudAccentColor());
    }

    public static Rect draw(DrawContext context, TextRenderer textRenderer, Rect bounds, String title, int fallbackAccent) {
        GlobalSettings settings = GlobalSettings.INSTANCE;
        HudStylePreset preset = settings.hudStylePreset();
        int accent = settings.hudAccentColor() == 0 ? fallbackAccent : settings.hudAccentColor();
        int backgroundRgb = switch (preset) {
            case CLEAN -> CLEAN_BACKGROUND_RGB;
            case MINIMAL -> MINIMAL_BACKGROUND_RGB;
            case REFERENCE -> BACKGROUND_RGB;
        };
        int background = withAlpha(backgroundRgb, adjustedBackgroundOpacity(settings, preset));
        int border = withAlpha(accent, adjustedBorderOpacity(settings, preset));

        GuiDraw.fill(context, bounds, background);
        drawBorder(context, textRenderer, bounds, title, border, preset);
        if (settings.hudGradientLine()) {
            drawGradientLine(context, bounds, accent, preset);
        }
        drawTitle(context, textRenderer, bounds, title, accent, preset);
        return content(bounds, preset, settings.hudCompactMode());
    }

    public static int extraHeight() {
        return GlobalSettings.INSTANCE.hudCompactMode() ? 15 : 18;
    }

    private static Rect content(Rect bounds, HudStylePreset preset, boolean compact) {
        int horizontalPadding = compact ? 4 : 6;
        int topPadding = compact ? 13 : 16;
        if (preset == HudStylePreset.MINIMAL) {
            topPadding = compact ? 12 : 15;
        }
        int bottomPadding = compact ? 3 : 5;
        return new Rect(
                bounds.x() + horizontalPadding,
                bounds.y() + topPadding,
                Math.max(1, bounds.width() - horizontalPadding * 2),
                Math.max(1, bounds.height() - topPadding - bottomPadding)
        );
    }

    private static void drawBorder(DrawContext context, TextRenderer textRenderer, Rect bounds, String title, int color, HudStylePreset preset) {
        if (preset == HudStylePreset.MINIMAL) {
            GuiDraw.verticalLine(context, bounds.x(), bounds.y(), bounds.height(), color);
            GuiDraw.horizontalLine(context, bounds.x(), bounds.y(), bounds.width(), multiplyAlpha(color, 0.55D));
            return;
        }

        GuiDraw.verticalLine(context, bounds.x(), bounds.y(), bounds.height(), color);
        GuiDraw.verticalLine(context, bounds.right() - 1, bounds.y(), bounds.height(), color);
        GuiDraw.horizontalLine(context, bounds.x(), bounds.bottom() - 1, bounds.width(), color);

        int titleWidth = titleWidth(textRenderer, title);
        int titleX = bounds.x() + 7;
        int notchStart = Math.max(bounds.x() + 2, titleX - 4);
        int notchEnd = Math.min(bounds.right() - 2, titleX + titleWidth + 6);
        if (preset == HudStylePreset.REFERENCE && notchEnd > notchStart) {
            GuiDraw.horizontalLine(context, bounds.x(), bounds.y(), notchStart - bounds.x(), color);
            GuiDraw.horizontalLine(context, notchEnd, bounds.y(), bounds.right() - notchEnd, color);
            GuiDraw.horizontalLine(context, notchStart + 2, bounds.y() + 3, Math.max(0, notchEnd - notchStart - 4), multiplyAlpha(color, 0.34D));
            return;
        }

        GuiDraw.horizontalLine(context, bounds.x(), bounds.y(), bounds.width(), color);
    }

    private static void drawTitle(DrawContext context, TextRenderer textRenderer, Rect bounds, String title, int color, HudStylePreset preset) {
        int titleX = bounds.x() + 7;
        int titleY = bounds.y() + (preset == HudStylePreset.REFERENCE ? -4 : 3);
        if (titleY < 0) {
            titleY = bounds.y() + 2;
        }
        GuiDraw.text(context, textRenderer, title, titleX, titleY, color);
    }

    private static void drawGradientLine(DrawContext context, Rect bounds, int accent, HudStylePreset preset) {
        int y = bounds.y() + (preset == HudStylePreset.MINIMAL ? 12 : 13);
        int startX = bounds.x() + 6;
        int width = Math.max(24, bounds.width() - 12);
        int segmentWidth = Math.max(1, width / LINE_SEGMENTS);
        for (int i = 0; i < LINE_SEGMENTS; i++) {
            double alpha = 0.60D * (1.0D - i / (double) LINE_SEGMENTS);
            int x = startX + i * segmentWidth;
            int w = i == LINE_SEGMENTS - 1 ? startX + width - x : segmentWidth;
            GuiDraw.fill(context, new Rect(x, y, Math.max(1, w), 1), multiplyAlpha(accent, alpha));
        }
    }

    private static double adjustedBackgroundOpacity(GlobalSettings settings, HudStylePreset preset) {
        double opacity = settings.hudBackgroundOpacity();
        if (preset == HudStylePreset.CLEAN) {
            opacity *= 0.88D;
        } else if (preset == HudStylePreset.MINIMAL) {
            opacity *= 0.72D;
        }
        return MathHelper.clamp(opacity, 0.0D, 1.0D);
    }

    private static double adjustedBorderOpacity(GlobalSettings settings, HudStylePreset preset) {
        double opacity = settings.hudBorderOpacity();
        if (preset == HudStylePreset.CLEAN) {
            opacity *= 0.78D;
        } else if (preset == HudStylePreset.MINIMAL) {
            opacity *= 0.55D;
        }
        return MathHelper.clamp(opacity, 0.0D, 1.0D);
    }

    private static int titleWidth(TextRenderer textRenderer, String title) {
        double scale = GlobalSettings.INSTANCE.customFontEnabled() ? GlobalSettings.INSTANCE.textScale() : 1.0D;
        return (int) Math.ceil(textRenderer.getWidth(title) * scale);
    }

    private static int withAlpha(int rgb, double alpha) {
        int channel = (int) Math.round(MathHelper.clamp(alpha, 0.0D, 1.0D) * 255.0D);
        return channel << 24 | rgb & 0x00FFFFFF;
    }

    private static int multiplyAlpha(int color, double multiplier) {
        int alpha = color >>> 24;
        int nextAlpha = (int) Math.round(alpha * MathHelper.clamp(multiplier, 0.0D, 1.0D));
        return nextAlpha << 24 | color & 0x00FFFFFF;
    }
}
