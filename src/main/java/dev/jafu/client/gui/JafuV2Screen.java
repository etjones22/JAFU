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

public final class JafuV2Screen extends Screen {
    private static final Text TITLE = Text.literal("JAFU V2");
    private static final String MOD_HIDER_TOOLTIP = "Blocks ModAnnouncer packets that mods like Firament send.";
    private static final String TOKEN_GUARD_TOOLTIP = "Best effort only: mods in the same JVM can still try reflection or lower-level tricks.";
    private static final int PANEL = 0xEA0D0816;
    private static final int PANEL_DEEP = 0xF0070410;
    private static final int CARD = 0xB7171220;
    private static final int CARD_DIM = 0x94110C1B;
    private static final int CARD_ACTIVE = 0xD620111A;
    private static final int CARD_BORDER = 0x4D37202A;
    private static final int WARM = 0xFFFF8D6E;
    private static final int WARM_SOFT = 0x44FF8D6E;
    private static final int WARM_DEEP = 0x803A2026;
    private static final int MUTED = 0xFF7E7587;
    private static final int RAIL_WIDTH = 42;
    private static final int CARD_GAP = 8;
    private static final int COLUMN_GAP = 10;
    private static final int COLLAPSED_CARD_HEIGHT = 38;
    private static final int OPTION_ROW_HEIGHT = 17;
    private static final int OPTION_ROW_GAP = 4;

    private JafuCategory selectedCategory = JafuCategory.GENERAL;
    private int selectedModuleIndex;
    private boolean globalFontDropdownOpen;
    private boolean draggingGlobalTextScale;
    private boolean draggingChatScale;
    private ItemViewSetting draggingItemViewSetting;
    private ScrollableTooltipNumericSetting draggingTooltipSetting;
    private ItemValueNumericSetting draggingItemValueSetting;
    private boolean draggingPowderAutoResetSeconds;
    private boolean draggingCooldownSeconds;
    private long lastFrameMillis;
    private double animationBlend = 1.0D;
    private double globalFontDropdownAnimation;
    private final Map<String, Double> animatedProgress = new HashMap<>();
    private final Map<String, Double> animatedSliders = new HashMap<>();
    private final Map<JafuCategory, Double> categoryScrollOffsets = new HashMap<>();

