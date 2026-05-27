package dev.jafu.client.gui.util;

import dev.jafu.client.feature.general.guisettings.GuiSettings;
import dev.jafu.client.gui.CleanFont;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public final class GuiDraw {
    private GuiDraw() {
    }

    public static void fill(DrawContext context, Rect rect, int color) {
        context.fill(rect.left(), rect.top(), rect.right(), rect.bottom(), color);
    }

    public static void horizontalLine(DrawContext context, int x, int y, int width, int color) {
        context.fill(x, y, x + width, y + 1, color);
    }

    public static void verticalLine(DrawContext context, int x, int y, int height, int color) {
        context.fill(x, y, x + 1, y + height, color);
    }

    public static void text(
            DrawContext context,
            TextRenderer textRenderer,
            String text,
            int x,
            int y,
            int color
    ) {
        if (GuiSettings.INSTANCE.customFontEnabled()) {
            CleanFont.draw(context, textRenderer, text, x, y, color, 1.0D);
            return;
        }
        context.drawText(textRenderer, text, x, y, color, false);
    }
}
