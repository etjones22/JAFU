package dev.jafu.client.gui;

import dev.jafu.client.feature.general.globalsettings.ClickGuiVersion;
import dev.jafu.client.feature.general.globalsettings.GlobalSettings;
import dev.jafu.client.gui.util.GuiDraw;
import dev.jafu.client.gui.util.Rect;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class JafuV2Screen extends Screen {
    private static final Text TITLE = Text.literal("JAFU V2");

    public JafuV2Screen() {
        super(TITLE);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        Rect panel = panel();
        Rect closeButton = closeButton(panel);
        Rect card = new Rect(panel.x() + 28, panel.y() + 74, panel.width() - 56, panel.height() - 112);

        context.fill(0, 0, width, height, withAlpha(0x000000, 0.62D));
        GuiDraw.fill(context, panel, JafuTheme.PANEL);
        GuiDraw.fill(context, new Rect(panel.x(), panel.y(), panel.width(), 48), JafuTheme.PANEL);
        GuiDraw.horizontalLine(context, panel.x(), panel.y() + 48, panel.width(), JafuTheme.BORDER);
        GuiDraw.text(context, textRenderer, "JAFU V2", panel.x() + 26, panel.y() + 20, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, "New ClickGUI", panel.x() + 84, panel.y() + 20, JafuTheme.TEXT_MUTED);

        GuiDraw.fill(context, closeButton, JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, "x", closeButton.x() + 5, closeButton.y() + 3, JafuTheme.TEXT_MUTED);

        GuiDraw.fill(context, card, JafuTheme.PANEL_LIGHT);
        GuiDraw.horizontalLine(context, card.x(), card.y(), card.width(), JafuTheme.ACCENT);
        GuiDraw.text(context, textRenderer, "Global options", card.x() + 18, card.y() + 22, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, "ClickGUI", versionRow(panel).x(), versionRow(panel).y() + 4, JafuTheme.TEXT);
        drawVersionButton(context, panel, ClickGuiVersion.V1);
        drawVersionButton(context, panel, ClickGuiVersion.V2);

        GuiDraw.text(context, textRenderer, "V2 shell ready for the full redesign.", card.x() + 18, card.bottom() - 34, JafuTheme.TEXT_MUTED);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        Rect panel = panel();
        if (closeButton(panel).contains(click.x(), click.y())) {
            client.setScreen(null);
            return true;
        }
        if (versionButton(panel, ClickGuiVersion.V1).contains(click.x(), click.y())) {
            JafuScreens.switchTo(ClickGuiVersion.V1);
            return true;
        }
        if (versionButton(panel, ClickGuiVersion.V2).contains(click.x(), click.y())) {
            JafuScreens.switchTo(ClickGuiVersion.V2);
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void drawVersionButton(DrawContext context, Rect panel, ClickGuiVersion version) {
        Rect button = versionButton(panel, version);
        boolean selected = GlobalSettings.INSTANCE.clickGuiVersion() == version;
        GuiDraw.fill(context, button, selected ? JafuTheme.ACCENT_SOFT : JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, version.label(), button.x() + 13, button.y() + 5, selected ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
    }

    private Rect panel() {
        int panelWidth = Math.min(520, width - 36);
        int panelHeight = Math.min(300, height - 36);
        return new Rect((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private static Rect closeButton(Rect panel) {
        return new Rect(panel.right() - 26, panel.y() + 13, 14, 14);
    }

    private static Rect versionRow(Rect panel) {
        return new Rect(panel.x() + 46, panel.y() + 126, panel.width() - 92, 20);
    }

    private static Rect versionButton(Rect panel, ClickGuiVersion version) {
        Rect row = versionRow(panel);
        int buttonWidth = 42;
        int x = row.right() - buttonWidth * 2 + version.ordinal() * buttonWidth;
        return new Rect(x, row.y(), buttonWidth, 20);
    }

    private static int withAlpha(int rgb, double alpha) {
        int a = (int) Math.round(Math.max(0.0D, Math.min(1.0D, alpha)) * 255.0D);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