    public JafuV2Screen() {
        super(TITLE);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        updateAnimationClock(System.currentTimeMillis());

        Rect panel = panel();
        context.fill(0, 0, width, height, withAlpha(0x02040C, 0.66D));
        drawFrame(context, panel, mouseX, mouseY);
        drawRail(context, panel, mouseX, mouseY);

        if (selectedCategory == JafuCategory.CREDITS) {
            drawCredits(context, panel);
        } else {
            drawCards(context, panel, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, deltaTicks);
        drawHoverTooltip(context, panel, mouseX, mouseY);
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
        if (selectCategory(panel, click)) {
            return true;
        }
        if (selectedCategory == JafuCategory.CREDITS) {
            return super.mouseClicked(click, doubled);
        }
        if (!cardsViewport(panel).contains(click.x(), click.y())) {
            globalFontDropdownOpen = false;
            return super.mouseClicked(click, doubled);
        }
        if (handleSelectedModuleControls(panel, click)) {
            return true;
        }
        if (toggleModule(panel, click)) {
            return true;
        }
        if (selectModule(panel, click)) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        Rect card = selectedCard(panel());
        if (draggingGlobalTextScale) {
            updateGlobalTextScale(card, click.x());
            return true;
        }
        if (draggingChatScale) {
            updateChatScale(card, click.x());
            return true;
        }
        if (draggingItemViewSetting != null) {
            updateItemViewSlider(card, draggingItemViewSetting, click.x());
            return true;
        }
        if (draggingTooltipSetting != null) {
            updateScrollableTooltipSlider(card, draggingTooltipSetting, click.x());
            return true;
        }
        if (draggingItemValueSetting != null) {
            updateItemValueSlider(card, draggingItemValueSetting, click.x());
            return true;
        }
        if (draggingPowderAutoResetSeconds) {
            updatePowderAutoResetSeconds(card, click.x());
            return true;
        }
        if (draggingCooldownSeconds) {
            updateCooldownSeconds(card, click.x());
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Rect panel = panel();
        if (selectedCategory == JafuCategory.CREDITS || !cardsViewport(panel).contains(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        double current = scrollOffset();
        setScrollOffset(panel, current - verticalAmount * 22.0D);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean wasDragging = draggingGlobalTextScale || draggingChatScale || draggingItemViewSetting != null || draggingTooltipSetting != null || draggingItemValueSetting != null || draggingPowderAutoResetSeconds || draggingCooldownSeconds;
        draggingGlobalTextScale = false;
        draggingChatScale = false;
        draggingItemViewSetting = null;
        draggingTooltipSetting = null;
        draggingItemValueSetting = null;
        draggingPowderAutoResetSeconds = false;
        draggingCooldownSeconds = false;
        if (wasDragging) {
            return true;
        }
        return super.mouseReleased(click);
    }

    private void drawFrame(DrawContext context, Rect panel, int mouseX, int mouseY) {
        GuiDraw.fill(context, new Rect(panel.x() - 4, panel.y() - 4, panel.width() + 8, panel.height() + 8), 0x66000000);
        GuiDraw.fill(context, new Rect(panel.x() - 2, panel.y() - 2, panel.width() + 4, panel.height() + 4), 0xAA000000);
        GuiDraw.fill(context, panel, PANEL);
        GuiDraw.fill(context, rail(panel), PANEL_DEEP);
        GuiDraw.fill(context, new Rect(rail(panel).right(), panel.y(), 1, panel.height()), 0x22000000);

        Rect close = closeButton(panel);
        if (close.contains(mouseX, mouseY)) {
            GuiDraw.fill(context, close, 0xAA5C2730);
            GuiDraw.text(context, textRenderer, "x", close.x() + 5, close.y() + 3, JafuTheme.CLOSE_TEXT_HOVER);
        }
    }

    private void drawRail(DrawContext context, Rect panel, int mouseX, int mouseY) {
        Rect rail = rail(panel);
        drawJafuMark(context, rail.x() + 20, rail.y() + 24, WARM);

        JafuCategory[] categories = JafuCategory.values();
        for (int i = 0; i < categories.length; i++) {
            JafuCategory category = categories[i];
            Rect button = categoryButton(panel, i);
            boolean selected = selectedCategory == category;
            boolean hovered = button.contains(mouseX, mouseY);
            double progress = animatedProgress("v2:category:" + category.name(), selected);

            if (selected || hovered) {
                GuiDraw.fill(context, button, multiplyAlpha(WARM_DEEP, selected ? 1.0D : 0.55D));
            }
            if (progress > 0.01D) {
                GuiDraw.fill(context, new Rect(button.x() + 2, button.y() + 2, 2, button.height() - 4), multiplyAlpha(WARM, progress));
            }
            drawCategoryIcon(context, category, button.x() + button.width() / 2, button.y() + button.height() / 2, selected ? WARM : MUTED);
        }

        Rect badge = new Rect(rail.x() + 10, rail.bottom() - 30, 22, 22);
        GuiDraw.fill(context, badge, CARD_BORDER);
        GuiDraw.fill(context, new Rect(badge.x() + 3, badge.y() + 3, 16, 16), WARM_SOFT);
        GuiDraw.text(context, textRenderer, "V2", badge.x() + 5, badge.y() + 7, WARM);
    }

    private static void drawJafuMark(DrawContext context, int centerX, int centerY, int color) {
        GuiDraw.fill(context, new Rect(centerX - 1, centerY - 7, 2, 10), color);
        GuiDraw.fill(context, new Rect(centerX - 5, centerY + 2, 6, 2), color);
        GuiDraw.fill(context, new Rect(centerX - 5, centerY - 1, 2, 5), color);
        GuiDraw.fill(context, new Rect(centerX - 1, centerY - 7, 5, 2), multiplyAlpha(color, 0.65D));
    }

    private static void drawCategoryIcon(DrawContext context, JafuCategory category, int centerX, int centerY, int color) {
        switch (category) {
            case GENERAL -> {
                GuiDraw.fill(context, new Rect(centerX - 5, centerY - 5, 10, 2), color);
                GuiDraw.fill(context, new Rect(centerX - 5, centerY - 1, 8, 2), color);
                GuiDraw.fill(context, new Rect(centerX - 5, centerY + 3, 6, 2), color);
            }
            case QOL -> {
                GuiDraw.fill(context, new Rect(centerX - 1, centerY - 7, 2, 12), color);
                GuiDraw.fill(context, new Rect(centerX - 5, centerY - 1, 10, 2), color);
                GuiDraw.fill(context, new Rect(centerX - 3, centerY + 4, 6, 2), color);
                GuiDraw.fill(context, new Rect(centerX + 3, centerY - 4, 2, 4), multiplyAlpha(color, 0.7D));
            }
            case GUI -> {
                GuiDraw.fill(context, new Rect(centerX - 7, centerY - 6, 14, 3), color);
                GuiDraw.fill(context, new Rect(centerX - 7, centerY - 1, 10, 3), color);
                GuiDraw.fill(context, new Rect(centerX - 7, centerY + 4, 6, 3), color);
                GuiDraw.fill(context, new Rect(centerX + 5, centerY + 4, 2, 3), multiplyAlpha(color, 0.72D));
            }
            case SKYBLOCK -> {
                GuiDraw.fill(context, new Rect(centerX - 6, centerY - 4, 3, 2), color);
                GuiDraw.fill(context, new Rect(centerX - 3, centerY - 1, 3, 2), color);
                GuiDraw.fill(context, new Rect(centerX - 6, centerY + 2, 3, 2), color);
                GuiDraw.fill(context, new Rect(centerX + 1, centerY - 4, 3, 2), multiplyAlpha(color, 0.7D));
                GuiDraw.fill(context, new Rect(centerX + 4, centerY - 1, 3, 2), multiplyAlpha(color, 0.7D));
                GuiDraw.fill(context, new Rect(centerX + 1, centerY + 2, 3, 2), multiplyAlpha(color, 0.7D));
            }
            case MINING -> {
                GuiDraw.fill(context, new Rect(centerX - 2, centerY - 6, 4, 4), color);
                GuiDraw.fill(context, new Rect(centerX - 5, centerY, 4, 4), multiplyAlpha(color, 0.75D));
                GuiDraw.fill(context, new Rect(centerX + 1, centerY + 3, 4, 4), multiplyAlpha(color, 0.55D));
            }
            case DUNGEONS -> {
                GuiDraw.fill(context, new Rect(centerX - 6, centerY - 1, 12, 2), color);
                GuiDraw.fill(context, new Rect(centerX - 3, centerY - 4, 6, 8), multiplyAlpha(color, 0.55D));
                GuiDraw.fill(context, new Rect(centerX - 1, centerY - 1, 2, 2), color);
            }
            case GARDEN -> {
                GuiDraw.fill(context, new Rect(centerX - 1, centerY - 1, 2, 2), color);
                GuiDraw.fill(context, new Rect(centerX - 1, centerY - 7, 2, 4), multiplyAlpha(color, 0.7D));
                GuiDraw.fill(context, new Rect(centerX - 1, centerY + 3, 2, 4), multiplyAlpha(color, 0.7D));
                GuiDraw.fill(context, new Rect(centerX - 7, centerY - 1, 4, 2), multiplyAlpha(color, 0.7D));
                GuiDraw.fill(context, new Rect(centerX + 3, centerY - 1, 4, 2), multiplyAlpha(color, 0.7D));
            }
            case CREDITS -> {
                GuiDraw.fill(context, new Rect(centerX - 4, centerY - 5, 8, 2), color);
                GuiDraw.fill(context, new Rect(centerX - 5, centerY - 3, 2, 6), color);
                GuiDraw.fill(context, new Rect(centerX + 3, centerY - 3, 2, 6), color);
                GuiDraw.fill(context, new Rect(centerX - 4, centerY + 3, 8, 2), color);
            }
        }
    }

    private void drawCards(DrawContext context, Rect panel, int mouseX, int mouseY) {
        Rect area = cardsArea(panel);
        Rect viewport = cardsViewport(panel);
        List<JafuModule> modules = visibleModules();
        setScrollOffset(panel, scrollOffset());
        GuiDraw.text(context, textRenderer, selectedCategory.title(), area.x(), area.y(), WARM);
        GuiDraw.text(context, textRenderer, modules.size() + " modules", area.right() - 58, area.y(), MUTED);

        context.enableScissor(viewport.left(), viewport.top(), viewport.right(), viewport.bottom());
        try {
            for (int i = 0; i < modules.size(); i++) {
                Rect card = moduleCard(panel, i);
                if (intersects(card, viewport)) {
                    drawModuleCard(context, panel, card, modules.get(i), i, mouseX, mouseY);
                }
            }
        } finally {
            context.disableScissor();
        }

        drawScrollbar(context, panel);
    }

    private void drawScrollbar(DrawContext context, Rect panel) {
        Rect viewport = cardsViewport(panel);
        double maxScroll = maxScroll(panel);
        if (maxScroll <= 0.5D) {
            return;
        }
        int trackHeight = viewport.height();
        int contentHeight = trackHeight + (int) Math.ceil(maxScroll);
        int thumbHeight = Math.max(18, trackHeight * trackHeight / Math.max(trackHeight, contentHeight));
        int travel = Math.max(1, trackHeight - thumbHeight);
        int thumbY = viewport.y() + (int) Math.round((scrollOffset() / maxScroll) * travel);
        GuiDraw.fill(context, new Rect(viewport.right() - 3, viewport.y(), 1, viewport.height()), 0x33251A27);
        GuiDraw.fill(context, new Rect(viewport.right() - 4, thumbY, 3, thumbHeight), WARM_DEEP);
    }

    private void drawModuleCard(DrawContext context, Rect panel, Rect card, JafuModule module, int index, int mouseX, int mouseY) {
        boolean selected = selectedModuleIndex == index;
        boolean hovered = card.contains(mouseX, mouseY);
        boolean enabled = module.enabled();
        double enabledProgress = animatedProgress("v2:enabled:" + module.id(), enabled);
        Rect toggle = moduleToggle(panel, index);
        int titleX = card.x() + 14;
        int titleMax = Math.max(32, toggle.x() - titleX - 8);

        int border = selected ? WARM : hovered ? WARM_DEEP : CARD_BORDER;
        GuiDraw.fill(context, card, border);
        GuiDraw.fill(context, inset(card, 1), selected ? CARD_ACTIVE : CARD_DIM);
        GuiDraw.fill(context, new Rect(card.x() + 1, card.y() + 1, 2, card.height() - 2), selected ? WARM : multiplyAlpha(WARM, enabled ? 0.34D : 0.16D));

        GuiDraw.text(context, textRenderer, trimToWidth(module.name(), titleMax), titleX, card.y() + 11, enabled ? JafuTheme.TEXT : MUTED);
        drawToggle(context, toggle, enabled, enabledProgress);

        if (selected) {
            GuiDraw.text(context, textRenderer, trimToWidth(module.description(), card.width() - 34), card.x() + 14, card.y() + 29, MUTED);
            drawSelectedModuleControls(context, card, module);
        }
    }

    private void drawSelectedModuleControls(DrawContext context, Rect card, JafuModule module) {
        if (JafuModules.POWDER_CHEST_TRACKER.equals(module.id())) {
            drawPowderTrackerOptions(context, card);
        } else if (JafuModules.SACKS_STASH_TRACKER.equals(module.id())) {
            drawSacksStashTrackerOptions(context, card);
        } else if (JafuModules.GARDEN_RNG_CALCULATOR.equals(module.id())) {
            drawGardenRngOptions(context, card);
        } else if (JafuModules.GLOBAL_SETTINGS.equals(module.id())) {
            drawGlobalSettingsOptions(context, card);
        } else if (JafuModules.GUI_SETTINGS.equals(module.id())) {
            drawGuiSettingsOptions(context, card);
        } else if (JafuModules.ITEM_VIEW.equals(module.id())) {
            drawItemViewOptions(context, card);
        } else if (JafuModules.CHAT_ENHANCEMENTS.equals(module.id())) {
            drawChatEnhancementOptions(context, card);
        } else if (JafuModules.SCROLLABLE_TOOLTIPS.equals(module.id())) {
            drawScrollableTooltipsOptions(context, card);
        } else if (JafuModules.STORAGE_INDEXER.equals(module.id())) {
            drawStorageIndexerOptions(context, card);
        } else if (JafuModules.ITEM_VALUE_OVERLAY.equals(module.id())) {
            drawItemValueOverlayOptions(context, card);
        } else if (JafuModules.COOLDOWN_DISPLAY.equals(module.id())) {
            drawCooldownDisplayOptions(context, card);
        } else if (JafuModules.AUTO_UPDATER.equals(module.id())) {
            drawAutoUpdaterOptions(context, card);
        } else {
            drawPreview(context, card);
        }
    }

    private void drawPowderTrackerOptions(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "Tracker fields", card.x() + 14, card.y() + 48, MUTED);
        List<PowderChestStatOption> options = PowderChestStatOption.all();
        for (int i = 0; i < options.size(); i++) {
            PowderChestStatOption option = options.get(i);
            drawCheckboxRow(context, optionRow(card, i), option.label(), PowderChestSettings.INSTANCE.isVisible(option), "powder:" + option.id());
        }

        drawCheckboxRow(context, powderMiningToolRow(card), "Only show with drill/pickaxe", PowderChestSettings.INSTANCE.onlyShowWithMiningTool(), "powder:mining_tool");
        drawCheckboxRow(context, powderAutoResetRow(card), "Auto Reset", PowderChestSettings.INSTANCE.autoResetEnabled(), "powder:auto_reset");

        Rect secondsRow = powderAutoResetSecondsRow(card);
        double seconds = PowderChestSettings.INSTANCE.autoResetSeconds();
        double percent = (seconds - PowderChestSettings.MIN_AUTO_RESET_SECONDS)
                / (PowderChestSettings.MAX_AUTO_RESET_SECONDS - PowderChestSettings.MIN_AUTO_RESET_SECONDS);
        GuiDraw.text(context, textRenderer, "Reset after", secondsRow.x(), secondsRow.y() + 5, PowderChestSettings.INSTANCE.autoResetEnabled() ? JafuTheme.TEXT : MUTED);
        GuiDraw.text(context, textRenderer, formatPowderAutoResetSeconds(seconds), secondsRow.right() - 46, secondsRow.y() + 5, WARM);
        drawSlider(context, powderAutoResetSecondsSlider(card), animatedSlider("v2:powder:auto_reset_seconds", percent));

        Rect reset = powderResetButton(card);
        GuiDraw.fill(context, reset, CARD_BORDER);
        GuiDraw.text(context, textRenderer, "Reset stats", reset.x() + 9, reset.y() + 5, WARM);
    }

    private void drawSacksStashTrackerOptions(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "Tracker fields", card.x() + 14, card.y() + 48, MUTED);
        List<SacksStashOption> options = SacksStashOption.all();
        for (int i = 0; i < options.size(); i++) {
            SacksStashOption option = options.get(i);
            drawCheckboxRow(context, optionRow(card, i), option.label(), SacksStashSettings.INSTANCE.isVisible(option), "sacks:" + option.id());
        }
    }

    private void drawGardenRngOptions(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "HUD fields", card.x() + 14, card.y() + 48, MUTED);
        List<GardenRngStatOption> stats = GardenRngStatOption.all();
        for (int i = 0; i < stats.size(); i++) {
            GardenRngStatOption option = stats.get(i);
            drawCheckboxRow(context, optionRow(card, i), option.label(), GardenRngSettings.INSTANCE.isVisible(option), "garden:stat:" + option.id());
        }

        int familiesStart = gardenFamiliesStart();
        GuiDraw.text(context, textRenderer, "Drop families", card.x() + 14, optionRow(card, familiesStart - 1).y() + 5, MUTED);
        GardenDropFamily[] families = GardenDropFamily.values();
        for (int i = 0; i < families.length; i++) {
            GardenDropFamily family = families[i];
            drawCheckboxRow(context, optionRow(card, familiesStart + i), family.label(), GardenRngSettings.INSTANCE.familyEnabled(family), "garden:family:" + family.id());
        }

        int togglesStart = gardenTogglesStart();
        drawCheckboxRow(context, optionRow(card, togglesStart), "Feast tracking", GardenRngSettings.INSTANCE.feastTracking(), "garden:feast");
        drawCheckboxRow(context, optionRow(card, togglesStart + 1), "Assume active crop", GardenRngSettings.INSTANCE.assumeActiveCrop(), "garden:active_crop");
        drawCheckboxRow(context, optionRow(card, togglesStart + 2), "Overbloom +50%", GardenRngSettings.INSTANCE.overbloomEnabled(), "garden:overbloom");
        drawCheckboxRow(context, optionRow(card, togglesStart + 3), "Profit odds", GardenRngSettings.INSTANCE.showProfit(), "garden:profit");
        drawCheckboxRow(context, optionRow(card, togglesStart + 4), "Only show with Wheat Hoe", GardenRngSettings.INSTANCE.onlyShowWithWheatHoe(), "garden:wheat_hoe");
        drawCheckboxRow(context, optionRow(card, togglesStart + 5), "Auto reset idle", GardenRngSettings.INSTANCE.autoResetEnabled(), "garden:auto_reset");

        Rect resetSecondsRow = optionRow(card, togglesStart + 6);
        GuiDraw.text(context, textRenderer, "Idle reset", resetSecondsRow.x(), resetSecondsRow.y() + 5, GardenRngSettings.INSTANCE.autoResetEnabled() ? JafuTheme.TEXT : MUTED);
        GuiDraw.text(context, textRenderer, formatPowderAutoResetSeconds(GardenRngSettings.INSTANCE.autoResetSeconds()), resetSecondsRow.right() - 46, resetSecondsRow.y() + 5, WARM);

        Rect armorRow = optionRow(card, togglesStart + 7);
        GuiDraw.text(context, textRenderer, "Armor pieces", armorRow.x(), armorRow.y() + 5, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, GardenRngSettings.INSTANCE.armorPieces() + "/4", armorRow.right() - 26, armorRow.y() + 5, WARM);

        Rect resetActive = gardenResetActiveButton(card);
        Rect resetAll = gardenResetAllButton(card);
        GuiDraw.fill(context, resetActive, CARD_BORDER);
        GuiDraw.fill(context, resetAll, CARD_BORDER);
        GuiDraw.text(context, textRenderer, "Reset active", resetActive.x() + 8, resetActive.y() + 5, WARM);
        GuiDraw.text(context, textRenderer, "Reset all", resetAll.x() + 15, resetAll.y() + 5, WARM);

        int rateStart = gardenRateRowsStart();
        GuiDraw.text(context, textRenderer, "Rate multipliers", card.x() + 14, optionRow(card, rateStart - 1).y() + 5, MUTED);
        List<GardenDropState> activeDrops = GardenRngFeature.snapshot().drops().stream().limit(3).toList();
        if (activeDrops.isEmpty()) {
            Rect row = optionRow(card, rateStart);
            GuiDraw.text(context, textRenderer, "Harvest a crop to tune active drops.", row.x(), row.y() + 5, MUTED);
            return;
        }
        for (int i = 0; i < activeDrops.size(); i++) {
            GardenDropState drop = activeDrops.get(i);
            Rect row = optionRow(card, rateStart + i);
            GuiDraw.text(context, textRenderer, trimToWidth(drop.definition().displayName(), row.width() - 46), row.x(), row.y() + 5, drop.definition().color());
            GuiDraw.text(context, textRenderer, "x" + formatScale(GardenRngSettings.INSTANCE.rateMultiplier(drop.definition().id())), row.right() - 34, row.y() + 5, WARM);
        }
    }

    private void drawGlobalSettingsOptions(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "Shared settings", card.x() + 14, card.y() + 48, MUTED);

        drawCheckboxRow(context, optionRow(card, 0), "Custom font", GlobalSettings.INSTANCE.customFontEnabled(), "global:custom_font");

        Rect fontRow = optionRow(card, 1);
        Rect fontControl = globalFontControl(card);
        GuiDraw.text(context, textRenderer, "Font", fontRow.x(), fontRow.y() + 5, JafuTheme.TEXT);
        GuiDraw.fill(context, fontControl, CARD_BORDER);
        GuiDraw.text(context, textRenderer, GlobalSettings.INSTANCE.font().label(), fontControl.x() + 8, fontControl.y() + 5, WARM);
        GuiDraw.text(context, textRenderer, "^", fontControl.right() - 11, fontControl.y() + 5, MUTED);

        Rect sizeRow = globalTextScaleRow(card);
        double value = GlobalSettings.INSTANCE.textScale();
        double percent = (value - GlobalSettings.MIN_TEXT_SCALE) / (GlobalSettings.MAX_TEXT_SCALE - GlobalSettings.MIN_TEXT_SCALE);
        GuiDraw.text(context, textRenderer, "Text size", sizeRow.x(), sizeRow.y() + 5, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, formatScale(value), sizeRow.right() - 34, sizeRow.y() + 5, WARM);
        drawSlider(context, globalTextScaleSlider(card), animatedSlider("v2:global:text_scale", percent));

        if (globalFontDropdownOpen || globalFontDropdownAnimation > 0.01D) {
            drawGlobalFontDropdown(context, card, globalFontDropdownAnimation);
        }
    }

