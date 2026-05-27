package dev.jafu.client.gui;

import dev.jafu.client.gui.util.GuiDraw;
import dev.jafu.client.gui.util.Rect;
import dev.jafu.client.hud.HudLayoutStore;
import dev.jafu.client.hud.JafuHudElement;
import dev.jafu.client.hud.JafuHudElements;
import dev.jafu.client.module.JafuModules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class HudLayoutScreen extends Screen {
    private static final Text TITLE = Text.literal("JAFU HUD Layout");

    private final Screen parent;
    private JafuHudElement draggingElement;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudLayoutScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, width, height, 0xB0000000);
        drawToolbar(context, mouseX, mouseY);

        for (JafuHudElement element : JafuHudElements.all()) {
            drawElement(context, element, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        if (resetButton().contains(click.x(), click.y())) {
            HudLayoutStore.INSTANCE.resetAll();
            HudLayoutStore.INSTANCE.save();
            return true;
        }

        if (doneButton().contains(click.x(), click.y())) {
            close();
            return true;
        }

        for (int i = JafuHudElements.all().size() - 1; i >= 0; i--) {
            JafuHudElement element = JafuHudElements.all().get(i);
            Rect bounds = bounds(element);
            if (bounds.contains(click.x(), click.y())) {
                draggingElement = element;
                dragOffsetX = (int) click.x() - bounds.x();
                dragOffsetY = (int) click.y() - bounds.y();
                setDragging(true);
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingElement == null) {
            return super.mouseDragged(click, deltaX, deltaY);
        }

        HudLayoutStore.INSTANCE.move(
                draggingElement,
                (int) click.x() - dragOffsetX,
                (int) click.y() - dragOffsetY,
                draggingElement.width(),
                draggingElement.height(),
                width,
                height
        );
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingElement != null) {
            draggingElement = null;
            setDragging(false);
            HudLayoutStore.INSTANCE.save();
            return true;
        }

        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        HudLayoutStore.INSTANCE.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void drawToolbar(DrawContext context, int mouseX, int mouseY) {
        GuiDraw.text(context, textRenderer, "Drag HUD panels", 18, 16, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, "Positions save when you release the mouse.", 18, 30, JafuTheme.TEXT_MUTED);
        drawButton(context, resetButton(), "Reset", resetButton().contains(mouseX, mouseY));
        drawButton(context, doneButton(), "Done", doneButton().contains(mouseX, mouseY));
    }

    private void drawElement(DrawContext context, JafuHudElement element, int mouseX, int mouseY) {
        Rect bounds = bounds(element);
        boolean hovered = bounds.contains(mouseX, mouseY);
        boolean dragging = draggingElement == element;
        boolean enabled = JafuModules.isEnabled(element.moduleId());

        GuiDraw.fill(context, new Rect(bounds.x() - 1, bounds.y() - 1, bounds.width() + 2, bounds.height() + 2), dragging ? JafuTheme.ACCENT : hovered ? JafuTheme.BORDER : JafuTheme.BORDER_FAINT);
        GuiDraw.fill(context, bounds, enabled ? 0xDD101218 : 0xAA101218);
        GuiDraw.fill(context, new Rect(bounds.x(), bounds.y(), bounds.width(), 13), dragging ? JafuTheme.ACCENT_SOFT : JafuTheme.HEADER);
        GuiDraw.text(context, textRenderer, element.title(), bounds.x() + 6, bounds.y() + 4, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, enabled ? "Enabled" : "Disabled", bounds.x() + 6, bounds.y() + 18, enabled ? JafuTheme.GOOD : JafuTheme.TEXT_MUTED);
        GuiDraw.text(context, textRenderer, bounds.x() + ", " + bounds.y(), bounds.x() + 6, bounds.y() + 32, JafuTheme.TEXT_MUTED);
    }

    private Rect bounds(JafuHudElement element) {
        return HudLayoutStore.INSTANCE.bounds(element, element.width(), element.height(), width, height);
    }

    private Rect resetButton() {
        return new Rect(width - 114, 14, 44, 20);
    }

    private Rect doneButton() {
        return new Rect(width - 62, 14, 44, 20);
    }

    private void drawButton(DrawContext context, Rect button, String label, boolean hovered) {
        GuiDraw.fill(context, button, hovered ? JafuTheme.ACCENT_SOFT : JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, label, button.x() + 9, button.y() + 6, hovered ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
    }
}
