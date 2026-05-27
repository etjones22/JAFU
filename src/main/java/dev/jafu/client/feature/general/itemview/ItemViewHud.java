package dev.jafu.client.feature.general.itemview;

import dev.jafu.client.gui.JafuTheme;
import dev.jafu.client.gui.util.GuiDraw;
import dev.jafu.client.gui.util.Rect;
import dev.jafu.client.hud.HudLayoutStore;
import dev.jafu.client.hud.JafuHudElements;
import dev.jafu.client.module.JafuModules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;

public final class ItemViewHud {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 150;

    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!JafuModules.isEnabled(JafuModules.ITEM_VIEW) || client.player == null || client.options.hudHidden) {
            return;
        }

        ItemStack stack = viewedStack(client.player);
        if (stack.isEmpty()) {
            return;
        }

        Rect bounds = HudLayoutStore.INSTANCE.bounds(
                JafuHudElements.ITEM_VIEW,
                WIDTH,
                HEIGHT,
                client.getWindow().getScaledWidth(),
                client.getWindow().getScaledHeight()
        );

        GuiDraw.fill(context, bounds, 0x8A101218);
        drawItem(context, stack, bounds);
        drawLabel(context, client.textRenderer, stack, bounds);
    }

    private static ItemStack viewedStack(ClientPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty()) {
            return mainHand;
        }
        return player.getOffHandStack();
    }

    private static void drawItem(DrawContext context, ItemStack stack, Rect bounds) {
        ItemViewSettings settings = ItemViewSettings.INSTANCE;
        float itemSize = (float) settings.value(ItemViewSetting.SIZE);
        float scale = itemSize / 16.0F;
        float centerX = bounds.x() + bounds.width() / 2.0F + settings.intValue(ItemViewSetting.OFFSET_X);
        float centerY = bounds.y() + bounds.height() / 2.0F + settings.intValue(ItemViewSetting.OFFSET_Y);
        float rotation = (System.currentTimeMillis() % 360_000L) / 1000.0F * (float) settings.value(ItemViewSetting.SPEED);
        int z = settings.intValue(ItemViewSetting.OFFSET_Z);

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, centerY);
        context.getMatrices().rotate(rotation);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-8.0F, -8.0F);
        context.drawItemWithoutEntity(stack, 0, 0, z);
        context.getMatrices().popMatrix();
    }

    private static void drawLabel(DrawContext context, TextRenderer textRenderer, ItemStack stack, Rect bounds) {
        String name = stack.getName().getString();
        int maxWidth = bounds.width() - 12;
        String label = trimToWidth(textRenderer, name, maxWidth);
        GuiDraw.text(context, textRenderer, label, bounds.x() + 6, bounds.bottom() - 14, JafuTheme.TEXT_MUTED);
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