    private void drawGuiSettingsOptions(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "Menu appearance", card.x() + 14, card.y() + 48, MUTED);

        Rect versionRow = optionRow(card, 0);
        GuiDraw.text(context, textRenderer, "ClickGUI", versionRow.x(), versionRow.y() + 5, JafuTheme.TEXT);
        drawVersionButton(context, card, ClickGuiVersion.V1);
        drawVersionButton(context, card, ClickGuiVersion.V2);
    }

    private void drawItemViewOptions(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "Held item model", card.x() + 14, card.y() + 48, MUTED);
        List<ItemViewSetting> settings = ItemViewSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            ItemViewSetting setting = settings.get(i);
            Rect row = optionRow(card, i);
            double value = ItemViewSettings.INSTANCE.value(setting);
            double percent = (value - setting.min()) / (setting.max() - setting.min());

            GuiDraw.text(context, textRenderer, setting.label(), row.x(), row.y() + 5, JafuTheme.TEXT);
            GuiDraw.text(context, textRenderer, formatSliderValue(setting, value), row.right() - 42, row.y() + 5, WARM);
            drawSlider(context, itemViewSlider(card, i), animatedSlider("v2:item_view:" + setting.id(), percent));
        }
    }

    private void drawChatEnhancementOptions(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "Chat options", card.x() + 14, card.y() + 48, MUTED);
        List<ChatEnhancementOption> options = ChatEnhancementOption.all();
        for (int i = 0; i < options.size(); i++) {
            ChatEnhancementOption option = options.get(i);
            drawCheckboxRow(context, optionRow(card, i), option.label(), ChatEnhancementsSettings.INSTANCE.isEnabled(option), "chat:" + option.id());
        }

        Rect scaleRow = chatScaleRow(card);
        double value = ChatEnhancementsSettings.INSTANCE.configuredChatScale();
        double percent = (value - ChatEnhancementsSettings.MIN_CHAT_SCALE)
                / (ChatEnhancementsSettings.MAX_CHAT_SCALE - ChatEnhancementsSettings.MIN_CHAT_SCALE);
        GuiDraw.text(context, textRenderer, "Chat size", scaleRow.x(), scaleRow.y() + 5, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, formatScale(value), scaleRow.right() - 34, scaleRow.y() + 5, WARM);
        drawSlider(context, chatScaleSlider(card), animatedSlider("v2:chat:scale", percent));
    }

    private void drawScrollableTooltipsOptions(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "Tooltip behavior", card.x() + 14, card.y() + 48, MUTED);
        drawCheckboxRow(context, optionRow(card, 0), "Fade gradients", ScrollableTooltipsSettings.INSTANCE.fadeGradients(), "tooltips:fades");

        List<ScrollableTooltipNumericSetting> settings = ScrollableTooltipNumericSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            ScrollableTooltipNumericSetting setting = settings.get(i);
            int rowIndex = i + 1;
            Rect row = optionRow(card, rowIndex);
            double value = ScrollableTooltipsSettings.INSTANCE.value(setting);
            double percent = (value - setting.min()) / (setting.max() - setting.min());

            GuiDraw.text(context, textRenderer, setting.label(), row.x(), row.y() + 5, JafuTheme.TEXT);
            GuiDraw.text(context, textRenderer, setting.format(value), row.right() - 42, row.y() + 5, WARM);
            drawSlider(context, scrollableTooltipSlider(card, rowIndex), animatedSlider("v2:tooltips:" + setting.id(), percent));
        }

        Rect colorRow = optionRow(card, settings.size() + 1);
        GuiDraw.text(context, textRenderer, "Bar color", colorRow.x(), colorRow.y() + 5, JafuTheme.TEXT);
        drawScrollableTooltipColorSwatches(context, card, colorRow);
    }

    private void drawStorageIndexerOptions(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "Indexed storage", card.x() + 14, card.y() + 48, MUTED);

        Rect preview = new Rect(card.x() + 14, card.y() + 74, card.width() - 28, 48);
        GuiDraw.fill(context, preview, CARD_BORDER);
        GuiDraw.text(context, textRenderer, "Cache keys: " + StorageIndexStore.INSTANCE.snapshotKeyCount(), preview.x() + 10, preview.y() + 10, WARM);

        Optional<StorageSnapshot> newest = StorageIndexStore.INSTANCE.newestSnapshot();
        String newestText = newest
                .map(snapshot -> "Newest: " + snapshot.sourceName())
                .orElse("Newest: nothing indexed");
        GuiDraw.text(context, textRenderer, trimToWidth(newestText, preview.width() - 20), preview.x() + 10, preview.y() + 28, MUTED);
    }

    private void drawItemValueOverlayOptions(DrawContext context, Rect card) {
        PriceCacheStatus status = ItemValueOverlayFeature.status();
        GuiDraw.text(context, textRenderer, "Tooltip estimates", card.x() + 14, card.y() + 48, MUTED);

        List<ItemValueOverlayOption> options = ItemValueOverlayOption.all();
        for (int i = 0; i < options.size(); i++) {
            ItemValueOverlayOption option = options.get(i);
            drawCheckboxRow(context, optionRow(card, i), option.label(), ItemValueOverlaySettings.INSTANCE.isVisible(option), "item-value:" + option.id());
        }

        List<ItemValueNumericSetting> settings = ItemValueNumericSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            ItemValueNumericSetting setting = settings.get(i);
            int rowIndex = options.size() + i;
            Rect row = optionRow(card, rowIndex);
            double value = ItemValueOverlaySettings.INSTANCE.value(setting);
            double percent = (value - setting.min()) / (setting.max() - setting.min());

            GuiDraw.text(context, textRenderer, setting.label(), row.x(), row.y() + 5, JafuTheme.TEXT);
            GuiDraw.text(context, textRenderer, setting.format(value), row.right() - 42, row.y() + 5, WARM);
            drawSlider(context, itemValueSlider(card, rowIndex), animatedSlider("v2:item-value:" + setting.id(), percent));
        }

        int previewY = optionRow(card, options.size() + settings.size() + 1).y();
        Rect preview = new Rect(card.x() + 14, previewY, card.width() - 28, 54);
        GuiDraw.fill(context, preview, CARD_BORDER);
        GuiDraw.text(context, textRenderer, "Bazaar: " + status.bazaarProducts(), preview.x() + 10, preview.y() + 10, WARM);
        GuiDraw.text(context, textRenderer, "Auction names: " + status.auctionItems(), preview.x() + 10, preview.y() + 28, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, status.loading() ? "Refreshing prices..." : "Updated " + status.ageLabel(), preview.x() + 10, preview.y() + 46, MUTED);
    }

    private void drawCooldownDisplayOptions(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "Manual cooldown HUD", card.x() + 14, card.y() + 48, MUTED);

        List<CooldownDisplayOption> options = CooldownDisplayOption.all();
        for (int i = 0; i < options.size(); i++) {
            CooldownDisplayOption option = options.get(i);
            drawCheckboxRow(context, optionRow(card, i), option.label(), CooldownDisplaySettings.INSTANCE.isEnabled(option), "cooldown:" + option.id());
        }

        Rect sliderRow = optionRow(card, options.size());
        double value = CooldownDisplaySettings.INSTANCE.cooldownSeconds();
        double percent = (value - CooldownDisplaySettings.MIN_COOLDOWN_SECONDS)
                / (CooldownDisplaySettings.MAX_COOLDOWN_SECONDS - CooldownDisplaySettings.MIN_COOLDOWN_SECONDS);
        GuiDraw.text(context, textRenderer, "Cooldown length", sliderRow.x(), sliderRow.y() + 5, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, formatCooldownSeconds(value), sliderRow.right() - 42, sliderRow.y() + 5, WARM);
        drawSlider(context, cooldownSecondsSlider(card), animatedSlider("v2:cooldown:seconds", percent));

        Rect colorRow = optionRow(card, options.size() + 1);
        GuiDraw.text(context, textRenderer, "Text color", colorRow.x(), colorRow.y() + 5, JafuTheme.TEXT);
        drawCooldownColorSwatches(context, card, colorRow);

        Rect preview = new Rect(card.x() + 14, optionRow(card, options.size() + 3).y(), card.width() - 28, 44);
        GuiDraw.fill(context, preview, CARD_BORDER);
        GuiDraw.text(context, textRenderer, "Preview: " + (CooldownDisplaySettings.INSTANCE.isEnabled(CooldownDisplayOption.USE_PROGRESS_BAR) ? "progress bar" : "text timer"), preview.x() + 10, preview.y() + 9, CooldownDisplaySettings.INSTANCE.textColor());
        GuiDraw.text(context, textRenderer, "Move/resize in Edit HUD", preview.x() + 10, preview.y() + 27, MUTED);
    }

    private void drawAutoUpdaterOptions(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "Release channel", card.x() + 14, card.y() + 48, MUTED);
        Rect row = optionRow(card, 0);
        Rect control = updaterChannelButton(card);
        UpdateChannel channel = AutoUpdaterSettings.INSTANCE.channel();

        GuiDraw.text(context, textRenderer, "Channel", row.x(), row.y() + 5, JafuTheme.TEXT);
        GuiDraw.fill(context, control, CARD_BORDER);
        GuiDraw.text(context, textRenderer, channel.label(), control.x() + 10, control.y() + 5, WARM);

        GuiDraw.text(context, textRenderer, "/jafu update check", card.x() + 14, card.y() + 104, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, "/jafu update snapshot", card.x() + 14, card.y() + 122, MUTED);
    }

    private void drawPreview(DrawContext context, Rect card) {
        GuiDraw.text(context, textRenderer, "No extra settings", card.x() + 14, card.y() + 49, multiplyAlpha(MUTED, 0.72D));
    }

    private void drawCredits(DrawContext context, Rect panel) {
        Rect area = cardsArea(panel);
        GuiDraw.fill(context, area, CARD);
        GuiDraw.fill(context, new Rect(area.x() + 1, area.y() + 1, 2, area.height() - 2), WARM);
        GuiDraw.text(context, textRenderer, "Credits", area.x() + 18, area.y() + 22, WARM);
        GuiDraw.text(context, textRenderer, "Made with ChatGPT", area.x() + 18, area.y() + 58, JafuTheme.TEXT);
        GuiDraw.text(context, textRenderer, "Mashed together by Chorey", area.x() + 18, area.y() + 78, JafuTheme.WARN);
        GuiDraw.text(context, textRenderer, "All hail the LLM's", area.x() + 18, area.y() + 98, JafuTheme.GOOD);
        GuiDraw.text(context, textRenderer, "Mod Integrity: Success=false | Hash=unavailable", area.x() + 18, area.y() + 126, WARM);
    }

    private void drawCheckboxRow(DrawContext context, Rect row, String label, boolean checked, String key) {
        Rect checkbox = new Rect(row.right() - 12, row.y() + 5, 8, 8);
        GuiDraw.text(context, textRenderer, trimToWidth(label, row.width() - 18), row.x(), row.y() + 5, checked ? JafuTheme.TEXT : MUTED);
        drawCheckbox(context, checkbox, checked, key);
    }

    private void drawCheckbox(DrawContext context, Rect checkbox, boolean checked, String key) {
        double progress = animatedProgress("v2:checkbox:" + key, checked);
        GuiDraw.fill(context, checkbox, lerpColor(CARD_BORDER, WARM_DEEP, progress));
        if (progress > 0.05D) {
            GuiDraw.fill(context, new Rect(checkbox.x() + 3, checkbox.y() + 3, 2, 2), multiplyAlpha(WARM, progress));
        }
    }

    private void drawToggle(DrawContext context, Rect toggle, boolean enabled, double progress) {
        GuiDraw.fill(context, toggle, lerpColor(0x55231A2B, WARM_DEEP, progress));
        GuiDraw.fill(context, inset(toggle, 1), lerpColor(0x66201528, 0x884C2730, progress));
        int knobSize = toggle.height() - 4;
        int travel = toggle.width() - knobSize - 4;
        int knobX = toggle.x() + 2 + (int) Math.round(travel * progress);
        GuiDraw.fill(context, new Rect(knobX, toggle.y() + 2, knobSize, knobSize), enabled ? WARM : MUTED);
    }

    private void drawSlider(DrawContext context, Rect slider, double percent) {
        int knobX = slider.x() + (int) Math.round(MathHelper.clamp(percent, 0.0D, 1.0D) * slider.width());
        GuiDraw.fill(context, slider, 0x66462B36);
        GuiDraw.fill(context, new Rect(slider.x(), slider.y(), Math.max(0, knobX - slider.x()), slider.height()), WARM_DEEP);
        GuiDraw.fill(context, new Rect(knobX - 2, slider.y() - 2, 4, slider.height() + 4), WARM);
    }

    private void drawVersionButton(DrawContext context, Rect card, ClickGuiVersion version) {
        Rect button = versionButton(card, version);
        boolean selected = GlobalSettings.INSTANCE.clickGuiVersion() == version;
        GuiDraw.fill(context, button, selected ? WARM_DEEP : CARD_BORDER);
        GuiDraw.text(context, textRenderer, version.label(), button.x() + 10, button.y() + 5, selected ? WARM : MUTED);
    }

    private void drawGlobalFontDropdown(DrawContext context, Rect card, double progress) {
        Rect control = globalFontControl(card);
        GlobalFontOption[] options = GlobalFontOption.values();
        for (int i = 0; i < options.length; i++) {
            Rect row = globalFontDropdownRow(control, i);
            boolean selected = options[i] == GlobalSettings.INSTANCE.font();
            GuiDraw.fill(context, row, multiplyAlpha(selected ? WARM_DEEP : CARD_BORDER, progress));
            GuiDraw.text(context, textRenderer, options[i].label(), row.x() + 8, row.y() + 5, multiplyAlpha(selected ? WARM : MUTED, progress));
        }
    }

    private boolean selectCategory(Rect panel, Click click) {
        JafuCategory[] categories = JafuCategory.values();
        for (int i = 0; i < categories.length; i++) {
            if (categoryButton(panel, i).contains(click.x(), click.y())) {
                selectedCategory = categories[i];
                selectedModuleIndex = 0;
                globalFontDropdownOpen = false;
                setScrollOffset(panel, 0.0D);
                return true;
            }
        }
        return false;
    }

    private boolean selectModule(Rect panel, Click click) {
        List<JafuModule> modules = visibleModules();
        for (int i = 0; i < modules.size(); i++) {
            if (moduleCard(panel, i).contains(click.x(), click.y())) {
                selectedModuleIndex = i;
                globalFontDropdownOpen = false;
                ensureSelectedCardVisible(panel);
                return true;
            }
        }
        return false;
    }

    private boolean toggleModule(Rect panel, Click click) {
        List<JafuModule> modules = visibleModules();
        for (int i = 0; i < modules.size(); i++) {
            if (moduleToggle(panel, i).contains(click.x(), click.y())) {
                JafuModule module = modules.get(i);
                selectedModuleIndex = i;
                ensureSelectedCardVisible(panel);
                if (JafuModules.GLOBAL_SETTINGS.equals(module.id()) || JafuModules.GUI_SETTINGS.equals(module.id())) {
                    return true;
                }
                module.toggle();
                if (JafuModules.MOD_HIDER.equals(module.id())) {
                    ModHiderSettings.INSTANCE.setEnabled(module.enabled());
                } else if (JafuModules.TOKEN_GUARD.equals(module.id())) {
                    TokenGuardSettings.INSTANCE.setEnabled(module.enabled());
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleSelectedModuleControls(Rect panel, Click click) {
        JafuModule module = selectedModule();
        Rect card = selectedCard(panel);
        if (JafuModules.POWDER_CHEST_TRACKER.equals(module.id())) {
            return togglePowderTrackerOption(card, click);
        }
        if (JafuModules.SACKS_STASH_TRACKER.equals(module.id())) {
            return toggleSacksStashOption(card, click);
        }
        if (JafuModules.GARDEN_RNG_CALCULATOR.equals(module.id())) {
            return toggleGardenRngOption(card, click);
        }
        if (JafuModules.GLOBAL_SETTINGS.equals(module.id())) {
            return toggleGlobalSettingsOption(card, click);
        }
        if (JafuModules.GUI_SETTINGS.equals(module.id())) {
            return toggleGuiSettingsOption(card, click);
        }
        if (JafuModules.ITEM_VIEW.equals(module.id())) {
            return startItemViewSlider(card, click);
        }
        if (JafuModules.CHAT_ENHANCEMENTS.equals(module.id())) {
            return toggleChatEnhancementOption(card, click);
        }
        if (JafuModules.SCROLLABLE_TOOLTIPS.equals(module.id())) {
            return toggleScrollableTooltipsOption(card, click);
        }
        if (JafuModules.ITEM_VALUE_OVERLAY.equals(module.id())) {
            return toggleItemValueOverlayOption(card, click);
        }
        if (JafuModules.COOLDOWN_DISPLAY.equals(module.id())) {
            return toggleCooldownDisplayOption(card, click);
        }
        if (JafuModules.AUTO_UPDATER.equals(module.id())) {
            return toggleAutoUpdaterOption(card, click);
        }
        return false;
    }

    private boolean togglePowderTrackerOption(Rect card, Click click) {
        List<PowderChestStatOption> options = PowderChestStatOption.all();
        for (int i = 0; i < options.size(); i++) {
            if (optionRow(card, i).contains(click.x(), click.y())) {
                PowderChestSettings.INSTANCE.toggle(options.get(i));
                return true;
            }
        }
        if (powderMiningToolRow(card).contains(click.x(), click.y())) {
            PowderChestSettings.INSTANCE.toggleOnlyShowWithMiningTool();
            return true;
        }
        if (powderAutoResetRow(card).contains(click.x(), click.y())) {
            PowderChestSettings.INSTANCE.toggleAutoReset();
            return true;
        }
        if (expanded(powderAutoResetSecondsSlider(card), 4).contains(click.x(), click.y())) {
            draggingPowderAutoResetSeconds = true;
            updatePowderAutoResetSeconds(card, click.x());
            return true;
        }
        if (powderResetButton(card).contains(click.x(), click.y())) {
            PowderChestFeature.resetTracker();
            return true;
        }
        return false;
    }

    private boolean toggleSacksStashOption(Rect card, Click click) {
        List<SacksStashOption> options = SacksStashOption.all();
        for (int i = 0; i < options.size(); i++) {
            if (optionRow(card, i).contains(click.x(), click.y())) {
                SacksStashSettings.INSTANCE.toggle(options.get(i));
                return true;
            }
        }
        return false;
    }

    private boolean toggleGardenRngOption(Rect card, Click click) {
        List<GardenRngStatOption> stats = GardenRngStatOption.all();
        for (int i = 0; i < stats.size(); i++) {
            if (optionRow(card, i).contains(click.x(), click.y())) {
                GardenRngSettings.INSTANCE.toggleStat(stats.get(i));
                return true;
            }
        }

        GardenDropFamily[] families = GardenDropFamily.values();
        int familiesStart = gardenFamiliesStart();
        for (int i = 0; i < families.length; i++) {
            if (optionRow(card, familiesStart + i).contains(click.x(), click.y())) {
                GardenRngSettings.INSTANCE.toggleFamily(families[i]);
                return true;
            }
        }

        int togglesStart = gardenTogglesStart();
        if (optionRow(card, togglesStart).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleFeastTracking();
            return true;
        }
        if (optionRow(card, togglesStart + 1).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleAssumeActiveCrop();
            return true;
        }
        if (optionRow(card, togglesStart + 2).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleOverbloom();
            return true;
        }
        if (optionRow(card, togglesStart + 3).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleProfit();
            return true;
        }
        if (optionRow(card, togglesStart + 4).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleOnlyShowWithWheatHoe();
            return true;
        }
        if (optionRow(card, togglesStart + 5).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.toggleAutoReset();
            return true;
        }
        if (optionRow(card, togglesStart + 6).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.cycleAutoResetSeconds();
            return true;
        }
        if (optionRow(card, togglesStart + 7).contains(click.x(), click.y())) {
            GardenRngSettings.INSTANCE.cycleArmorPieces();
            return true;
        }
        if (gardenResetActiveButton(card).contains(click.x(), click.y())) {
            GardenRngFeature.resetActiveDrops();
            return true;
        }
        if (gardenResetAllButton(card).contains(click.x(), click.y())) {
            GardenRngFeature.resetAll();
            return true;
        }

        List<GardenDropState> activeDrops = GardenRngFeature.snapshot().drops().stream().limit(3).toList();
        int rateStart = gardenRateRowsStart();
        for (int i = 0; i < activeDrops.size(); i++) {
            if (optionRow(card, rateStart + i).contains(click.x(), click.y())) {
                GardenRngSettings.INSTANCE.cycleRateMultiplier(activeDrops.get(i).definition().id());
                return true;
            }
        }
        return false;
    }

    private boolean toggleGlobalSettingsOption(Rect card, Click click) {
        if (optionRow(card, 0).contains(click.x(), click.y())) {
            GlobalSettings.INSTANCE.toggleCustomFont();
            return true;
        }

        Rect fontControl = globalFontControl(card);
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
        if (globalFontDropdownOpen) {
            globalFontDropdownOpen = false;
            return true;
        }
        if (expanded(globalTextScaleSlider(card), 4).contains(click.x(), click.y())) {
            draggingGlobalTextScale = true;
            updateGlobalTextScale(card, click.x());
            return true;
        }
        return false;
    }

    private boolean toggleGuiSettingsOption(Rect card, Click click) {
        if (versionButton(card, ClickGuiVersion.V1).contains(click.x(), click.y())) {
            JafuScreens.switchTo(ClickGuiVersion.V1);
            return true;
        }
        if (versionButton(card, ClickGuiVersion.V2).contains(click.x(), click.y())) {
            GlobalSettings.INSTANCE.setClickGuiVersion(ClickGuiVersion.V2);
            return true;
        }
        return false;
    }

    private boolean startItemViewSlider(Rect card, Click click) {
        List<ItemViewSetting> settings = ItemViewSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            if (expanded(itemViewSlider(card, i), 4).contains(click.x(), click.y())) {
                draggingItemViewSetting = settings.get(i);
                updateItemViewSlider(card, draggingItemViewSetting, click.x());
                return true;
            }
        }
        return false;
    }

    private boolean toggleChatEnhancementOption(Rect card, Click click) {
        List<ChatEnhancementOption> options = ChatEnhancementOption.all();
        for (int i = 0; i < options.size(); i++) {
            if (optionRow(card, i).contains(click.x(), click.y())) {
                ChatEnhancementsSettings.INSTANCE.toggle(options.get(i));
                return true;
            }
        }
        if (expanded(chatScaleSlider(card), 4).contains(click.x(), click.y())) {
            draggingChatScale = true;
            updateChatScale(card, click.x());
            return true;
        }
        return false;
    }

    private boolean toggleScrollableTooltipsOption(Rect card, Click click) {
        if (optionRow(card, 0).contains(click.x(), click.y())) {
            ScrollableTooltipsSettings.INSTANCE.toggleFadeGradients();
            return true;
        }

        List<ScrollableTooltipNumericSetting> settings = ScrollableTooltipNumericSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            ScrollableTooltipNumericSetting setting = settings.get(i);
            int rowIndex = i + 1;
            if (expanded(scrollableTooltipSlider(card, rowIndex), 4).contains(click.x(), click.y())) {
                draggingTooltipSetting = setting;
                updateScrollableTooltipSlider(card, setting, click.x());
                return true;
            }
        }

        for (ScrollableTooltipColorOption option : ScrollableTooltipColorOption.all()) {
            if (scrollableTooltipColorSwatch(card, option).contains(click.x(), click.y())) {
                ScrollableTooltipsSettings.INSTANCE.setScrollBarColor(option.color());
                return true;
            }
        }
        return false;
    }

    private boolean toggleItemValueOverlayOption(Rect card, Click click) {
        List<ItemValueOverlayOption> options = ItemValueOverlayOption.all();
        for (int i = 0; i < options.size(); i++) {
            if (optionRow(card, i).contains(click.x(), click.y())) {
                ItemValueOverlaySettings.INSTANCE.toggle(options.get(i));
                return true;
            }
        }

        List<ItemValueNumericSetting> settings = ItemValueNumericSetting.all();
        for (int i = 0; i < settings.size(); i++) {
            ItemValueNumericSetting setting = settings.get(i);
            int rowIndex = options.size() + i;
            if (expanded(itemValueSlider(card, rowIndex), 4).contains(click.x(), click.y())) {
                draggingItemValueSetting = setting;
                updateItemValueSlider(card, setting, click.x());
                return true;
            }
        }
        return false;
    }

    private boolean toggleCooldownDisplayOption(Rect card, Click click) {
        List<CooldownDisplayOption> options = CooldownDisplayOption.all();
        for (int i = 0; i < options.size(); i++) {
            if (optionRow(card, i).contains(click.x(), click.y())) {
                CooldownDisplaySettings.INSTANCE.toggle(options.get(i));
                return true;
            }
        }

        if (expanded(cooldownSecondsSlider(card), 4).contains(click.x(), click.y())) {
            draggingCooldownSeconds = true;
            updateCooldownSeconds(card, click.x());
            return true;
        }

        for (CooldownDisplayColorOption option : CooldownDisplayColorOption.all()) {
            if (cooldownColorSwatch(card, option).contains(click.x(), click.y())) {
                CooldownDisplaySettings.INSTANCE.setTextColor(option.color());
                return true;
            }
        }
        return false;
    }

    private boolean toggleAutoUpdaterOption(Rect card, Click click) {
        if (updaterChannelButton(card).contains(click.x(), click.y()) || optionRow(card, 0).contains(click.x(), click.y())) {
            UpdateChannel channel = AutoUpdaterSettings.INSTANCE.cycleChannel();
            AutoUpdater.notifyPlayer("JAFU updater channel set to " + channel.label() + ".");
            return true;
        }
        return false;
    }

    private void updateGlobalTextScale(Rect card, double mouseX) {
        Rect slider = globalTextScaleSlider(card);
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = GlobalSettings.MIN_TEXT_SCALE
                + percent * (GlobalSettings.MAX_TEXT_SCALE - GlobalSettings.MIN_TEXT_SCALE);
        GlobalSettings.INSTANCE.setTextScale(value);
    }

    private void updateChatScale(Rect card, double mouseX) {
        Rect slider = chatScaleSlider(card);
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = ChatEnhancementsSettings.MIN_CHAT_SCALE
                + percent * (ChatEnhancementsSettings.MAX_CHAT_SCALE - ChatEnhancementsSettings.MIN_CHAT_SCALE);
        ChatEnhancementsSettings.INSTANCE.setChatScale(value);
    }

    private void updateItemViewSlider(Rect card, ItemViewSetting setting, double mouseX) {
        Rect slider = itemViewSlider(card, ItemViewSetting.all().indexOf(setting));
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = setting.min() + percent * (setting.max() - setting.min());
        ItemViewSettings.INSTANCE.setValue(setting, value);
    }

    private void updateScrollableTooltipSlider(Rect card, ScrollableTooltipNumericSetting setting, double mouseX) {
        int rowIndex = ScrollableTooltipNumericSetting.all().indexOf(setting) + 1;
        Rect slider = scrollableTooltipSlider(card, rowIndex);
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = setting.min() + percent * (setting.max() - setting.min());
        ScrollableTooltipsSettings.INSTANCE.setValue(setting, value);
    }

    private void updateItemValueSlider(Rect card, ItemValueNumericSetting setting, double mouseX) {
        int rowIndex = ItemValueOverlayOption.all().size() + ItemValueNumericSetting.all().indexOf(setting);
        Rect slider = itemValueSlider(card, rowIndex);
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = setting.min() + percent * (setting.max() - setting.min());
        ItemValueOverlaySettings.INSTANCE.setValue(setting, value);
    }

    private void updatePowderAutoResetSeconds(Rect card, double mouseX) {
        Rect slider = powderAutoResetSecondsSlider(card);
        double percent = MathHelper.clamp((mouseX - slider.x()) / slider.width(), 0.0D, 1.0D);
        double value = PowderChestSettings.MIN_AUTO_RESET_SECONDS
                + percent * (PowderChestSettings.MAX_AUTO_RESET_SECONDS - PowderChestSettings.MIN_AUTO_RESET_SECONDS);
        PowderChestSettings.INSTANCE.setAutoResetSeconds(value);
    }

    private void updateCooldownSeconds(Rect card, double mouseX) {
        Rect slider = cooldownSecondsSlider(card);
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

    private double animatedProgress(String key, boolean target) {
        double current = animatedProgress.getOrDefault(key, target ? 1.0D : 0.0D);
        double clampedTarget = target ? 1.0D : 0.0D;
        double next = current + (clampedTarget - current) * animationBlend;
        if (Math.abs(next - clampedTarget) < 0.004D) {
            next = clampedTarget;
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

    private void drawHoverTooltip(DrawContext context, Rect panel, int mouseX, int mouseY) {
        JafuCategory[] categories = JafuCategory.values();
        for (int i = 0; i < categories.length; i++) {
            if (categoryButton(panel, i).contains(mouseX, mouseY)) {
                context.drawTooltip(textRenderer, Text.literal(categories[i].label()), mouseX, mouseY);
                return;
            }
        }
        if (selectedCategory != JafuCategory.QOL) {
            return;
        }
        if (!cardsViewport(panel).contains(mouseX, mouseY)) {
            return;
        }
        List<JafuModule> modules = visibleModules();
        for (int i = 0; i < modules.size(); i++) {
            JafuModule module = modules.get(i);
            if (JafuModules.MOD_HIDER.equals(module.id()) && moduleCard(panel, i).contains(mouseX, mouseY)) {
                context.drawTooltip(textRenderer, Text.literal(MOD_HIDER_TOOLTIP), mouseX, mouseY);
                return;
            }
            if (JafuModules.TOKEN_GUARD.equals(module.id()) && moduleCard(panel, i).contains(mouseX, mouseY)) {
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
        selectedModuleIndex = MathHelper.clamp(selectedModuleIndex, 0, modules.size() - 1);
        return modules.get(selectedModuleIndex);
    }

    private List<JafuModule> visibleModules() {
        List<JafuModule> modules = JafuModules.byCategory(selectedCategory);
        if (selectedModuleIndex >= modules.size()) {
            selectedModuleIndex = Math.max(0, modules.size() - 1);
        }
        return modules;
    }

    private Rect panel() {
        int panelWidth = Math.min(444, width - 40);
        int panelHeight = Math.min(306, height - 36);
        return new Rect((width - panelWidth) / 2, (height - panelHeight) / 2, panelWidth, panelHeight);
    }

    private static Rect inset(Rect rect, int amount) {
        return new Rect(rect.x() + amount, rect.y() + amount, rect.width() - amount * 2, rect.height() - amount * 2);
    }

    private static Rect closeButton(Rect panel) {
        return new Rect(panel.right() - 22, panel.y() + 12, 12, 12);
    }

    private static Rect rail(Rect panel) {
        return new Rect(panel.x(), panel.y(), RAIL_WIDTH, panel.height());
    }

    private static Rect categoryButton(Rect panel, int index) {
        Rect rail = rail(panel);
        JafuCategory[] categories = JafuCategory.values();
        int top = 42;
        int reservedBottom = 66;
        int available = Math.max(1, rail.height() - top - reservedBottom);
        int step = categories.length <= 1 ? 30 : MathHelper.clamp(available / (categories.length - 1), 26, 34);
        return new Rect(rail.x() + 6, rail.y() + top + index * step, 30, 28);
    }

    private static Rect cardsArea(Rect panel) {
        Rect rail = rail(panel);
        return new Rect(rail.right() + 14, panel.y() + 14, panel.right() - rail.right() - 28, panel.height() - 28);
    }

    private static Rect cardsViewport(Rect panel) {
        Rect area = cardsArea(panel);
        return new Rect(area.x(), area.y() + 24, area.width(), area.height() - 24);
    }

    private Rect selectedCard(Rect panel) {
        return moduleCard(panel, selectedModuleIndex);
    }

    private Rect moduleCard(Rect panel, int index) {
        Rect viewport = cardsViewport(panel);
        List<JafuModule> modules = visibleModules();
        int columnWidth = (viewport.width() - COLUMN_GAP - 6) / 2;
        int column = index % 2;
        int x = viewport.x() + column * (columnWidth + COLUMN_GAP);
        int y = viewport.y() - (int) Math.round(scrollOffset());
        for (int i = column; i < index; i += 2) {
            y += cardHeight(modules.get(i), selectedModuleIndex == i) + CARD_GAP;
        }
        JafuModule module = modules.get(index);
        return new Rect(x, y, columnWidth, cardHeight(module, selectedModuleIndex == index));
    }

    private int cardHeight(JafuModule module, boolean selected) {
        if (!selected) {
            return COLLAPSED_CARD_HEIGHT;
        }
        if (JafuModules.POWDER_CHEST_TRACKER.equals(module.id())) {
            return 234;
        }
        if (JafuModules.SACKS_STASH_TRACKER.equals(module.id())) {
            return 164;
        }
        if (JafuModules.GARDEN_RNG_CALCULATOR.equals(module.id())) {
            return 496;
        }
        if (JafuModules.ITEM_VIEW.equals(module.id())) {
            return 178;
        }
        if (JafuModules.CHAT_ENHANCEMENTS.equals(module.id())) {
            return 184;
        }
        if (JafuModules.SCROLLABLE_TOOLTIPS.equals(module.id())) {
            return 226;
        }
        if (JafuModules.STORAGE_INDEXER.equals(module.id())) {
            return 136;
        }
        if (JafuModules.ITEM_VALUE_OVERLAY.equals(module.id())) {
            return 266;
        }
        if (JafuModules.COOLDOWN_DISPLAY.equals(module.id())) {
            return 232;
        }
        if (JafuModules.AUTO_UPDATER.equals(module.id())) {
            return 140;
        }
        if (JafuModules.GLOBAL_SETTINGS.equals(module.id())) {
            return globalFontDropdownOpen ? 218 : 150;
        }
        if (JafuModules.GUI_SETTINGS.equals(module.id())) {
            return 96;
        }
        return 64;
    }

    private Rect moduleToggle(Rect panel, int index) {
        Rect card = moduleCard(panel, index);
        return new Rect(card.right() - 44, card.y() + 10, 28, 12);
    }

    private static Rect optionRow(Rect card, int index) {
        int step = OPTION_ROW_HEIGHT + OPTION_ROW_GAP;
        return new Rect(card.x() + 14, card.y() + 58 + index * step, card.width() - 28, OPTION_ROW_HEIGHT);
    }

    private static Rect powderResetButton(Rect card) {
        Rect row = optionRow(card, PowderChestStatOption.all().size() + 4);
        return new Rect(row.x(), row.y(), 82, 16);
    }

    private static Rect powderMiningToolRow(Rect card) {
        return optionRow(card, PowderChestStatOption.all().size());
    }

    private static Rect powderAutoResetRow(Rect card) {
        return optionRow(card, PowderChestStatOption.all().size() + 1);
    }

    private static Rect powderAutoResetSecondsRow(Rect card) {
        return optionRow(card, PowderChestStatOption.all().size() + 2);
    }

    private static Rect powderAutoResetSecondsSlider(Rect card) {
        Rect row = powderAutoResetSecondsRow(card);
        return new Rect(row.x(), row.y() + 14, Math.max(30, row.width() - 50), 3);
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

    private static Rect gardenResetActiveButton(Rect card) {
        Rect row = optionRow(card, gardenTogglesStart() + 9);
        return new Rect(row.x(), row.y(), 88, 16);
    }

    private static Rect gardenResetAllButton(Rect card) {
        Rect row = optionRow(card, gardenTogglesStart() + 9);
        return new Rect(row.x() + 96, row.y(), 78, 16);
    }

    private static Rect globalFontControl(Rect card) {
        Rect row = optionRow(card, 1);
        return new Rect(row.right() - 86, row.y(), 86, 16);
    }

    private static Rect globalFontDropdownRow(Rect control, int index) {
        return new Rect(control.x(), control.bottom() + 2 + index * 16, control.width(), 16);
    }

    private Rect globalTextScaleRow(Rect card) {
        return optionRow(card, globalFontDropdownOpen ? GlobalFontOption.values().length + 2 : 2);
    }

    private Rect globalTextScaleSlider(Rect card) {
        Rect row = globalTextScaleRow(card);
        return new Rect(row.x(), row.y() + 14, Math.max(30, row.width() - 48), 3);
    }

    private static Rect versionButton(Rect card, ClickGuiVersion version) {
        Rect row = optionRow(card, 0);
        int buttonWidth = 38;
        return new Rect(row.right() - buttonWidth * 2 + version.ordinal() * buttonWidth, row.y(), buttonWidth, 18);
    }

    private static Rect itemViewSlider(Rect card, int index) {
        Rect row = optionRow(card, index);
        return new Rect(row.x(), row.y() + 14, Math.max(30, row.width() - 50), 3);
    }

    private static Rect chatScaleRow(Rect card) {
        return optionRow(card, ChatEnhancementOption.all().size() + 1);
    }

    private static Rect chatScaleSlider(Rect card) {
        Rect row = chatScaleRow(card);
        return new Rect(row.x(), row.y() + 14, Math.max(30, row.width() - 48), 3);
    }

    private static Rect scrollableTooltipSlider(Rect card, int rowIndex) {
        Rect row = optionRow(card, rowIndex);
        return new Rect(row.x(), row.y() + 14, Math.max(30, row.width() - 50), 3);
    }

    private static Rect itemValueSlider(Rect card, int rowIndex) {
        Rect row = optionRow(card, rowIndex);
        return new Rect(row.x(), row.y() + 14, Math.max(30, row.width() - 50), 3);
    }

    private static Rect cooldownSecondsSlider(Rect card) {
        Rect row = optionRow(card, CooldownDisplayOption.all().size());
        return new Rect(row.x(), row.y() + 14, Math.max(30, row.width() - 50), 3);
    }

    private void drawCooldownColorSwatches(DrawContext context, Rect card, Rect row) {
        for (CooldownDisplayColorOption option : CooldownDisplayColorOption.all()) {
            Rect swatch = cooldownColorSwatch(card, option);
            boolean selected = CooldownDisplaySettings.INSTANCE.textColor() == option.color();
            GuiDraw.fill(context, new Rect(swatch.x() - 1, swatch.y() - 1, swatch.width() + 2, swatch.height() + 2), selected ? WARM : CARD_BORDER);
            GuiDraw.fill(context, swatch, option.color());
        }
        CooldownDisplayColorOption current = CooldownDisplayColorOption.fromColor(CooldownDisplaySettings.INSTANCE.textColor());
        GuiDraw.text(context, textRenderer, current.label(), row.right() - 34, row.y() + 5, MUTED);
    }

    private static Rect cooldownColorSwatch(Rect card, CooldownDisplayColorOption option) {
        Rect row = optionRow(card, CooldownDisplayOption.all().size() + 1);
        int size = 8;
        int gap = 4;
        int count = CooldownDisplayColorOption.all().size();
        int startX = row.right() - 42 - count * size - (count - 1) * gap;
        int index = CooldownDisplayColorOption.all().indexOf(option);
        return new Rect(startX + index * (size + gap), row.y() + 5, size, size);
    }

    private void drawScrollableTooltipColorSwatches(DrawContext context, Rect card, Rect row) {
        for (ScrollableTooltipColorOption option : ScrollableTooltipColorOption.all()) {
            Rect swatch = scrollableTooltipColorSwatch(card, option);
            boolean selected = ScrollableTooltipsSettings.INSTANCE.scrollBarColor() == option.color();
            GuiDraw.fill(context, new Rect(swatch.x() - 1, swatch.y() - 1, swatch.width() + 2, swatch.height() + 2), selected ? WARM : CARD_BORDER);
            GuiDraw.fill(context, swatch, option.color());
        }
        ScrollableTooltipColorOption current = ScrollableTooltipColorOption.fromColor(ScrollableTooltipsSettings.INSTANCE.scrollBarColor());
        GuiDraw.text(context, textRenderer, current.label(), row.right() - 34, row.y() + 5, MUTED);
    }

    private static Rect scrollableTooltipColorSwatch(Rect card, ScrollableTooltipColorOption option) {
        Rect row = optionRow(card, ScrollableTooltipNumericSetting.all().size() + 1);
        int size = 8;
        int gap = 4;
        int count = ScrollableTooltipColorOption.all().size();
        int startX = row.right() - 42 - count * size - (count - 1) * gap;
        int index = ScrollableTooltipColorOption.all().indexOf(option);
        return new Rect(startX + index * (size + gap), row.y() + 5, size, size);
    }

    private static Rect updaterChannelButton(Rect card) {
        Rect row = optionRow(card, 0);
        return new Rect(row.right() - 74, row.y(), 74, 16);
    }

    private double scrollOffset() {
        return categoryScrollOffsets.getOrDefault(selectedCategory, 0.0D);
    }

    private void setScrollOffset(Rect panel, double value) {
        double clamped = MathHelper.clamp(value, 0.0D, maxScroll(panel));
        if (clamped <= 0.5D) {
            categoryScrollOffsets.remove(selectedCategory);
            return;
        }
        categoryScrollOffsets.put(selectedCategory, clamped);
    }

    private double maxScroll(Rect panel) {
        return Math.max(0, contentHeight() - cardsViewport(panel).height());
    }

    private int contentHeight() {
        List<JafuModule> modules = visibleModules();
        int[] columnHeights = new int[2];
        for (int i = 0; i < modules.size(); i++) {
            int column = i % 2;
            columnHeights[column] += cardHeight(modules.get(i), selectedModuleIndex == i) + CARD_GAP;
        }
        return Math.max(0, Math.max(columnHeights[0], columnHeights[1]) - CARD_GAP);
    }

    private void ensureSelectedCardVisible(Rect panel) {
        Rect viewport = cardsViewport(panel);
        Rect card = selectedCard(panel);
        double scroll = scrollOffset();
        if (card.y() < viewport.y()) {
            setScrollOffset(panel, scroll - (viewport.y() - card.y()));
            return;
        }
        if (card.bottom() > viewport.bottom()) {
            setScrollOffset(panel, scroll + (card.bottom() - viewport.bottom()));
        }
    }

    private static boolean intersects(Rect first, Rect second) {
        return first.right() > second.left()
                && first.left() < second.right()
                && first.bottom() > second.top()
                && first.top() < second.bottom();
    }

    private static Rect expanded(Rect rect, int amount) {
        return new Rect(rect.x() - amount, rect.y() - amount, rect.width() + amount * 2, rect.height() + amount * 2);
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
}
