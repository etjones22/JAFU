package dev.jafu.client.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import dev.jafu.client.feature.general.chat.ChatEnhancementOption;
import dev.jafu.client.feature.general.chat.ChatEnhancementsSettings;
import dev.jafu.client.feature.general.globalsettings.ClickGuiVersion;
import dev.jafu.client.feature.general.globalsettings.GlobalFontOption;
import dev.jafu.client.feature.general.globalsettings.GlobalSettings;
import dev.jafu.client.feature.general.itemview.ItemViewSetting;
import dev.jafu.client.feature.general.itemview.ItemViewSettings;
import dev.jafu.client.feature.general.updater.AutoUpdater;
import dev.jafu.client.feature.general.updater.AutoUpdaterSettings;
import dev.jafu.client.feature.general.updater.UpdateChannel;
import dev.jafu.client.feature.garden.rng.GardenDropFamily;
import dev.jafu.client.feature.garden.rng.GardenDropState;
import dev.jafu.client.feature.garden.rng.GardenRngFeature;
import dev.jafu.client.feature.garden.rng.GardenRngSettings;
import dev.jafu.client.feature.garden.rng.GardenRngStatOption;
import dev.jafu.client.feature.gui.scrollabletooltips.ScrollableTooltipColorOption;
import dev.jafu.client.feature.gui.scrollabletooltips.ScrollableTooltipNumericSetting;
import dev.jafu.client.feature.gui.scrollabletooltips.ScrollableTooltipsSettings;
import dev.jafu.client.feature.mining.powder.PowderChestFeature;
import dev.jafu.client.feature.mining.powder.PowderChestSettings;
import dev.jafu.client.feature.mining.powder.PowderChestStatOption;
import dev.jafu.client.feature.mining.sacks.SacksStashOption;
import dev.jafu.client.feature.mining.sacks.SacksStashSettings;
import dev.jafu.client.feature.qol.cooldown.CooldownDisplayColorOption;
import dev.jafu.client.feature.qol.cooldown.CooldownDisplayOption;
import dev.jafu.client.feature.qol.cooldown.CooldownDisplaySettings;
import dev.jafu.client.feature.qol.itemvalue.ItemValueOverlayFeature;
import dev.jafu.client.feature.qol.itemvalue.ItemValueNumericSetting;
import dev.jafu.client.feature.qol.itemvalue.ItemValueOverlayOption;
import dev.jafu.client.feature.qol.itemvalue.ItemValueOverlaySettings;
import dev.jafu.client.feature.qol.itemvalue.PriceCacheStatus;
import dev.jafu.client.feature.qol.modhider.ModHiderSettings;
import dev.jafu.client.feature.qol.tokenguard.TokenGuardSettings;
import dev.jafu.client.feature.skyblock.storage.StorageIndexStore;
import dev.jafu.client.feature.skyblock.storage.StorageSnapshot;
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
    private static final Text TITLE = Text.literal("JAFU V1");
    private static final String SUBTITLE = "Just A Few Updates";
    private static final String COMMAND = "/jafu";
    private static final String MOD_HIDER_TOOLTIP = "Blocks ModAnnouncer packets that mods like Firament send.";
    private static final String TOKEN_GUARD_TOOLTIP = "Best effort only: mods in the same JVM can still try reflection or lower-level tricks.";
    private static final long SCREEN_ANIMATION_MILLIS = 180L;
    private static final long PANEL_TRANSITION_MILLIS = 160L;

    private JafuCategory selectedCategory = JafuCategory.GENERAL;
    private int selectedModuleIndex;
    private ItemViewSetting draggingItemViewSetting;
    private ScrollableTooltipNumericSetting draggingTooltipSetting;
    private ItemValueNumericSetting draggingItemValueSetting;
    private boolean draggingPowderAutoResetSeconds;
    private boolean draggingCooldownSeconds;
    private boolean draggingGlobalTextScale;
    private boolean draggingChatScale;
    private boolean globalFontDropdownOpen;
    private final long openedAtMillis = System.currentTimeMillis();
    private long closingStartedAtMillis = -1L;
    private long categoryTransitionStartedAtMillis = openedAtMillis;
    private long detailTransitionStartedAtMillis = openedAtMillis;
    private long lastFrameMillis;
    private double animationBlend = 1.0D;
    private double globalFontDropdownAnimation;
    private final Map<String, Double> animatedProgress = new HashMap<>();
    private final Map<String, Double> animatedSliders = new HashMap<>();

    public JafuScreen() {
        super(TITLE);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        long nowMillis = System.currentTimeMillis();
        updateAnimationClock(nowMillis);
        if (closingStartedAtMillis >= 0L && normalized(nowMillis - closingStartedAtMillis, SCREEN_ANIMATION_MILLIS) >= 1.0D) {
            client.setScreen(null);
            return;
        }

        JafuLayout layout = JafuLayout.fromScreen(width, height);
        double screenProgress = screenProgress(nowMillis);
        float easedScreenProgress = easeOut((float) screenProgress);
        context.fill(0, 0, width, height, withAlpha(0x000000, 0.62D * easedScreenProgress));

        float scale = 0.965F + easedScreenProgress * 0.035F;
        float centerX = layout.panel().x() + layout.panel().width() / 2.0F;
        float centerY = layout.panel().y() + layout.panel().height() / 2.0F;

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(centerX, centerY);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-centerX, -centerY);
        drawFrame(context, layout);
        drawHeader(context, layout, mouseX, mouseY);
        drawCategories(context, layout);
        if (selectedCategory == JafuCategory.CREDITS) {
            drawCredits(context, layout);
        } else {
            drawModuleList(context, layout, mouseX, mouseY);
            drawModuleDetails(context, layout);
        }
        context.getMatrices().popMatrix();

        super.render(context, mouseX, mouseY, deltaTicks);
        drawHoverTooltip(context, layout, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closingStartedAtMillis >= 0L) {
            return true;
        }

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

        if (toggleGardenRngOption(layout, click)) {
            return true;
        }

        if (toggleGuiSettingsOption(layout, click)) {
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

        if (toggleScrollableTooltipsOption(layout, click)) {
            return true;
        }

        if (toggleItemValueOverlayOption(layout, click)) {
            return true;
        }

        if (toggleCooldownDisplayOption(layout, click)) {
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
        if (closingStartedAtMillis >= 0L) {
            return true;
        }

        if (draggingItemViewSetting != null) {
            updateItemViewSlider(JafuLayout.fromScreen(width, height), draggingItemViewSetting, click.x());
            return true;
        }

        if (draggingTooltipSetting != null) {
            updateScrollableTooltipSlider(JafuLayout.fromScreen(width, height), draggingTooltipSetting, click.x());
            return true;
        }

        if (draggingItemValueSetting != null) {
            updateItemValueSlider(JafuLayout.fromScreen(width, height), draggingItemValueSetting, click.x());
            return true;
        }

        if (draggingPowderAutoResetSeconds) {
            updatePowderAutoResetSeconds(JafuLayout.fromScreen(width, height), click.x());
            return true;
        }

        if (draggingCooldownSeconds) {
            updateCooldownSeconds(JafuLayout.fromScreen(width, height), click.x());
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
        if (closingStartedAtMillis >= 0L) {
            return true;
        }

        if (draggingItemViewSetting != null) {
            draggingItemViewSetting = null;
            return true;
        }

        if (draggingTooltipSetting != null) {
            draggingTooltipSetting = null;
            return true;
        }

        if (draggingItemValueSetting != null) {
            draggingItemValueSetting = null;
            return true;
        }

        if (draggingPowderAutoResetSeconds) {
            draggingPowderAutoResetSeconds = false;
            return true;
        }

        if (draggingCooldownSeconds) {
            draggingCooldownSeconds = false;
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

    @Override
    public void close() {
        if (closingStartedAtMillis < 0L) {
            closingStartedAtMillis = System.currentTimeMillis();
        }
    }

    private boolean selectCategory(JafuLayout layout, Click click) {
        JafuCategory[] categories = JafuCategory.values();
        for (int i = 0; i < categories.length; i++) {
            if (layout.categoryButton(i).contains(click.x(), click.y())) {
                if (selectedCategory != categories[i]) {
                    categoryTransitionStartedAtMillis = System.currentTimeMillis();
                    detailTransitionStartedAtMillis = categoryTransitionStartedAtMillis;
                    globalFontDropdownOpen = false;
                }
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
                if (selectedModuleIndex != i) {
                    detailTransitionStartedAtMillis = System.currentTimeMillis();
                    globalFontDropdownOpen = false;
                }
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
                JafuModule module = modules.get(i);
                if (JafuModules.GLOBAL_SETTINGS.equals(module.id())
                        || JafuModules.GUI_SETTINGS.equals(module.id())) {
                    if (selectedModuleIndex != i) {
                        detailTransitionStartedAtMillis = System.currentTimeMillis();
                    }
                    selectedModuleIndex = i;
                    return true;
                }
                module.toggle();
                if (JafuModules.MOD_HIDER.equals(module.id())) {
                    ModHiderSettings.INSTANCE.setEnabled(module.enabled());
                } else if (JafuModules.TOKEN_GUARD.equals(module.id())) {
                    TokenGuardSettings.INSTANCE.setEnabled(module.enabled());
                }
                detailTransitionStartedAtMillis = System.currentTimeMillis();
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
            double activeProgress = animatedProgress("category:" + categories[i].name(), active);

            if (activeProgress > 0.01D) {
                GuiDraw.fill(context, button, multiplyAlpha(JafuTheme.ACCENT_SOFT, activeProgress));
                int railHeight = (int) Math.round(button.height() * activeProgress);
                GuiDraw.fill(context, new Rect(button.x(), button.y(), 3, railHeight), JafuTheme.ACCENT);
            }
            GuiDraw.text(
                    context,
                    textRenderer,
                    categories[i].label(),
                    button.x() + 12,
                    button.y() + 8,
                    lerpColor(JafuTheme.TEXT_MUTED, JafuTheme.TEXT, activeProgress)
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
        float transition = easeOut((float) normalized(System.currentTimeMillis() - categoryTransitionStartedAtMillis, PANEL_TRANSITION_MILLIS));
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((1.0F - transition) * -8.0F, 0.0F);

        Rect panel = layout.panel();
        List<JafuModule> modules = visibleModules();
        int listX = layout.moduleRow(0).x();
        GuiDraw.text(context, textRenderer, selectedCategory.title(), listX + 12, panel.y() + 58, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, "Modules", listX + 12, panel.y() + 74, JafuTheme.TEXT_MUTED);

        for (int i = 0; i < modules.size(); i++) {
            drawModuleRow(context, layout.moduleRow(i), modules.get(i), i, mouseX, mouseY);
        }

        GuiDraw.text(
                context,
                textRenderer,
                "Hypixel-safe defaults: informational UI only.",
                listX + 12,
                layout.contentBottom() - 22,
                JafuTheme.TEXT_MUTED
        );
        context.getMatrices().popMatrix();
    }

    private void drawCredits(DrawContext context, JafuLayout layout) {
        Rect panel = layout.panel();
        Rect creditsPanel = new Rect(panel.x() + 166, panel.y() + 72, panel.right() - panel.x() - 184, panel.height() - 116);
        long nowMillis = System.currentTimeMillis();

        GuiDraw.text(context, textRenderer, selectedCategory.title(), panel.x() + 166, panel.y() + 58, JafuTheme.TEXT);
        GuiDraw.fill(context, creditsPanel, JafuTheme.PANEL_LIGHT);
        GuiDraw.horizontalLine(context, creditsPanel.x(), creditsPanel.y(), creditsPanel.width(), JafuTheme.ACCENT);
        drawCreditsPulseBar(context, new Rect(creditsPanel.x() + 18, creditsPanel.y() + 20, creditsPanel.width() - 36, 3), nowMillis);

        int firstLineY = creditsPanel.y() + 42;
        int firstLineWidth = textRenderer.getWidth("Made with ") + textRenderer.getWidth("ChatGPT");
        int firstLineX = creditsPanel.x() + (creditsPanel.width() - firstLineWidth) / 2;
        GuiDraw.text(context, textRenderer, "Made with ", firstLineX, firstLineY, JafuTheme.TEXT_MUTED);
        drawAnimatedCreditsWord(
                context,
                "ChatGPT",
                firstLineX + textRenderer.getWidth("Made with "),
                firstLineY,
                nowMillis,
                JafuTheme.ACCENT,
                JafuTheme.GOOD
        );

        int secondLineY = firstLineY + 34;
        int secondLineWidth = textRenderer.getWidth("Mashed together by ") + textRenderer.getWidth("Chorey");
        int secondLineX = creditsPanel.x() + (creditsPanel.width() - secondLineWidth) / 2;
        GuiDraw.text(context, textRenderer, "Mashed together by ", secondLineX, secondLineY, JafuTheme.TEXT_MUTED);
        drawAnimatedCreditsWord(
                context,
                "Chorey",
                secondLineX + textRenderer.getWidth("Mashed together by "),
                secondLineY,
                nowMillis + 420L,
                JafuTheme.WARN,
                JafuTheme.ACCENT
        );

        int thirdLineY = secondLineY + 32;
        int thirdLineWidth = textRenderer.getWidth("All hail the ") + textRenderer.getWidth("LLM's");
        int thirdLineX = creditsPanel.x() + (creditsPanel.width() - thirdLineWidth) / 2;
        GuiDraw.text(context, textRenderer, "All hail the ", thirdLineX, thirdLineY, JafuTheme.TEXT_MUTED);
        drawAnimatedCreditsWord(
                context,
                "LLM's",
                thirdLineX + textRenderer.getWidth("All hail the "),
                thirdLineY,
                nowMillis + 840L,
                JafuTheme.GOOD,
                JafuTheme.WARN
        );

        int fourthLineY = thirdLineY + 30;
        String fourthLine = "Mod Integrity: Success=false | Hash=unavailable";
        GuiDraw.text(context, textRenderer, fourthLine, creditsPanel.x() + (creditsPanel.width() - textRenderer.getWidth(fourthLine)) / 2, fourthLineY, JafuTheme.ACCENT);

        drawCreditsPulseBar(context, new Rect(creditsPanel.x() + 48, fourthLineY + 24, creditsPanel.width() - 96, 2), nowMillis + 700L);
    }

    private void drawCreditsPulseBar(DrawContext context, Rect bar, long nowMillis) {
        GuiDraw.fill(context, bar, multiplyAlpha(JafuTheme.CONTROL, 0.78D));
        GuiDraw.fill(context, new Rect(bar.x(), bar.y(), bar.width(), 1), multiplyAlpha(JafuTheme.ACCENT_SOFT, 0.7D));

        int sweepWidth = Math.max(18, bar.width() / 4);
        double sweep = (nowMillis % 1500L) / 1500.0D;
        int sweepX = bar.x() - sweepWidth + (int) Math.round((bar.width() + sweepWidth) * sweep);
        int start = Math.max(bar.x(), sweepX);
        int end = Math.min(bar.right(), sweepX + sweepWidth);
        if (end > start) {
            double colorProgress = (Math.sin(nowMillis / 230.0D) + 1.0D) * 0.5D;
            GuiDraw.fill(context, new Rect(start, bar.y(), end - start, bar.height()), lerpColor(JafuTheme.ACCENT, JafuTheme.GOOD, colorProgress));
        }
    }

    private int drawAnimatedCreditsWord(
            DrawContext context,
            String word,
            int x,
            int y,
            long nowMillis,
            int fromColor,
            int toColor
    ) {
        int cursorX = x;
        for (int i = 0; i < word.length(); i++) {
            String character = word.substring(i, i + 1);
            double wave = (Math.sin(nowMillis / 160.0D + i * 0.72D) + 1.0D) * 0.5D;
            int color = lerpColor(fromColor, toColor, wave);
            int offsetY = (int) Math.round((wave - 0.5D) * 3.0D);
            GuiDraw.text(context, textRenderer, character, cursorX, y + offsetY, color);
            cursorX += textRenderer.getWidth(character);
        }
        return cursorX - x;
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
        double selectedProgress = animatedProgress("module:selected:" + module.id(), selected);
        double enabledProgress = animatedProgress("module:enabled:" + module.id(), module.enabled());
        int restingBackground = hovered ? JafuTheme.HOVERED_ROW : JafuTheme.PANEL_LIGHT;
        int background = lerpColor(restingBackground, JafuTheme.SELECTED_ROW, selectedProgress);

        Rect toggle = new Rect(rowBounds.right() - 38, rowBounds.y() + 7, 30, 20);
        int textX = rowBounds.x() + 28;
        int textWidth = Math.max(20, toggle.x() - textX - 8);

        GuiDraw.fill(context, rowBounds, background);
        GuiDraw.horizontalLine(context, rowBounds.x(), rowBounds.y(), rowBounds.width(), lerpColor(JafuTheme.BORDER_FAINT, JafuTheme.ACCENT, selectedProgress));
        GuiDraw.fill(context, new Rect(rowBounds.x() + 10, rowBounds.y() + 10, 8, 8), module.color());
        GuiDraw.text(context, textRenderer, trimToWidth(module.name(), textWidth), textX, rowBounds.y() + 7, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, trimToWidth(module.description(), textWidth), textX, rowBounds.y() + 20, JafuTheme.TEXT_MUTED);

        GuiDraw.fill(context, toggle, lerpColor(JafuTheme.CONTROL, 0xFF26445B, enabledProgress));
        int knobX = toggle.x() + 4 + (int) Math.round(enabledProgress * (toggle.width() - 12));
        GuiDraw.fill(context, new Rect(knobX, toggle.y() + 5, 6, 10), lerpColor(JafuTheme.TEXT_MUTED, JafuTheme.ACCENT, enabledProgress));
        GuiDraw.text(
                context,
                textRenderer,
                module.enabled() ? "ON" : "OFF",
                rowBounds.right() - 31,
                rowBounds.y() + 13,
                lerpColor(JafuTheme.TEXT_MUTED, JafuTheme.GOOD, enabledProgress)
        );
    }

    private void drawModuleDetails(DrawContext context, JafuLayout layout) {
        float transition = easeOut((float) normalized(System.currentTimeMillis() - detailTransitionStartedAtMillis, PANEL_TRANSITION_MILLIS));
        context.getMatrices().pushMatrix();
        context.getMatrices().translate((1.0F - transition) * 10.0F, 0.0F);

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
        } else if (JafuModules.GARDEN_RNG_CALCULATOR.equals(selectedModule.id())) {
            drawGardenRngOptions(context, detailPanel);
        } else if (JafuModules.GLOBAL_SETTINGS.equals(selectedModule.id())) {
            drawGlobalSettingsOptions(context, detailPanel);
        } else if (JafuModules.GUI_SETTINGS.equals(selectedModule.id())) {
            drawGuiSettingsOptions(context, detailPanel);
        } else if (JafuModules.ITEM_VIEW.equals(selectedModule.id())) {
            drawItemViewOptions(context, detailPanel);
        } else if (JafuModules.CHAT_ENHANCEMENTS.equals(selectedModule.id())) {
            drawChatEnhancementOptions(context, detailPanel);
        } else if (JafuModules.SCROLLABLE_TOOLTIPS.equals(selectedModule.id())) {
            drawScrollableTooltipsOptions(context, detailPanel);
        } else if (JafuModules.STORAGE_INDEXER.equals(selectedModule.id())) {
            drawStorageIndexerOptions(context, detailPanel);
        } else if (JafuModules.ITEM_VALUE_OVERLAY.equals(selectedModule.id())) {
            drawItemValueOverlayOptions(context, detailPanel);
        } else if (JafuModules.COOLDOWN_DISPLAY.equals(selectedModule.id())) {
            drawCooldownDisplayOptions(context, detailPanel);
        } else if (JafuModules.AUTO_UPDATER.equals(selectedModule.id())) {
            drawAutoUpdaterOptions(context, detailPanel);
        } else {
            drawPreview(context, detailPanel);
        }
        drawStatus(context, layout);
        context.getMatrices().popMatrix();
    }

    private void drawPowderTrackerOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Tracker fields", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);
        List<PowderChestStatOption> options = PowderChestStatOption.all();
        for (int i = 0; i < options.size(); i++) {
            PowderChestStatOption option = options.get(i);
            Rect row = powderTrackerOptionRow(detailPanel, i);
            Rect checkbox = new Rect(row.x(), row.y() + 3, 10, 10);
            boolean visible = PowderChestSettings.INSTANCE.isVisible(option);

            drawCheckbox(context, checkbox, visible, "powder:" + option.id());
            GuiDraw.text(context, textRenderer, option.label(), row.x() + 18, row.y() + 4, visible ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
        }

        Rect miningToolRow = powderMiningToolRow(detailPanel);
        boolean miningToolOnly = PowderChestSettings.INSTANCE.onlyShowWithMiningTool();
        drawCheckbox(context, new Rect(miningToolRow.x(), miningToolRow.y() + 3, 10, 10), miningToolOnly, "powder:mining_tool");
        GuiDraw.text(context, textRenderer, "Only show with drill/pickaxe", miningToolRow.x() + 18, miningToolRow.y() + 4, miningToolOnly ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);

        Rect autoResetRow = powderAutoResetRow(detailPanel);
        Rect autoResetCheckbox = new Rect(autoResetRow.x(), autoResetRow.y() + 3, 10, 10);
        boolean autoReset = PowderChestSettings.INSTANCE.autoResetEnabled();
        drawCheckbox(context, autoResetCheckbox, autoReset, "powder:auto_reset");
        GuiDraw.text(context, textRenderer, "Auto Reset", autoResetRow.x() + 18, autoResetRow.y() + 4, autoReset ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);

        Rect secondsRow = powderAutoResetSecondsRow(detailPanel);
        Rect secondsSlider = powderAutoResetSecondsSlider(detailPanel);
        double seconds = PowderChestSettings.INSTANCE.autoResetSeconds();
        double percent = (seconds - PowderChestSettings.MIN_AUTO_RESET_SECONDS)
                / (PowderChestSettings.MAX_AUTO_RESET_SECONDS - PowderChestSettings.MIN_AUTO_RESET_SECONDS);
        double animatedPercent = animatedSlider("powder:auto_reset_seconds", percent);
        int knobX = secondsSlider.x() + (int) Math.round(animatedPercent * secondsSlider.width());

        GuiDraw.text(context, textRenderer, "Reset after", secondsRow.x(), secondsRow.y() + 4, autoReset ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
        GuiDraw.text(context, textRenderer, formatPowderAutoResetSeconds(seconds), secondsRow.right() - 48, secondsRow.y() + 4, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, secondsSlider, JafuTheme.CONTROL);
        GuiDraw.fill(context, new Rect(secondsSlider.x(), secondsSlider.y(), knobX - secondsSlider.x(), secondsSlider.height()), autoReset ? JafuTheme.ACCENT_SOFT : JafuTheme.CONTROL);
        GuiDraw.fill(context, new Rect(knobX - 2, secondsSlider.y() - 2, 4, secondsSlider.height() + 4), autoReset ? JafuTheme.ACCENT : JafuTheme.TEXT_MUTED);

        Rect resetButton = powderTrackerResetButton(detailPanel);
        GuiDraw.text(context, textRenderer, "Session", detailPanel.x() + 16, resetButton.y() - 16, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, resetButton, JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, "Reset stats", resetButton.x() + 13, resetButton.y() + 6, JafuTheme.WARN);
    }

    private void drawGuiSettingsOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Menu appearance", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);

        Rect clickGuiRow = trackerOptionRow(detailPanel, 0);
        GuiDraw.text(context, textRenderer, "ClickGUI", clickGuiRow.x(), clickGuiRow.y() + 4, JafuTheme.TEXT);
        drawClickGuiVersionButton(context, detailPanel, ClickGuiVersion.V1, 0);
        drawClickGuiVersionButton(context, detailPanel, ClickGuiVersion.V2, 0);
    }

    private void drawGardenRngOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "HUD fields", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);
        List<GardenRngStatOption> stats = GardenRngStatOption.all();
        for (int i = 0; i < stats.size(); i++) {
            GardenRngStatOption option = stats.get(i);
            Rect row = trackerOptionRow(detailPanel, i);
            Rect checkbox = new Rect(row.x(), row.y() + 3, 10, 10);
            boolean visible = GardenRngSettings.INSTANCE.isVisible(option);
            drawCheckbox(context, checkbox, visible, "garden:stat:" + option.id());
            GuiDraw.text(context, textRenderer, option.label(), row.x() + 18, row.y() + 4, visible ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
        }

        int rowIndex = gardenFamiliesStart();
        GuiDraw.text(context, textRenderer, "Drop families", detailPanel.x() + 16, trackerOptionRow(detailPanel, rowIndex - 1).y() + 4, JafuTheme.TEXT_MUTED);
        GardenDropFamily[] families = GardenDropFamily.values();
        for (int i = 0; i < families.length; i++) {
            GardenDropFamily family = families[i];
            Rect row = trackerOptionRow(detailPanel, rowIndex + i);
            boolean enabled = GardenRngSettings.INSTANCE.familyEnabled(family);
            drawCheckbox(context, new Rect(row.x(), row.y() + 3, 10, 10), enabled, "garden:family:" + family.id());
            GuiDraw.text(context, textRenderer, family.label(), row.x() + 18, row.y() + 4, enabled ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
        }

        rowIndex = gardenTogglesStart();
        drawGardenToggleRow(context, detailPanel, rowIndex, "Feast tracking", GardenRngSettings.INSTANCE.feastTracking(), "garden:feast");
        drawGardenToggleRow(context, detailPanel, rowIndex + 1, "Assume active crop", GardenRngSettings.INSTANCE.assumeActiveCrop(), "garden:active_crop");
        drawGardenToggleRow(context, detailPanel, rowIndex + 2, "Overbloom +50%", GardenRngSettings.INSTANCE.overbloomEnabled(), "garden:overbloom");
        drawGardenToggleRow(context, detailPanel, rowIndex + 3, "Profit odds", GardenRngSettings.INSTANCE.showProfit(), "garden:profit");
        drawGardenToggleRow(context, detailPanel, rowIndex + 4, "Only show with Wheat Hoe", GardenRngSettings.INSTANCE.onlyShowWithWheatHoe(), "garden:wheat_hoe");
        drawGardenToggleRow(context, detailPanel, rowIndex + 5, "Auto reset idle", GardenRngSettings.INSTANCE.autoResetEnabled(), "garden:auto_reset");

        Rect resetSecondsRow = trackerOptionRow(detailPanel, rowIndex + 6);
        GuiDraw.text(context, textRenderer, "Idle reset", resetSecondsRow.x(), resetSecondsRow.y() + 4, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, formatPowderAutoResetSeconds(GardenRngSettings.INSTANCE.autoResetSeconds()), resetSecondsRow.right() - 48, resetSecondsRow.y() + 4, JafuTheme.ACCENT);

        Rect armorRow = trackerOptionRow(detailPanel, rowIndex + 7);
        GuiDraw.text(context, textRenderer, "Armor pieces", armorRow.x(), armorRow.y() + 4, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, GardenRngSettings.INSTANCE.armorPieces() + "/4", armorRow.right() - 28, armorRow.y() + 4, JafuTheme.ACCENT);

        Rect resetActive = gardenResetActiveButton(detailPanel);
        Rect resetAll = gardenResetAllButton(detailPanel);
        GuiDraw.fill(context, resetActive, JafuTheme.CONTROL);
        GuiDraw.fill(context, resetAll, JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, "Reset active", resetActive.x() + 9, resetActive.y() + 6, JafuTheme.WARN);
        GuiDraw.text(context, textRenderer, "Reset all", resetAll.x() + 16, resetAll.y() + 6, JafuTheme.WARN);

        List<GardenDropState> activeDrops = GardenRngFeature.snapshot().drops().stream().limit(3).toList();
        int rateStart = gardenRateRowsStart();
        GuiDraw.text(context, textRenderer, "Rate multipliers", detailPanel.x() + 16, trackerOptionRow(detailPanel, rateStart - 1).y() + 4, JafuTheme.TEXT_MUTED);
        if (activeDrops.isEmpty()) {
            Rect row = trackerOptionRow(detailPanel, rateStart);
            GuiDraw.text(context, textRenderer, "Harvest a crop to tune active drops.", row.x(), row.y() + 4, JafuTheme.TEXT_MUTED);
            return;
        }
        for (int i = 0; i < activeDrops.size(); i++) {
            GardenDropState drop = activeDrops.get(i);
            Rect row = trackerOptionRow(detailPanel, rateStart + i);
            GuiDraw.text(context, textRenderer, trimToWidth(drop.definition().displayName(), row.width() - 58), row.x(), row.y() + 4, drop.definition().color());
            GuiDraw.text(context, textRenderer, "x" + formatScale(GardenRngSettings.INSTANCE.rateMultiplier(drop.definition().id())), row.right() - 34, row.y() + 4, JafuTheme.ACCENT);
        }
    }

    private void drawGardenToggleRow(DrawContext context, Rect detailPanel, int index, String label, boolean enabled, String animationKey) {
        Rect row = trackerOptionRow(detailPanel, index);
        drawCheckbox(context, new Rect(row.x(), row.y() + 3, 10, 10), enabled, animationKey);
        GuiDraw.text(context, textRenderer, label, row.x() + 18, row.y() + 4, enabled ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
    }

    private void drawGlobalSettingsOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Shared text settings", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);

        Rect customFontRow = trackerOptionRow(detailPanel, 0);
        Rect customFontCheckbox = new Rect(customFontRow.x(), customFontRow.y() + 3, 10, 10);
        boolean customFontEnabled = GlobalSettings.INSTANCE.customFontEnabled();
        drawCheckbox(context, customFontCheckbox, customFontEnabled, "global:custom_font");
        GuiDraw.text(context, textRenderer, "Custom font", customFontRow.x() + 18, customFontRow.y() + 4, customFontEnabled ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);

        Rect fontRow = trackerOptionRow(detailPanel, 1);
        Rect fontControl = globalFontControl(detailPanel);
        GuiDraw.text(context, textRenderer, "Nice font", fontRow.x(), fontRow.y() + 4, JafuTheme.TEXT);
        GuiDraw.fill(context, fontControl, JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, GlobalSettings.INSTANCE.font().label(), fontControl.x() + 8, fontControl.y() + 4, JafuTheme.ACCENT);
        GuiDraw.text(context, textRenderer, "v", fontControl.right() - 10, fontControl.y() + 4, JafuTheme.TEXT_MUTED);

        Rect sizeRow = trackerOptionRow(detailPanel, 2);
        Rect slider = globalTextScaleSlider(detailPanel);
        double value = GlobalSettings.INSTANCE.textScale();
        double percent = (value - GlobalSettings.MIN_TEXT_SCALE) / (GlobalSettings.MAX_TEXT_SCALE - GlobalSettings.MIN_TEXT_SCALE);
        double animatedPercent = animatedSlider("global:text_scale", percent);
        int knobX = slider.x() + (int) Math.round(animatedPercent * slider.width());

        GuiDraw.text(context, textRenderer, "Text size", sizeRow.x(), sizeRow.y() + 4, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, formatScale(value), sizeRow.right() - 34, sizeRow.y() + 4, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, slider, JafuTheme.CONTROL);
        GuiDraw.fill(context, new Rect(slider.x(), slider.y(), knobX - slider.x(), slider.height()), JafuTheme.ACCENT_SOFT);
        GuiDraw.fill(context, new Rect(knobX - 2, slider.y() - 2, 4, slider.height() + 4), JafuTheme.ACCENT);

        Rect clickGuiRow = trackerOptionRow(detailPanel, 3);
        GuiDraw.text(context, textRenderer, "ClickGUI", clickGuiRow.x(), clickGuiRow.y() + 4, JafuTheme.TEXT);
        drawClickGuiVersionButton(context, detailPanel, ClickGuiVersion.V1);
        drawClickGuiVersionButton(context, detailPanel, ClickGuiVersion.V2);

        if (globalFontDropdownOpen || globalFontDropdownAnimation > 0.01D) {
            drawGlobalFontDropdown(context, detailPanel, globalFontDropdownAnimation);
        }
    }

    private void drawClickGuiVersionButton(DrawContext context, Rect detailPanel, ClickGuiVersion version) {
        drawClickGuiVersionButton(context, detailPanel, version, 3);
    }

    private void drawClickGuiVersionButton(DrawContext context, Rect detailPanel, ClickGuiVersion version, int rowIndex) {
        Rect button = clickGuiVersionButton(detailPanel, version, rowIndex);
        boolean selected = GlobalSettings.INSTANCE.clickGuiVersion() == version;
        GuiDraw.fill(context, button, selected ? JafuTheme.ACCENT_SOFT : JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, version.label(), button.x() + 10, button.y() + 4, selected ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
    }

    private void drawGlobalFontDropdown(DrawContext context, Rect detailPanel, double progress) {
        Rect control = globalFontControl(detailPanel);
        GlobalFontOption[] options = GlobalFontOption.values();
        float eased = easeOut((float) progress);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(0.0F, (1.0F - eased) * -5.0F);
        for (int i = 0; i < options.length; i++) {
            Rect row = globalFontDropdownRow(control, i);
            boolean selected = options[i] == GlobalSettings.INSTANCE.font();
            GuiDraw.fill(context, row, multiplyAlpha(selected ? JafuTheme.ACCENT_SOFT : JafuTheme.CONTROL, eased));
            GuiDraw.text(context, textRenderer, options[i].label(), row.x() + 8, row.y() + 4, multiplyAlpha(selected ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED, eased));
        }
        context.getMatrices().popMatrix();
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
            double animatedPercent = animatedSlider("item_view:" + setting.id(), percent);
            int knobX = slider.x() + (int) Math.round(animatedPercent * slider.width());

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

            drawCheckbox(context, checkbox, enabled, "chat:" + option.id());
            GuiDraw.text(context, textRenderer, option.label(), row.x() + 18, row.y() + 4, enabled ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
        }

        Rect sizeRow = chatScaleRow(detailPanel);
        Rect slider = chatScaleSlider(detailPanel);
        double value = ChatEnhancementsSettings.INSTANCE.configuredChatScale();
        double percent = (value - ChatEnhancementsSettings.MIN_CHAT_SCALE)
                / (ChatEnhancementsSettings.MAX_CHAT_SCALE - ChatEnhancementsSettings.MIN_CHAT_SCALE);
        double animatedPercent = animatedSlider("chat:scale", percent);
        int knobX = slider.x() + (int) Math.round(animatedPercent * slider.width());

        GuiDraw.text(context, textRenderer, "Chat size", sizeRow.x(), sizeRow.y() + 4, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, formatScale(value), sizeRow.right() - 34, sizeRow.y() + 4, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, slider, JafuTheme.CONTROL);
        GuiDraw.fill(context, new Rect(slider.x(), slider.y(), knobX - slider.x(), slider.height()), JafuTheme.ACCENT_SOFT);
        GuiDraw.fill(context, new Rect(knobX - 2, slider.y() - 2, 4, slider.height() + 4), JafuTheme.ACCENT);
    }

    private void drawScrollableTooltipsOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Tooltip behavior", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);

        Rect fadeRow = trackerOptionRow(detailPanel, 0);
        Rect checkbox = new Rect(fadeRow.x(), fadeRow.y() + 3, 10, 10);
        boolean fades = ScrollableTooltipsSettings.INSTANCE.fadeGradients();
        drawCheckbox(context, checkbox, fades, "tooltips:fades");
        GuiDraw.text(context, textRenderer, "Fade gradients", fadeRow.x() + 18, fadeRow.y() + 4, fades ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);

        List<ScrollableTooltipNumericSetting> settings = ScrollableTooltipNumericSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            ScrollableTooltipNumericSetting setting = settings.get(i);
            int rowIndex = i + 1;
            Rect row = trackerOptionRow(detailPanel, rowIndex);
            Rect slider = scrollableTooltipSlider(detailPanel, rowIndex);
            double value = ScrollableTooltipsSettings.INSTANCE.value(setting);
            double percent = (value - setting.min()) / (setting.max() - setting.min());
            double animatedPercent = animatedSlider("tooltips:" + setting.id(), percent);
            int knobX = slider.x() + (int) Math.round(animatedPercent * slider.width());

            GuiDraw.text(context, textRenderer, setting.label(), row.x(), row.y() + 4, JafuTheme.TEXT);
            GuiDraw.text(context, textRenderer, setting.format(value), row.right() - 42, row.y() + 4, JafuTheme.TEXT_MUTED);
            GuiDraw.fill(context, slider, JafuTheme.CONTROL);
            GuiDraw.fill(context, new Rect(slider.x(), slider.y(), knobX - slider.x(), slider.height()), JafuTheme.ACCENT_SOFT);
            GuiDraw.fill(context, new Rect(knobX - 2, slider.y() - 2, 4, slider.height() + 4), JafuTheme.ACCENT);
        }

        Rect colorRow = trackerOptionRow(detailPanel, settings.size() + 1);
        GuiDraw.text(context, textRenderer, "Bar color", colorRow.x(), colorRow.y() + 4, JafuTheme.TEXT);
        drawScrollableTooltipColorSwatches(context, detailPanel, colorRow);
    }

    private void drawStorageIndexerOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Indexed storage", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);
        GuiDraw.text(context, textRenderer, "Backpacks use exact item matching.", detailPanel.x() + 16, detailPanel.y() + 82, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, "Ender chest and sacks use fallbacks.", detailPanel.x() + 16, detailPanel.y() + 100, JafuTheme.TEXT_MUTED);

        Rect summary = new Rect(detailPanel.x() + 16, detailPanel.y() + 132, detailPanel.width() - 32, 44);
        GuiDraw.fill(context, summary, JafuTheme.PREVIEW);
        GuiDraw.text(context, textRenderer, "Cache keys: " + StorageIndexStore.INSTANCE.snapshotKeyCount(), summary.x() + 12, summary.y() + 10, JafuTheme.ACCENT);

        Optional<StorageSnapshot> newest = StorageIndexStore.INSTANCE.newestSnapshot();
        String newestText = newest
                .map(snapshot -> "Newest: " + snapshot.sourceName())
                .orElse("Newest: nothing indexed yet");
        GuiDraw.text(context, textRenderer, newestText, summary.x() + 12, summary.y() + 26, JafuTheme.TEXT_MUTED);
    }

    private void drawItemValueOverlayOptions(DrawContext context, Rect detailPanel) {
        PriceCacheStatus status = ItemValueOverlayFeature.status();
        GuiDraw.text(context, textRenderer, "Tooltip estimates", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);

        List<ItemValueOverlayOption> options = ItemValueOverlayOption.all();
        for (int i = 0; i < options.size(); i++) {
            ItemValueOverlayOption option = options.get(i);
            Rect row = trackerOptionRow(detailPanel, i);
            Rect checkbox = new Rect(row.x(), row.y() + 3, 10, 10);
            boolean visible = ItemValueOverlaySettings.INSTANCE.isVisible(option);

            drawCheckbox(context, checkbox, visible, "item-value:" + option.id());
            GuiDraw.text(context, textRenderer, option.label(), row.x() + 18, row.y() + 4, visible ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
        }

        List<ItemValueNumericSetting> settings = ItemValueNumericSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            ItemValueNumericSetting setting = settings.get(i);
            int rowIndex = options.size() + i;
            Rect row = trackerOptionRow(detailPanel, rowIndex);
            Rect slider = itemValueSlider(detailPanel, rowIndex);
            double value = ItemValueOverlaySettings.INSTANCE.value(setting);
            double percent = (value - setting.min()) / (setting.max() - setting.min());
            double animatedPercent = animatedSlider("item-value:" + setting.id(), percent);
            int knobX = slider.x() + (int) Math.round(animatedPercent * slider.width());

            GuiDraw.text(context, textRenderer, setting.label(), row.x(), row.y() + 4, JafuTheme.TEXT);
            GuiDraw.text(context, textRenderer, setting.format(value), row.right() - 42, row.y() + 4, JafuTheme.TEXT_MUTED);
            GuiDraw.fill(context, slider, JafuTheme.CONTROL);
            GuiDraw.fill(context, new Rect(slider.x(), slider.y(), Math.max(0, knobX - slider.x()), slider.height()), JafuTheme.ACCENT_SOFT);
            GuiDraw.fill(context, new Rect(knobX - 2, slider.y() - 2, 4, slider.height() + 4), JafuTheme.ACCENT);
        }

        int summaryY = trackerOptionRow(detailPanel, options.size() + settings.size() + 1).y();
        Rect summary = new Rect(detailPanel.x() + 16, summaryY, detailPanel.width() - 32, 60);
        GuiDraw.fill(context, summary, JafuTheme.PREVIEW);
        GuiDraw.text(context, textRenderer, "Bazaar products: " + status.bazaarProducts(), summary.x() + 12, summary.y() + 10, JafuTheme.ACCENT);
        GuiDraw.text(context, textRenderer, "AH names cached: " + status.auctionItems(), summary.x() + 12, summary.y() + 26, JafuTheme.WARN);
        GuiDraw.text(context, textRenderer, status.loading() ? "Refreshing prices..." : "Updated: " + status.ageLabel(), summary.x() + 12, summary.y() + 42, JafuTheme.TEXT_MUTED);
    }

    private void drawCooldownDisplayOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Manual cooldown HUD", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);

        List<CooldownDisplayOption> options = CooldownDisplayOption.all();
        for (int i = 0; i < options.size(); i++) {
            CooldownDisplayOption option = options.get(i);
            Rect row = trackerOptionRow(detailPanel, i);
            Rect checkbox = new Rect(row.x(), row.y() + 3, 10, 10);
            boolean enabled = CooldownDisplaySettings.INSTANCE.isEnabled(option);

            drawCheckbox(context, checkbox, enabled, "cooldown:" + option.id());
            GuiDraw.text(context, textRenderer, option.label(), row.x() + 18, row.y() + 4, enabled ? JafuTheme.TEXT : JafuTheme.TEXT_MUTED);
        }

        Rect sliderRow = trackerOptionRow(detailPanel, options.size());
        Rect slider = cooldownSecondsSlider(detailPanel);
        double value = CooldownDisplaySettings.INSTANCE.cooldownSeconds();
        double percent = (value - CooldownDisplaySettings.MIN_COOLDOWN_SECONDS)
                / (CooldownDisplaySettings.MAX_COOLDOWN_SECONDS - CooldownDisplaySettings.MIN_COOLDOWN_SECONDS);
        double animatedPercent = animatedSlider("cooldown:seconds", percent);
        int knobX = slider.x() + (int) Math.round(animatedPercent * slider.width());
        GuiDraw.text(context, textRenderer, "Cooldown length", sliderRow.x(), sliderRow.y() + 4, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, formatCooldownSeconds(value), sliderRow.right() - 44, sliderRow.y() + 4, JafuTheme.TEXT_MUTED);
        GuiDraw.fill(context, slider, JafuTheme.CONTROL);
        GuiDraw.fill(context, new Rect(slider.x(), slider.y(), Math.max(0, knobX - slider.x()), slider.height()), JafuTheme.ACCENT_SOFT);
        GuiDraw.fill(context, new Rect(knobX - 2, slider.y() - 2, 4, slider.height() + 4), JafuTheme.ACCENT);

        Rect colorRow = trackerOptionRow(detailPanel, options.size() + 1);
        GuiDraw.text(context, textRenderer, "Text color", colorRow.x(), colorRow.y() + 4, JafuTheme.TEXT);
        drawCooldownColorSwatches(context, detailPanel, colorRow);

        Rect preview = new Rect(detailPanel.x() + 16, trackerOptionRow(detailPanel, options.size() + 3).y(), detailPanel.width() - 32, 46);
        GuiDraw.fill(context, preview, JafuTheme.PREVIEW);
        GuiDraw.text(context, textRenderer, "Preview: " + (CooldownDisplaySettings.INSTANCE.isEnabled(CooldownDisplayOption.USE_PROGRESS_BAR) ? "progress bar" : "text timer"), preview.x() + 12, preview.y() + 10, CooldownDisplaySettings.INSTANCE.textColor());
        GuiDraw.text(context, textRenderer, "Move and resize this in Edit HUD.", preview.x() + 12, preview.y() + 28, JafuTheme.TEXT_MUTED);
    }

    private void drawSacksStashTrackerOptions(DrawContext context, Rect detailPanel) {
        GuiDraw.text(context, textRenderer, "Tracker fields", detailPanel.x() + 16, detailPanel.y() + 58, JafuTheme.TEXT_MUTED);
        List<SacksStashOption> options = SacksStashOption.all();
        for (int i = 0; i < options.size(); i++) {
            SacksStashOption option = options.get(i);
            Rect row = trackerOptionRow(detailPanel, i);
            Rect checkbox = new Rect(row.x(), row.y() + 3, 10, 10);
            boolean visible = SacksStashSettings.INSTANCE.isVisible(option);

            drawCheckbox(context, checkbox, visible, "sacks:" + option.id());
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
        Rect detailPanel = layout.detailPanel();
        Rect status = new Rect(detailPanel.x() + 16, detailPanel.bottom() - 32, detailPanel.width() - 32, 20);
        String text = JafuModules.STORAGE_INDEXER.equals(selectedModule().id())
                ? "Status: indexing opened storage"
                : JafuModules.ITEM_VALUE_OVERLAY.equals(selectedModule().id())
                ? "Status: showing tooltip values"
                : "Status: scaffolded";
        GuiDraw.fill(context, status, JafuTheme.CONTROL);
        GuiDraw.text(context, textRenderer, text, status.x() + 12, status.y() + 6, JafuTheme.GOOD);
    }

    private void drawHoverTooltip(DrawContext context, JafuLayout layout, int mouseX, int mouseY) {
        if (closingStartedAtMillis >= 0L || selectedCategory != JafuCategory.QOL) {
            return;
        }

        List<JafuModule> modules = visibleModules();
        for (int i = 0; i < modules.size(); i++) {
            JafuModule module = modules.get(i);
            if (JafuModules.MOD_HIDER.equals(module.id()) && layout.moduleRow(i).contains(mouseX, mouseY)) {
                context.drawTooltip(textRenderer, Text.literal(MOD_HIDER_TOOLTIP), mouseX, mouseY);
                return;
            }
            if (JafuModules.TOKEN_GUARD.equals(module.id()) && layout.moduleRow(i).contains(mouseX, mouseY)) {
                context.drawTooltip(textRenderer, Text.literal(TOKEN_GUARD_TOOLTIP), mouseX, mouseY);
                return;
            }
        }
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

        if (powderMiningToolRow(detailPanel).contains(click.x(), click.y())) {
            PowderChestSettings.INSTANCE.toggleOnlyShowWithMiningTool();
            return true;
        }

        if (powderAutoResetRow(detailPanel).contains(click.x(), click.y())) {
            PowderChestSettings.INSTANCE.toggleAutoReset();
            return true;
        }

        if (expanded(powderAutoResetSecondsSlider(detailPanel), 4).contains(click.x(), click.y())) {
            draggingPowderAutoResetSeconds = true;
            updatePowderAutoResetSeconds(layout, click.x());
            return true;
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

    private boolean toggleGardenRngOption(JafuLayout layout, Click click) {
        JafuModule selectedModule = selectedModule();
        if (!JafuModules.GARDEN_RNG_CALCULATOR.equals(selectedModule.id())) {
            return false;
        }

        Rect detailPanel = layout.detailPanel();
        List<GardenRngStatOption> stats = GardenRngStatOption.all();
        for (int i = 0; i < stats.size(); i++) {
            if (trackerOptionRow(detailPanel, i).contains(click.x(), click.y())) {
                GardenRngSettings.INSTANCE.toggleStat(stats.get(i));
                return true;
            }
        }

        GardenDropFamily[] families = GardenDropFamily.values();
        int familiesStart = gardenFamiliesStart();
        for (int i = 0; i < families.length; i++) {
            if (trackerOptionRow(detailPanel, familiesStart + i).contains(click.x(), click.y())) {
                GardenRngSettings.INSTANCE.toggleFamily(families[i]);
                return true;
            }
        }

        int togglesStart = gardenTogglesStart();
        if (trackerOptionRow(detailPanel, togglesStart).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleFeastTracking();
            return true;
        }
        if (trackerOptionRow(detailPanel, togglesStart + 1).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleAssumeActiveCrop();
            return true;
        }
        if (trackerOptionRow(detailPanel, togglesStart + 2).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleOverbloom();
            return true;
        }
        if (trackerOptionRow(detailPanel, togglesStart + 3).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleProfit();
            return true;
        }
        if (trackerOptionRow(detailPanel, togglesStart + 4).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleOnlyShowWithWheatHoe();
            return true;
        }
        if (trackerOptionRow(detailPanel, togglesStart + 5).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleAutoReset();
            return true;
        }
        if (trackerOptionRow(detailPanel, togglesStart + 6).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.cycleAutoResetSeconds();
            return true;
        }
        if (trackerOptionRow(detailPanel, togglesStart + 7).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.cycleArmorPieces();
            return true;
        }
        if (gardenResetActiveButton(detailPanel).contains(click.x(), click.y())) {
            GardenRngFeature.resetActiveDrops();
            return true;
        }
        if (gardenResetAllButton(detailPanel).contains(click.x(), click.y())) {
            GardenRngFeature.resetAll();
            return true;
        }

        List<GardenDropState> activeDrops = GardenRngFeature.snapshot().drops().stream().limit(3).toList();
        int rateStart = gardenRateRowsStart();
        for (int i = 0; i < activeDrops.size(); i++) {
            if (trackerOptionRow(detailPanel, rateStart + i).contains(click.x(), click.y())) {
                GardenRngSettings.INSTANCE.cycleRateMultiplier(activeDrops.get(i).definition().id());
                return true;
            }
        }
        return false;
    }

    private boolean toggleGuiSettingsOption(JafuLayout layout, Click click) {
        JafuModule selectedModule = selectedModule();
        if (!JafuModules.GUI_SETTINGS.equals(selectedModule.id())) {
            return false;
        }

        Rect detailPanel = layout.detailPanel();
        if (clickGuiVersionButton(detailPanel, ClickGuiVersion.V1, 0).contains(click.x(), click.y())) {
            JafuScreens.switchTo(ClickGuiVersion.V1);
            return true;
        }
        if (clickGuiVersionButton(detailPanel, ClickGuiVersion.V2, 0).contains(click.x(), click.y())) {
            JafuScreens.switchTo(ClickGuiVersion.V2);
            return true;
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
        if (trackerOptionRow(detailPanel, 0).contains(click.x(), click.y())) {
            GlobalSettings.INSTANCE.toggleCustomFont();
            return true;
        }

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
        if (trackerOptionRow(detailPanel, 2).contains(click.x(), click.y())) {
            draggingGlobalTextScale = true;
            updateGlobalTextScale(layout, click.x());
            return true;
        }
        if (clickGuiVersionButton(detailPanel, ClickGuiVersion.V1).contains(click.x(), click.y())) {
            JafuScreens.switchTo(ClickGuiVersion.V1);
            return true;
        }
        if (clickGuiVersionButton(detailPanel, ClickGuiVersion.V2).contains(click.x(), click.y())) {
            JafuScreens.switchTo(ClickGuiVersion.V2);
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

    private boolean toggleScrollableTooltipsOption(JafuLayout layout, Click click) {
        JafuModule selectedModule = selectedModule();
        if (!JafuModules.SCROLLABLE_TOOLTIPS.equals(selectedModule.id())) {
            return false;
        }

        Rect detailPanel = layout.detailPanel();
        if (trackerOptionRow(detailPanel, 0).contains(click.x(), click.y())) {
            ScrollableTooltipsSettings.INSTANCE.toggleFadeGradients();
            return true;
        }

        List<ScrollableTooltipNumericSetting> settings = ScrollableTooltipNumericSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            ScrollableTooltipNumericSetting setting = settings.get(i);
            int rowIndex = i + 1;
            if (expanded(scrollableTooltipSlider(detailPanel, rowIndex), 4).contains(click.x(), click.y())) {
                draggingTooltipSetting = setting;
                updateScrollableTooltipSlider(layout, setting, click.x());
                return true;
            }
        }

        for (ScrollableTooltipColorOption option : ScrollableTooltipColorOption.all()) {
            if (scrollableTooltipColorSwatch(detailPanel, option).contains(click.x(), click.y())) {
                ScrollableTooltipsSettings.INSTANCE.setScrollBarColor(option.color());
                return true;
            }
        }
        return false;
    }

    private boolean toggleItemValueOverlayOption(JafuLayout layout, Click click) {
        JafuModule selectedModule = selectedModule();
        if (!JafuModules.ITEM_VALUE_OVERLAY.equals(selectedModule.id())) {
            return false;
        }

        Rect detailPanel = layout.detailPanel();
        List<ItemValueOverlayOption> options = ItemValueOverlayOption.all();
        for (int i = 0; i < options.size(); i++) {
            if (trackerOptionRow(detailPanel, i).contains(click.x(), click.y())) {
                ItemValueOverlaySettings.INSTANCE.toggle(options.get(i));
                return true;
            }
        }

        List<ItemValueNumericSetting> settings = ItemValueNumericSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            ItemValueNumericSetting setting = settings.get(i);
            int rowIndex = options.size() + i;
            if (expanded(itemValueSlider(detailPanel, rowIndex), 4).contains(click.x(), click.y())) {
                draggingItemValueSetting = setting;
                updateItemValueSlider(layout, setting, click.x());
                return true;
            }
        }
        return false;
    }

    private boolean toggleCooldownDisplayOption(JafuLayout layout, Click click) {
        JafuModule selectedModule = selectedModule();
        if (!JafuModules.COOLDOWN_DISPLAY.equals(selectedModule.id())) {
            return false;
        }

        Rect detailPanel = layout.detailPanel();
        List<CooldownDisplayOption> options = CooldownDisplayOption.all();
        for (int i = 0; i < options.size(); i++) {
            if (trackerOptionRow(detailPanel, i).contains(click.x(), click.y())) {
                CooldownDisplaySettings.INSTANCE.toggle(options.get(i));
                return true;
            }
        }

        if (expanded(cooldownSecondsSlider(detailPanel), 4).contains(click.x(), click.y())) {
            draggingCooldownSeconds = true;
            updateCooldownSeconds(layout, click.x());
            return true;
        }

        for (CooldownDisplayColorOption option : CooldownDisplayColorOption.all()) {
            if (cooldownColorSwatch(detailPanel, option).contains(click.x(), click.y())) {
                CooldownDisplaySettings.INSTANCE.setTextColor(option.color());
                return true;
            }
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

    private void updateScrollableTooltipSlider(JafuLayout layout, ScrollableTooltipNumericSetting setting, double mouseX) {
        int rowIndex = ScrollableTooltipNumericSetting.all().indexOf(setting) + 1;
        Rect slider = scrollableTooltipSlider(layout.detailPanel(), rowIndex);
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = setting.min() + percent * (setting.max() - setting.min());
        ScrollableTooltipsSettings.INSTANCE.setValue(setting, value);
    }

    private void updateItemValueSlider(JafuLayout layout, ItemValueNumericSetting setting, double mouseX) {
        int rowIndex = ItemValueOverlayOption.all().size() + ItemValueNumericSetting.all().indexOf(setting);
        Rect slider = itemValueSlider(layout.detailPanel(), rowIndex);
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = setting.min() + percent * (setting.max() - setting.min());
        ItemValueOverlaySettings.INSTANCE.setValue(setting, value);
    }

    private void updatePowderAutoResetSeconds(JafuLayout layout, double mouseX) {
        Rect slider = powderAutoResetSecondsSlider(layout.detailPanel());
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = PowderChestSettings.MIN_AUTO_RESET_SECONDS
                + percent * (PowderChestSettings.MAX_AUTO_RESET_SECONDS - PowderChestSettings.MIN_AUTO_RESET_SECONDS);
        PowderChestSettings.INSTANCE.setAutoResetSeconds(value);
    }

    private void updateCooldownSeconds(JafuLayout layout, double mouseX) {
        Rect slider = cooldownSecondsSlider(layout.detailPanel());
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = CooldownDisplaySettings.MIN_COOLDOWN_SECONDS
                + percent * (CooldownDisplaySettings.MAX_COOLDOWN_SECONDS - CooldownDisplaySettings.MIN_COOLDOWN_SECONDS);
        CooldownDisplaySettings.INSTANCE.setCooldownSeconds(value);
    }

    private void updateAnimationClock(long nowMillis) {
        if (lastFrameMillis == 0L) {
            lastFrameMillis = nowMillis;
            animationBlend = 1.0D;
        } else {
            long elapsed = Math.max(0L, nowMillis - lastFrameMillis);
            animationBlend = MathHelper.clamp(elapsed / 95.0D, 0.0D, 1.0D);
            lastFrameMillis = nowMillis;
        }

        globalFontDropdownAnimation += ((globalFontDropdownOpen ? 1.0D : 0.0D) - globalFontDropdownAnimation) * animationBlend;
        if (!globalFontDropdownOpen && globalFontDropdownAnimation < 0.01D) {
            globalFontDropdownAnimation = 0.0D;
        }
    }

    private double screenProgress(long nowMillis) {
        if (closingStartedAtMillis >= 0L) {
            return 1.0D - normalized(nowMillis - closingStartedAtMillis, SCREEN_ANIMATION_MILLIS);
        }
        return normalized(nowMillis - openedAtMillis, SCREEN_ANIMATION_MILLIS);
    }

    private double animatedProgress(String key, boolean target) {
        double targetValue = target ? 1.0D : 0.0D;
        double current = animatedProgress.getOrDefault(key, targetValue);
        double next = current + (targetValue - current) * animationBlend;
        if (Math.abs(next - targetValue) < 0.004D) {
            next = targetValue;
        }
        animatedProgress.put(key, next);
        return next;
    }

    private double animatedSlider(String key, double target) {
        double clampedTarget = MathHelper.clamp(target, 0.0D, 1.0D);
        double current = animatedSliders.getOrDefault(key, clampedTarget);
        double next = current + (clampedTarget - current) * animationBlend;
        if (Math.abs(next - clampedTarget) < 0.004D) {
            next = clampedTarget;
        }
        animatedSliders.put(key, next);
        return next;
    }

    private void drawCheckbox(DrawContext context, Rect checkbox, boolean checked, String key) {
        double progress = animatedProgress("checkbox:" + key, checked);
        GuiDraw.fill(context, checkbox, lerpColor(JafuTheme.CONTROL, 0xFF26445B, progress));

        int markSize = (int) Math.round(6.0D * progress);
        if (markSize > 0) {
            int offset = (checkbox.width() - markSize) / 2;
            GuiDraw.fill(context, new Rect(checkbox.x() + offset, checkbox.y() + offset, markSize, markSize), JafuTheme.ACCENT);
        } else {
            GuiDraw.fill(context, new Rect(checkbox.x() + 2, checkbox.y() + 2, 6, 6), JafuTheme.BORDER);
        }
    }

    private static double normalized(long elapsedMillis, long durationMillis) {
        if (durationMillis <= 0L) {
            return 1.0D;
        }
        return MathHelper.clamp(elapsedMillis / (double) durationMillis, 0.0D, 1.0D);
    }

    private static float easeOut(float progress) {
        float clamped = MathHelper.clamp(progress, 0.0F, 1.0F);
        float inverse = 1.0F - clamped;
        return 1.0F - inverse * inverse * inverse;
    }

    private static int withAlpha(int rgb, double alpha) {
        int a = (int) Math.round(MathHelper.clamp(alpha, 0.0D, 1.0D) * 255.0D);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    private static int multiplyAlpha(int color, double alpha) {
        int baseAlpha = color >>> 24;
        int a = (int) Math.round(baseAlpha * MathHelper.clamp(alpha, 0.0D, 1.0D));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, double progress) {
        double t = MathHelper.clamp(progress, 0.0D, 1.0D);
        int a = lerpChannel(from >>> 24, to >>> 24, t);
        int r = lerpChannel((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerpChannel((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerpChannel(int from, int to, double progress) {
        return (int) Math.round(from + (to - from) * progress);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (maxWidth <= 0 || text.isEmpty()) {
            return "";
        }
        if (textRenderer.getWidth(text) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = textRenderer.getWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        int availableWidth = maxWidth - ellipsisWidth;
        String trimmed = text;
        while (!trimmed.isEmpty() && textRenderer.getWidth(trimmed) > availableWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ellipsis;
    }

    private static Rect powderTrackerOptionRow(Rect detailPanel, int index) {
        return trackerOptionRow(detailPanel, index);
    }

    private static Rect powderAutoResetRow(Rect detailPanel) {
        return trackerOptionRow(detailPanel, PowderChestStatOption.all().size() + 1);
    }

    private static Rect powderAutoResetSecondsRow(Rect detailPanel) {
        return trackerOptionRow(detailPanel, PowderChestStatOption.all().size() + 2);
    }

    private static Rect powderAutoResetSecondsSlider(Rect detailPanel) {
        Rect row = powderAutoResetSecondsRow(detailPanel);
        return new Rect(row.x() + 74, row.y() + 7, Math.max(30, row.width() - 130), 4);
    }

    private static Rect powderTrackerResetButton(Rect detailPanel) {
        Rect row = trackerOptionRow(detailPanel, PowderChestStatOption.all().size() + 4);
        return new Rect(row.x(), row.y(), 82, 18);
    }

    private static Rect powderMiningToolRow(Rect detailPanel) {
        return trackerOptionRow(detailPanel, PowderChestStatOption.all().size());
    }

    private static int gardenFamiliesStart() {
        return GardenRngStatOption.all().size() + 1;
    }

    private static int gardenTogglesStart() {
        return gardenFamiliesStart() + GardenDropFamily.values().length + 1;
    }

    private static int gardenRateRowsStart() {
        return gardenTogglesStart() + 11;
    }

    private static Rect gardenResetActiveButton(Rect detailPanel) {
        Rect row = trackerOptionRow(detailPanel, gardenTogglesStart() + 9);
        return new Rect(row.x(), row.y(), 92, 18);
    }

    private static Rect gardenResetAllButton(Rect detailPanel) {
        Rect row = trackerOptionRow(detailPanel, gardenTogglesStart() + 9);
        return new Rect(row.x() + 100, row.y(), 82, 18);
    }

    private static Rect trackerOptionRow(Rect detailPanel, int index) {
        return new Rect(detailPanel.x() + 16, detailPanel.y() + 78 + index * 20, detailPanel.width() - 32, 16);
    }

    private static Rect itemViewSlider(Rect detailPanel, int index) {
        Rect row = trackerOptionRow(detailPanel, index);
        return new Rect(row.x() + 48, row.y() + 7, Math.max(30, row.width() - 96), 4);
    }

    private static Rect globalFontControl(Rect detailPanel) {
        Rect row = trackerOptionRow(detailPanel, 1);
        return new Rect(row.right() - 82, row.y(), 82, 16);
    }

    private static Rect globalFontDropdownRow(Rect control, int index) {
        return new Rect(control.x(), control.bottom() + 2 + index * 16, control.width(), 16);
    }

    private static Rect globalTextScaleSlider(Rect detailPanel) {
        Rect row = trackerOptionRow(detailPanel, 2);
        return new Rect(row.x() + 62, row.y() + 7, Math.max(30, row.width() - 108), 4);
    }

    private static Rect clickGuiVersionButton(Rect detailPanel, ClickGuiVersion version) {
        return clickGuiVersionButton(detailPanel, version, 3);
    }

    private static Rect clickGuiVersionButton(Rect detailPanel, ClickGuiVersion version, int rowIndex) {
        Rect row = trackerOptionRow(detailPanel, rowIndex);
        int width = 34;
        int x = row.right() - width * 2 + version.ordinal() * width;
        return new Rect(x, row.y(), width, 16);
    }

    private static Rect chatScaleRow(Rect detailPanel) {
        return trackerOptionRow(detailPanel, ChatEnhancementOption.all().size() + 1);
    }

    private static Rect chatScaleSlider(Rect detailPanel) {
        Rect row = chatScaleRow(detailPanel);
        return new Rect(row.x() + 62, row.y() + 7, Math.max(30, row.width() - 108), 4);
    }

    private static Rect scrollableTooltipSlider(Rect detailPanel, int rowIndex) {
        Rect row = trackerOptionRow(detailPanel, rowIndex);
        return new Rect(row.x() + 78, row.y() + 7, Math.max(30, row.width() - 126), 4);
    }

    private static Rect itemValueSlider(Rect detailPanel, int rowIndex) {
        Rect row = trackerOptionRow(detailPanel, rowIndex);
        return new Rect(row.x() + 70, row.y() + 7, Math.max(30, row.width() - 118), 4);
    }

    private static Rect cooldownSecondsSlider(Rect detailPanel) {
        Rect row = trackerOptionRow(detailPanel, CooldownDisplayOption.all().size());
        return new Rect(row.x() + 96, row.y() + 7, Math.max(30, row.width() - 142), 4);
    }

    private void drawCooldownColorSwatches(DrawContext context, Rect detailPanel, Rect row) {
        for (CooldownDisplayColorOption option : CooldownDisplayColorOption.all()) {
            Rect swatch = cooldownColorSwatch(detailPanel, option);
            boolean selected = CooldownDisplaySettings.INSTANCE.textColor() == option.color();
            GuiDraw.fill(context, new Rect(swatch.x() - 1, swatch.y() - 1, swatch.width() + 2, swatch.height() + 2), selected ? JafuTheme.TEXT : JafuTheme.CONTROL);
            GuiDraw.fill(context, swatch, option.color());
        }
        CooldownDisplayColorOption current = CooldownDisplayColorOption.fromColor(CooldownDisplaySettings.INSTANCE.textColor());
        GuiDraw.text(context, textRenderer, current.label(), row.right() - 36, row.y() + 4, JafuTheme.TEXT_MUTED);
    }

    private static Rect cooldownColorSwatch(Rect detailPanel, CooldownDisplayColorOption option) {
        Rect row = trackerOptionRow(detailPanel, CooldownDisplayOption.all().size() + 1);
        int size = 10;
        int gap = 5;
        int count = CooldownDisplayColorOption.all().size();
        int startX = row.right() - 44 - count * size - (count - 1) * gap;
        int index = CooldownDisplayColorOption.all().indexOf(option);
        return new Rect(startX + index * (size + gap), row.y() + 3, size, size);
    }

    private void drawScrollableTooltipColorSwatches(DrawContext context, Rect detailPanel, Rect row) {
        for (ScrollableTooltipColorOption option : ScrollableTooltipColorOption.all()) {
            Rect swatch = scrollableTooltipColorSwatch(detailPanel, option);
            boolean selected = ScrollableTooltipsSettings.INSTANCE.scrollBarColor() == option.color();
            GuiDraw.fill(context, new Rect(swatch.x() - 1, swatch.y() - 1, swatch.width() + 2, swatch.height() + 2), selected ? JafuTheme.TEXT : JafuTheme.CONTROL);
            GuiDraw.fill(context, swatch, option.color());
        }
        ScrollableTooltipColorOption current = ScrollableTooltipColorOption.fromColor(ScrollableTooltipsSettings.INSTANCE.scrollBarColor());
        GuiDraw.text(context, textRenderer, current.label(), row.right() - 36, row.y() + 4, JafuTheme.TEXT_MUTED);
    }

    private static Rect scrollableTooltipColorSwatch(Rect detailPanel, ScrollableTooltipColorOption option) {
        Rect row = trackerOptionRow(detailPanel, ScrollableTooltipNumericSetting.all().size() + 1);
        int size = 10;
        int gap = 5;
        int count = ScrollableTooltipColorOption.all().size();
        int startX = row.right() - 44 - count * size - (count - 1) * gap;
        int index = ScrollableTooltipColorOption.all().indexOf(option);
        return new Rect(startX + index * (size + gap), row.y() + 3, size, size);
    }

    private static Rect expanded(Rect rect, int amount) {
        return new Rect(rect.x() - amount, rect.y() - amount, rect.width() + amount * 2, rect.height() + amount * 2);
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

    private static String formatPowderAutoResetSeconds(double value) {
        int seconds = (int) Math.round(value);
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        if (remainingSeconds == 0) {
            return minutes + "m";
        }
        return minutes + "m " + remainingSeconds + "s";
    }

    private static String formatCooldownSeconds(double value) {
        return String.format("%.1fs", value);
    }
}
