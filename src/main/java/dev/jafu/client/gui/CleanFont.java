package dev.jafu.client.gui;

import dev.jafu.client.JafuClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CleanFont {
    public static final Identifier ID = Identifier.of(JafuClient.MOD_ID, "clean");
    private static final StyleSpriteSource.Font FONT = new StyleSpriteSource.Font(ID);

    private CleanFont() {
    }

    public static Text text(String text) {
        return Text.literal(text).styled(style -> style.withFont(FONT));
    }

    public static Text apply(Text text) {
        return text.copy().styled(style -> style.withFont(FONT));
    }

    public static void draw(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
        context.drawText(textRenderer, text(text), x, y, color, false);
    }
}
