package dev.jafu.client.gui;

import java.util.List;

import dev.jafu.client.feature.general.chat.ChatEnhancementOption;
import dev.jafu.client.feature.general.chat.ChatEnhancementsSettings;
import dev.jafu.client.feature.general.globalsettings.GlobalFontOption;
import dev.jafu.client.feature.general.globalsettings.GlobalSettings;
import dev.jafu.client.feature.general.itemview.ItemViewSetting;
import dev.jafu.client.feature.general.itemview.ItemViewSettings;
import dev.jafu.client.feature.general.updater.AutoUpdater;
import dev.jafu.client.feature.general.updater.AutoUpdaterSettings;
import dev.jafu.client.feature.general.updater.UpdateChannel;
import dev.jafu.client.feature.mining.powder.PowderChestFeature;
import dev.jafu.client.feature.mining.powder.PowderChestSettings;
import dev.jafu.client.feature.mining.powder.PowderChestStatOption;
import dev.jafu.client.feature.mining.sacks.SacksStashOption;
import dev.jafu.client.feature.mining.sacks.SacksStashSettings;
import dev.jafu.client.gui.util.GuiDraw;
import dev.jafu.client.gui.util.Rect;
import dev.jafu.client.module.JafuCategory;
import dev.jafu.client.module.JafuModule;
import dev.jafu.client.module.JafuModules;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class JafuScreen extends Screen {
    private static final Text TITLE = Text.literal("JAFU");
    private static final String SUBTITLE = "Just A Few Updates";
    private static final String COMMAND = "/jafu";

    private JafuCategory selectedCategory = JafuCategory.GENERAL;
    private int selectedModuleIndex;
    private ItemViewSetting draggingItemViewSetting;
    private boolean draggingGlobalTextScale;
    private boolean draggingChatScale;
    private boolean globalFontDropdownOpen;

    public JafuScreen() {
        super(TITLE);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, width, height, 0xA0000000);

        JafuLayout layout = JafuLayout.fromScreen(width, height);
        drawFrame(context, layout);
        drawHeader(context, layout, mouseX, mouseY);
        drawCategories(context, layout);
        if (selectedCategory == JafuCategory.CREDITS) {
            drawCredits(context, layout);
        } else {
            drawModuleList(context, layout, mouseX, mouseY);
            drawModuleDetails(context, layout);
        }

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        JafuLayout layout = JafuLayout.fromScreen(width, height);
        if (layout.closeButton().contains(click.x(), click.y())) {
            close();
            return true;
        }

        if (layout.hudLayoutButton().contains(click.x(), click.y())) {
            client.setScreen(new HudLayoutScreen(this));
            return true;
        }

        if (selectCategory(layout, click)) {
            return true;
        }

        if (selectedCategory == JafuCategory.CREDITS) {
            return super.mouseClicked(click, doubled);
        }

        if (togglePowderTrackerOption(layout, click)) {
            return true;
        }

        if (toggleSacksStashTrackerOption(layout, click)) {
            return true;
        }

        if (toggleGlobalSettingsOption(layout, click)) {
            return true;
        }

        if (startItemViewSlider(layout, click)) {
            return true;
        }

        if (toggleChatEnhancementOption(layout, click)) {
            return true;
        }

        if (toggleAutoUpdaterOption(layout, click)) {
            return true;
        }

        if (toggleModule(layout, click)) {
            return true;
        }

        if (selectModule(layout, click)) {
            return true;
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (draggingItemViewSetting != null) {
            updateItemViewSlider(JafuLayout.fromScreen(width, height), draggingItemViewSetting, click.x());
            return true;
        }

        if (draggingGlobalTextScale) {
            updateGlobalTextScale(JafuLayout.fromScreen(width, height), click.x());
            return true;
        }

        if (draggingChatScale) {
            updateChatScale(JafuLayout.fromScreen(width, height), click.x());
            return true;
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingItemViewSetting != null) {
            draggingItemViewSetting = null;
            return true;
        }

        if (draggingGlobalTextScale) {
            draggingGlobalTextScale = false;
            return true;
        }

        if (draggingChatScale) {
            draggingChatScale = false;
            return true;
        }

        return super.mouseReleased(click);
    }

    private boolean selectCategory(JafuLayout layout, Click click) {
        JafuCategory[] categories = JafuCategory.values();
        for (int i = 0; i < categories.length; i++) {
            if (layout.categoryButton(i).contains(click.x(), click.y())) {
                selectedCategory = categories[i];
                selectedModuleIndex = 0;
                return true;
            }
        }
        return false;
    }

    private boolean selectModule(JafuLayout layout, Click click) {
        List<JafuModule> modules = visibleModules();
        for (int i = 0; i < modules.size(); i++) {
            if (layout.moduleRow(i).contains(click.x(), click.y())) {
                selectedModuleIndex = i;
                return true;
            }
        }
        return false;
    }

    private boolean toggleModule(JafuLayout layout, Click click) {
        List<JafuModule> modules = visibleModules();
        for (int i = 0; i < modules.size(); i++) {
            if (layout.moduleToggle(i).contains(click.x(), click.y())) {
                if (JafuModules.GLOBAL_SETTINGS.equals(modules.get(i).id())) {
                    selectedModuleIndex = i;
                    return true;
                }
                modules.get(i).toggle();
                selectedModuleIndex = i;
                return true;
            }
        }
        return false;
    }

    private void drawFrame(DrawContext context, JafuLayout layout) {
        Rect panel = layout.panel();
        GuiDraw.fill(context, new Rect(panel.x() - 1, panel.y() - 1, panel.width() + 2, panel.height() + 2), JafuTheme.SHADOW);
        GuiDraw.fill(context, panel, JafuTheme.PANEL);
        GuiDraw.fill(context, layout.sidebar(), JafuTheme.SIDEBAR);
        GuiDraw.fill(context, layout.header(), JafuTheme.HEADER);
        GuiDraw.horizontalLine(context, panel.x(), layout.header().bottom(), panel.width(), JafuTheme.BORDER);
        GuiDraw.verticalLine(context, layout.contentX(), layout.header().bottom(), panel.height() - layout.header().height(), JafuTheme.BORDER);
    }

    private void drawHeader(DrawContext context, JafuLayout layout, int mouseX, int mouseY) {
        Rect panel = layout.panel();
        Rect closeButton = layout.closeButton();
        boolean closeHovered = closeButton.contains(mouseX, mouseY);

        GuiDraw.text(context, textRenderer, TITLE.getString(), panel.x() + 16, panel.y() + 12, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, SUBTITLE, panel.x() + 52, panel.y() + 12, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, closeButton, closeHovered ? JafuTheme.CLOSE_HOVER : JafuTheme.CLOSE);
        GuiDraw.text(
                context,
                textRenderer,
                "x",
                closeButton.x() + 4,
                closeButton.y() + 3,
                closeHovered ? JafuTheme.CLOSE_TEXT_HOVER : JafuTheme.TEXT_MUTED
        );
    }

    private void drawCategories(DrawContext context, JafuLayout layout) {
        JafuCategory[] categories = JafuCategory.values();
        for (int i = 0; i < categories.length; i++) {
            Rect button = layout.categoryButton(i);
            boolean active = selectedCategory == categories[i];

            GuiDraw.fill(context, button, active ? JafuTheme.ACCENT_SOFT : 0x00000000);
            if (active) {
                GuiDraw.fill(context, new Rect(button.x(), button.y(), 3, button.height()), JafuTheme.ACCENT);
            }
            GuiDraw.text(
                    context,
                    textRenderer,
                    categories[i].label(),
                    button.x() + 12,
                    button.y() + 8,
                    active ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED
            );
        }

        Rect commandBadge = layout.commandBadge();
        GuiDraw.text(context, textRenderer, "Client command", commandBadge.x(), commandBadge.y() - 16, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, commandBadge, JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, COMMAND, commandBadge.x() + 34, commandBadge.y() + 6, JafuTheme.ACCENT);

        Rect hudLayoutButton = layout.hudLayoutButton();
        GuiDraw.fill(context, hudLayoutButton, JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, "Edit HUD", hudLayoutButton.x() + 34, hudLayoutButton.y() + 7, JafuTheme.TEXT_MUTED);
    }

    private void drawModuleList(DrawContext context, JafuLayout layout, int mouseX, int mouseY) {
        Rect panel = layout.panel();
        List<JafuModule> modules = visibleModules();
        GuiDraw.text(context, textRenderer, selectedCategory.title(), panel.x() + 166, panel.y() + 58, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, "Modules", panel.x() + 166, panel.y() + 74, JafuTheme.TEXT_MUTED);

        for (int i = 0; i < modules.size(); i++) {
            drawModuleRow(context, layout.moduleRow(i), modules.get(i), i, mouseX, mouseY);
        }

        GuiDraw.text(
                context,
                textRenderer,
                "Hypixel-safe defaults: informational UI only.",
                panel.x() + 166,
                layout.contentBottom() - 22,
                JafuTheme.TEXT_MUTED
        );
    }

    private void drawCredits(DrawContext context, JafuLayout layout) {
        Rect panel = layout.panel();
        Rect creditsPanel = new Rect(panel.x() + 166, panel.y() + 72, panel.right() - panel.x() - 184, panel.height() - 116);

        GuiDraw.text(context, textRenderer, selectedCategory.title(), panel.x() + 166, panel.y() + 58, JafuTheme.TEXT);
        GuiDraw.fill(context, creditsPanel, JafuTheme.PANEL_LIGHT);
        GuiDraw.horizontalLine(context, creditsPanel.x(), creditsPanel.y(), creditsPanel.width(), JafuTheme.ACCENT);

        int textX = creditsPanel.x() + 18;
        int textY = creditsPanel.y() + 28;
        String prefix = "Developed with ";
        String suffix = " by ";
        String name = "Chorey";
        int x = textX;

        GuiDraw.text(context, textRenderer, prefix, x, textY, JafuTheme.TEXT_MUTED);
        x += textRenderer.getWidth(prefix);
        GuiDraw.text(context, textRenderer, "\u2665", x, textY, JafuTheme.CLOSE_TEXT_HOVER);
        x += textRenderer.getWidth("\u2665");
        GuiDraw.text(context, textRenderer, suffix, x, textY, JafuTheme.TEXT_MUTED);
        x += textRenderer.getWidth(suffix);
        GuiDraw.text(context, textRenderer, name, x, textY, JafuTheme.ACCENT);
    }

    private void drawModuleRow(
            DrawContext context,
            Rect rowBounds,
            JafuModule module,
            int index,
            int mouseX,
            int mouseY
    ) {
        boolean selected = selectedModuleIndex == index;
        boolean hovered = rowBounds.contains(mouseX, mouseY);
        int background = selected ? JafuTheme.SELECTED_ROW : hovered ? JafuTheme.HOVERED_ROW : JafuTheme.PANEL_LIGHT;

        GuiDraw.fill(context, rowBounds, background);
        GuiDraw.horizontalLine(context, rowBounds.x(), rowBounds.y(), rowBounds.width(), selected ? JafuTheme.ACCENT : JafuTheme.BORDER_FAINT);
        GuiDraw.fill(context, new Rect(rowBounds.x() + 10, rowBounds.y() + 11, 8, 8), module.color());
        GuiDraw.text(context, textRenderer, module.name(), rowBounds.x() + 28, rowBounds.y() + 8, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, module.description(), rowBounds.x() + 28, rowBounds.y() + 21, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, new Rect(rowBounds.right() - 38, rowBounds.y() + 8, 30, 20), module.enabled() ? JafuTheme.ACCENT_SOFT : JafuTheme.CONTROL);
        GuiDraw.text(
                context,
                textRenderer,
                module.enabled() ? "ON" : "OFF",
                rowBounds.right() - 31,
                rowBounds.y() + 14,
                module.enabled() ? JafuTheme.GOOD : JafuTheme.TEXT_MUTED
        );
    }

    private void drawModuleDetails(DrawContext context, JafuLayout layout) {
        Rect detailPanel = layout.detailPanel();
        JafuModule selectedModule = selectedModule();

        GuiDraw.fill(context, detailPanel, JafuTheme.PANEL_LIGHT);
        GuiDraw.horizontalLine(context, detailPanel.x(), detailPanel.y(), detailPanel.width(), JafuTheme.ACCENT);
        GuiDraw.text(context, textRenderer, selectedModule.name(), detailPanel.x() + 16, detailPanel.y() + 16, JafuTheme.TEXT);
        GuiDraw.text(
                context,
                textRenderer,
                selectedModule.enabled() ? "Enabled" : "Disabled",
                detailPanel.x() + 16,
                detailPanel.y() + 32,
                selectedModule.enabled() ? JafuTheme.GOOD : JafuTheme.WARN
        );

        if (JafuModules.POWDER_CHEST_TRACKER.equals(selectedModule.id())) {
            drawPowderTrackerOptions(context, detailPanel);
        } else if (JafuModules.SACKS_STASH_TRACKER.equals(selectedModule.id())) {
            drawSacksStashTrackerOptions(context, detailPanel);
        } else if (JafuModules.GLOBAL_SETTINGS.equals(selectedModule.id())) {
            drawGlobalSettingsOptions(context, detailPanel);
        } else if (JafuModules.ITEM_VIEW.equals(selectedModule.id())) {
            drawItemViewOptions(context, detailPanel);
        } else if (JafuModules.CHAT_ENHANCEMENTS.equals(selectedModule.id())) {
            drawChatEnhancementOptions(context, detailPanel);
        } else if (JafuModules.AUTO_UPDATER.equals(selectedModule.id())) {
            drawAutoUpdaterOptions(context, detailPanel);
        } else {
            drawPreview(context, detailPanel);
        }
        drawStatus(context, layout);
    }

    private void drawPowderTrackerOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Tracker fields", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);
        List<PowderChestStatOption> options = PowderChestStatOption.all();
        for (int i = 0; i < options.size(); i++) {
            PowderChestStatOption option = options.get(i);
            Rect row = powderTrackerOptionRow(detailPanel, i);
            Rect checkbox = new Rect(row.x(), row.y() + 3, 10, 10);
            boolean visible = PowderChestSettings.INSTANCE.isVisible(option);

            GuiDraw.fill(context, checkbox, visible ? JafuTheme.ACCENT_SOFT : JafuTheme.CONTROL);
            GuiDraw.fill(context, new Rect(checkbox.x() + 2, checkbox.y() + 2, 6, 6), visible ? JafuTheme.ACCENT : JafuTheme.BORDER);
            GuiDraw.text(context, textRenderer, option.label(), row.x() + 18, row.y() + 4, visible ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
        }

        Rect resetButton = powderTrackerResetButton(detailPanel);
        GuiDraw.text(context, textRenderer, "Session", detailPanel.x() + 16, resetButton.y() - 16, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, resetButton, JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, "Reset stats", resetButton.x() + 13, resetButton.y() + 6, JafuTheme.WARN);
    }

    private void drawGlobalSettingsOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Shared text settings", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);

        Rect fontRow = trackerOptionRow(detailPanel, 0);
        Rect fontControl = globalFontControl(detailPanel);
        GuiDraw.text(context, textRenderer, "Nice font", fontRow.x(), fontRow.y() + 4, JafuTheme.TEXT);
        GuiDraw.fill(context, fontControl, JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, GlobalSettings.INSTANCE.font().label(), fontControl.x() + 8, fontControl.y() + 4, JafuTheme.ACCENT);
        GuiDraw.text(context, textRenderer, "v", fontControl.right() - 10, fontControl.y() + 4, JafuTheme.TEXT_MUTED);

        Rect sizeRow = trackerOptionRow(detailPanel, 1);
        Rect slider = globalTextScaleSlider(detailPanel);
        double value = GlobalSettings.INSTANCE.textScale();
        double percent = (value - GlobalSettings.MIN_TEXT_SCALE) / (GlobalSettings.MAX_TEXT_SCALE - GlobalSettings.MIN_TEXT_SCALE);
        int knobX = slider.x() + (int) Math.round(percent * slider.width());

        GuiDraw.text(context, textRenderer, "Text size", sizeRow.x(), sizeRow.y() + 4, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, formatScale(value), sizeRow.right() - 34, sizeRow.y() + 4, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, slider, JafuTheme.CONTROL);
        GuiDraw.fill(context, new Rect(slider.x(), slider.y(), knobX - slider.x(), slider.height()), JafuTheme.ACCENT_SOFT);
        GuiDraw.fill(context, new Rect(knobX - 2, slider.y() - 2, 4, slider.height() + 4), JafuTheme.ACCENT);

        if (globalFontDropdownOpen) {
            drawGlobalFontDropdown(context, detailPanel);
        }
    }

    private void drawGlobalFontDropdown(DrawContext context, Rect detailPanel) {
        Rect control = globalFontControl(detailPanel);
        GlobalFontOption[] options = GlobalFontOption.values();
        for (int i = 0; i < options.length; i++) {
            Rect row = globalFontDropdownRow(control, i);
            boolean selected = options[i] == GlobalSettings.INSTANCE.font();
            GuiDraw.fill(context, row, selected ? JafuTheme.ACCENT_SOFT : JafuTheme.CONTROL);
            GuiDraw.text(context, textRenderer, options[i].label(), row.x() + 8, row.y() + 4, selected ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
        }
    }

    private void drawItemViewOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Held item model", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);
        List<ItemViewSetting> settings = ItemViewSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            ItemViewSetting setting = settings.get(i);
            Rect row = trackerOptionRow(detailPanel, i);
            Rect slider = itemViewSlider(detailPanel, i);
            double value = ItemViewSettings.INSTANCE.value(setting);
            double percent = (value - setting.min()) / (setting.max() - setting.min());
            int knobX = slider.x() + (int) Math.round(percent * slider.width());

            GuiDraw.text(context, textRenderer, setting.label(), row.x(), row.y() + 4, JafuTheme.TEXT);
            GuiDraw.text(context, textRenderer, formatSliderValue(setting, value), row.right() - 42, row.y() + 4, JafuTheme.TEXT_MUTED);
            GuiDraw.fill(context, slider, JafuTheme.CONTROL);
            GuiDraw.fill(context, new Rect(slider.x(), slider.y(), knobX - slider.x(), slider.height()), JafuTheme.ACCENT_SOFT);
            GuiDraw.fill(context, new Rect(knobX - 2, slider.y() - 2, 4, slider.height() + 4), JafuTheme.ACCENT);
        }
    }

    private void drawChatEnhancementOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Chat options", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);
        List<ChatEnhancementOption> options = ChatEnhancementOption.all();
        for (int i = 0; i < options.size(); i++) {
            ChatEnhancementOption option = options.get(i);
            Rect row = trackerOptionRow(detailPanel, i);
            Rect checkbox = new Rect(row.x(), row.y() + 3, 10, 10);
            boolean enabled = ChatEnhancementsSettings.INSTANCE.isEnabled(option);

            GuiDraw.fill(context, checkbox, enabled ? JafuTheme.ACCENT_SOFT : JafuTheme.CONTROL);
            GuiDraw.fill(context, new Rect(checkbox.x() + 2, checkbox.y() + 2, 6, 6), enabled ? JafuTheme.ACCENT : JafuTheme.BORDER);
            GuiDraw.text(context, textRenderer, option.label(), row.x() + 18, row.y() + 4, enabled ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
        }

        Rect sizeRow = chatScaleRow(detailPanel);
        Rect slider = chatScaleSlider(detailPanel);
        double value = ChatEnhancementsSettings.INSTANCE.configuredChatScale();
        double percent = (value - ChatEnhancementsSettings.MIN_CHAT_SCALE)
                / (ChatEnhancementsSettings.MAX_CHAT_SCALE - ChatEnhancementsSettings.MIN_CHAT_SCALE);
        int knobX = slider.x() + (int) Math.round(percent * slider.width());

        GuiDraw.text(context, textRenderer, "Chat size", sizeRow.x(), sizeRow.y() + 4, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, formatScale(value), sizeRow.right() - 34, sizeRow.y() + 4, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, slider, JafuTheme.CONTROL);
        GuiDraw.fill(context, new Rect(slider.x(), slider.y(), knobX - slider.x(), slider.height()), JafuTheme.ACCENT_SOFT);
        GuiDraw.fill(context, new Rect(knobX - 2, slider.y() - 2, 4, slider.height() + 4), JafuTheme.ACCENT);
    }

    private void drawSacksStashTrackerOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Tracker fields", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);
        List<SacksStashOption> options = SacksStashOption.all();
        for (int i = 0; i < options.size(); i++) {
            SacksStashOption option = options.get(i);
            Rect row = trackerOptionRow(detailPanel, i);
            Rect checkbox = new Rect(row.x(), row.y() + 3, 10, 10);
            boolean visible = SacksStashSettings.INSTANCE.isVisible(option);

            GuiDraw.fill(context, checkbox, visible ? JafuTheme.ACCENT_SOFT : JafuTheme.CONTROL);
            GuiDraw.fill(context, new Rect(checkbox.x() + 2, checkbox.y() + 2, 6, 6), visible ? JafuTheme.ACCENT : JafuTheme.BORDER);
            GuiDraw.text(context, textRenderer, option.label(), row.x() + 18, row.y() + 4, visible ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
        }
    }

    private void drawAutoUpdaterOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Release channel", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);

        Rect row = trackerOptionRow(detailPanel, 0);
        Rect control = new Rect(row.right() - 72, row.y(), 72, 16);
        UpdateChannel channel = AutoUpdaterSettings.INSTANCE.channel();

        GuiDraw.text(context, textRenderer, "Channel", row.x(), row.y() + 4, JafuTheme.TEXT);
        GuiDraw.fill(context, control, JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, channel.label(), control.x() + 12, control.y() + 4, JafuTheme.ACCENT);

        GuiDraw.text(context, textRenderer, "Commands", detailPanel.x() + 16, detailPanel.y() + 120, JafuTheme.TEXT_MUTED);
        GuiDraw.text(context, textRenderer, "/jafu update check", detailPanel.x() + 16, detailPanel.y() + 140, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, "/jafu update snapshot", detailPanel.x() + 16, detailPanel.y() + 158, JafuTheme.TEXT_MUTED);
    }

    private void drawPreview(DrawContext context, Rect detailPanel) {
        int previewY = detailPanel.y() + 62;
        Rect preview = new Rect(detailPanel.x() + 16, previewY + 16, detailPanel.width() - 32, 72);

        GuiDraw.text(context, textRenderer, "Preview", detailPanel.x() + 16, previewY, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, preview, JafuTheme.PREVIEW);
        GuiDraw.text(context, textRenderer, "Compact cards, dark panels,", preview.x() + 12, preview.y() + 14, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, "accent rails, and clear states.", preview.x() + 12, preview.y() + 28, JafuTheme.TEXT_MUTED);
        GuiDraw.text(context, textRenderer, "Ready for real feature wiring.", preview.x() + 12, preview.y() + 48, JafuTheme.ACCENT);
    }

    private void drawStatus(DrawContext context, JafuLayout layout) {
        Rect panel = layout.panel();
        Rect status = new Rect(panel.x() + 400, panel.bottom() - 76, panel.right() - panel.x() - 434, 20);
        GuiDraw.fill(context, status, JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, "Status: scaffolded", status.x() + 12, status.y() + 6, JafuTheme.GOOD);
    }

    private JafuModule selectedModule() {
        List<JafuModule> modules = visibleModules();
        if (modules.isEmpty()) {
            throw new IllegalStateException("No module is selected for " + selectedCategory);
        }
        int safeIndex = MathHelper.clamp(selectedModuleIndex, 0, modules.size() - 1);
        return modules.get(safeIndex);
    }

    private List<JafuModule> visibleModules() {
        List<JafuModule> modules = JafuModules.byCategory(selectedCategory);
        if (selectedModuleIndex >= modules.size()) {
            selectedModuleIndex = Math.max(0, modules.size() - 1);
        }
        return modules;
    }

    private boolean togglePowderTrackerOption(JafuLayout layout, Click click) {
        JafuModule selectedModule = selectedModule();
        if (!JafuModules.POWDER_CHEST_TRACKER.equals(selectedModule.id())) {
            return false;
        }

        Rect detailPanel = layout.detailPanel();
        List<PowderChestStatOption> options = PowderChestStatOption.all();
        for (int i = 0; i < options.size(); i++) {
            if (powderTrackerOptionRow(detailPanel, i).contains(click.x(), click.y())) {
                PowderChestSettings.INSTANCE.toggle(options.get(i));
                return true;
            }
        }

        if (powderTrackerResetButton(detailPanel).contains(click.x(), click.y())) {
            PowderChestFeature.resetTracker();
            return true;
        }
        return false;
    }

    private boolean toggleSacksStashTrackerOption(JafuLayout layout, Click click) {
        JafuModule selectedModule = selectedModule();
        if (!JafuModules.SACKS_STASH_TRACKER.equals(selectedModule.id())) {
            return false;
        }

        Rect detailPanel = layout.detailPanel();
        List<SacksStashOption> options = SacksStashOption.all();
        for (int i = 0; i < options.size(); i++) {
            if (trackerOptionRow(detailPanel, i).contains(click.x(), click.y())) {
                SacksStashSettings.INSTANCE.toggle(options.get(i));
                return true;
            }
        }
        return false;
    }

    private boolean toggleGlobalSettingsOption(JafuLayout layout, Click click) {
        JafuModule selectedModule = selectedModule();
        if (!JafuModules.GLOBAL_SETTINGS.equals(selectedModule.id())) {
            globalFontDropdownOpen = false;
            return false;
        }

        Rect detailPanel = layout.detailPanel();
        Rect fontControl = globalFontControl(detailPanel);
        if (globalFontDropdownOpen) {
            GlobalFontOption[] options = GlobalFontOption.values();
            for (int i = 0; i < options.length; i++) {
                if (globalFontDropdownRow(fontControl, i).contains(click.x(), click.y())) {
                    GlobalSettings.INSTANCE.setFont(options[i]);
                    globalFontDropdownOpen = false;
                    return true;
                }
            }
        }

        if (fontControl.contains(click.x(), click.y())) {
            globalFontDropdownOpen = !globalFontDropdownOpen;
            return true;
        }

        globalFontDropdownOpen = false;
        if (trackerOptionRow(detailPanel, 1).contains(click.x(), click.y())) {
            draggingGlobalTextScale = true;
            updateGlobalTextScale(layout, click.x());
            return true;
        }
        return false;
    }

    private boolean startItemViewSlider(JafuLayout layout, Click click) {
        JafuModule selectedModule = selectedModule();
        if (!JafuModules.ITEM_VIEW.equals(selectedModule.id())) {
            return false;
        }

        Rect detailPanel = layout.detailPanel();
        List<ItemViewSetting> settings = ItemViewSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            Rect row = trackerOptionRow(detailPanel, i);
            if (row.contains(click.x(), click.y())) {
                draggingItemViewSetting = settings.get(i);
                updateItemViewSlider(layout, draggingItemViewSetting, click.x());
                return true;
            }
        }
        return false;
    }

    private boolean toggleChatEnhancementOption(JafuLayout layout, Click click) {
        JafuModule selectedModule = selectedModule();
        if (!JafuModules.CHAT_ENHANCEMENTS.equals(selectedModule.id())) {
            return false;
        }

        Rect detailPanel = layout.detailPanel();
        List<ChatEnhancementOption> options = ChatEnhancementOption.all();
        for (int i = 0; i < options.size(); i++) {
            if (trackerOptionRow(detailPanel, i).contains(click.x(), click.y())) {
                ChatEnhancementsSettings.INSTANCE.toggle(options.get(i));
                return true;
            }
        }

        if (chatScaleRow(detailPanel).contains(click.x(), click.y())) {
            draggingChatScale = true;
            updateChatScale(layout, click.x());
            return true;
        }
        return false;
    }

    private boolean toggleAutoUpdaterOption(JafuLayout layout, Click click) {
        JafuModule selectedModule = selectedModule();
        if (!JafuModules.AUTO_UPDATER.equals(selectedModule.id())) {
            return false;
        }

        Rect detailPanel = layout.detailPanel();
        if (trackerOptionRow(detailPanel, 0).contains(click.x(), click.y())) {
            UpdateChannel channel = AutoUpdaterSettings.INSTANCE.cycleChannel();
            AutoUpdater.notifyPlayer("JAFU updater channel set to " + channel.label() + ".");
            return true;
        }
        return false;
    }

    private void updateItemViewSlider(JafuLayout layout, ItemViewSetting setting, double mouseX) {
        Rect slider = itemViewSlider(layout.detailPanel(), ItemViewSetting.all().indexOf(setting));
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = setting.min() + percent * (setting.max() - setting.min());
        ItemViewSettings.INSTANCE.setValue(setting, value);
    }

    private void updateGlobalTextScale(JafuLayout layout, double mouseX) {
        Rect slider = globalTextScaleSlider(layout.detailPanel());
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = GlobalSettings.MIN_TEXT_SCALE
                + percent * (GlobalSettings.MAX_TEXT_SCALE - GlobalSettings.MIN_TEXT_SCALE);
        GlobalSettings.INSTANCE.setTextScale(value);
    }

    private void updateChatScale(JafuLayout layout, double mouseX) {
        Rect slider = chatScaleSlider(layout.detailPanel());
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = ChatEnhancementsSettings.MIN_CHAT_SCALE
                + percent * (ChatEnhancementsSettings.MAX_CHAT_SCALE - ChatEnhancementsSettings.MIN_CHAT_SCALE);
        ChatEnhancementsSettings.INSTANCE.setChatScale(value);
    }

    private static Rect powderTrackerOptionRow(Rect detailPanel, int index) {
        return trackerOptionRow(detailPanel, index);
    }

    private static Rect powderTrackerResetButton(Rect detailPanel) {
        Rect row = trackerOptionRow(detailPanel, PowderChestStatOption.all().size() + 1);
        return new Rect(row.x(), row.y(), 82, 18);
    }

    private static Rect trackerOptionRow(Rect detailPanel, int index) {
        return new Rect(detailPanel.x() + 16, detailPanel.y() + 78 + index * 20, detailPanel.width() - 32, 16);
    }

    private static Rect itemViewSlider(Rect detailPanel, int index) {
        Rect row = trackerOptionRow(detailPanel, index);
        return new Rect(row.x() + 48, row.y() + 7, Math.max(30, row.width() - 96), 4);
    }

    private static Rect globalFontControl(Rect detailPanel) {
        Rect row = trackerOptionRow(detailPanel, 0);
        return new Rect(row.right() - 82, row.y(), 82, 16);
    }

    private static Rect globalFontDropdownRow(Rect control, int index) {
        return new Rect(control.x(), control.bottom() + 2 + index * 16, control.width(), 16);
    }

    private static Rect globalTextScaleSlider(Rect detailPanel) {
        Rect row = trackerOptionRow(detailPanel, 1);
        return new Rect(row.x() + 62, row.y() + 7, Math.max(30, row.width() - 108), 4);
    }

    private static Rect chatScaleRow(Rect detailPanel) {
        return trackerOptionRow(detailPanel, ChatEnhancementOption.all().size() + 1);
    }

    private static Rect chatScaleSlider(Rect detailPanel) {
        Rect row = chatScaleRow(detailPanel);
        return new Rect(row.x() + 62, row.y() + 7, Math.max(30, row.width() - 108), 4);
    }

    private static String formatSliderValue(ItemViewSetting setting, double value) {
        if (setting == ItemViewSetting.SPEED) {
            return String.format("%.2f", value);
        }
        return Integer.toString((int) Math.round(value));
    }

    private static String formatScale(double value) {
        return String.format("%.2f", value);
    }
}
