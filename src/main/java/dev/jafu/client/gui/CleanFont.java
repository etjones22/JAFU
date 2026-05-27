package dev.jafu.client.gui;

import dev.jafu.client.JafuClient;
import dev.jafu.client.feature.general.globalsettings.GlobalSettings;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CleanFont {
    public static final Identifier ID = Identifier.of(JafuClient.MOD_ID, "clean");

    private CleanFont() {
    }

    public static Text text(String text) {
        return Text.literal(text).styled(style -> style.withFont(GlobalSettings.INSTANCE.font().font()));
    }

    public static Text apply(Text text) {
        return text.copy().styled(style -> style.withFont(GlobalSettings.INSTANCE.font().font()));
    }

    public static void draw(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
        draw(context, textRenderer, text, x, y, color, GlobalSettings.INSTANCE.textScale());
    }

    public static void draw(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color, double scale) {
        if (Math.abs(scale - 1.0D) <= 0.001D) {
            context.drawText(textRenderer, text(text), x, y, color, false);
            return;
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().translate((float) x, (float) y);
        context.getMatrices().scale((float) scale, (float) scale);
        context.drawText(textRenderer, text(text), 0, 0, color, false);
        context.getMatrices().popMatrix();
    }
}
