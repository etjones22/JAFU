package dev.jafu.client.feature.general.globalsettings;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;
import net.minecraft.util.math.MathHelper;

public final class GlobalSettings implements JafuConfigurable {
    public static final GlobalSettings INSTANCE = new GlobalSettings();
    public static final double MIN_TEXT_SCALE = 0.75D;
    public static final double MAX_TEXT_SCALE = 1.5D;
    public static final double TEXT_SCALE_STEP = 0.05D;
    public static final double MIN_HUD_OPACITY = 0.20D;
    public static final double MAX_HUD_OPACITY = 1.0D;
    public static final double HUD_OPACITY_STEP = 0.05D;

    private final ConfigFile config = ConfigFile.named("jafu-global.properties");
    private GlobalFontOption font = GlobalFontOption.CLEAN;
    private ClickGuiVersion clickGuiVersion = ClickGuiVersion.V1;
    private HudStylePreset hudStylePreset = HudStylePreset.REFERENCE;
    private int hudAccentColor = HudAccentColorOption.SKYBLOCK_GOLD.color();
    private boolean customFontEnabled = false;
    private boolean hudGradientLine = true;
    private boolean hudCompactMode;
    private double textScale = 1.0D;
    private double hudBackgroundOpacity = 0.70D;
    private double hudBorderOpacity = 1.0D;

    private GlobalSettings() {
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "global";
    }

    public GlobalFontOption font() {
        return font;
    }

    public GlobalFontOption cycleFont() {
        GlobalFontOption[] options = GlobalFontOption.values();
        font = options[(font.ordinal() + 1) % options.length];
        save();
        return font;
    }

    public void setFont(GlobalFontOption font) {
        this.font = font;
        save();
    }

    public ClickGuiVersion clickGuiVersion() {
        return clickGuiVersion;
    }

    public void setClickGuiVersion(ClickGuiVersion clickGuiVersion) {
        this.clickGuiVersion = clickGuiVersion;
        save();
    }

    public HudStylePreset hudStylePreset() {
        return hudStylePreset;
    }

    public void cycleHudStylePreset() {
        HudStylePreset[] presets = HudStylePreset.values();
        hudStylePreset = presets[(hudStylePreset.ordinal() + 1) % presets.length];
        save();
    }

    public int hudAccentColor() {
        return hudAccentColor;
    }

    public void setHudAccentColor(int hudAccentColor) {
        this.hudAccentColor = hudAccentColor | 0xFF000000;
        save();
    }

    public boolean customFontEnabled() {
        return customFontEnabled;
    }

    public void toggleCustomFont() {
        customFontEnabled = !customFontEnabled;
        save();
    }

    public void setCustomFontEnabled(boolean customFontEnabled) {
        this.customFontEnabled = customFontEnabled;
        save();
    }

    public double textScale() {
        return textScale;
    }

    public void setTextScale(double value) {
        textScale = snap(value, MIN_TEXT_SCALE, MAX_TEXT_SCALE, TEXT_SCALE_STEP);
        save();
    }

    public boolean hudGradientLine() {
        return hudGradientLine;
    }

    public void toggleHudGradientLine() {
        hudGradientLine = !hudGradientLine;
        save();
    }

    public boolean hudCompactMode() {
        return hudCompactMode;
    }

    public void toggleHudCompactMode() {
        hudCompactMode = !hudCompactMode;
        save();
    }

    public double hudBackgroundOpacity() {
        return hudBackgroundOpacity;
    }

    public void setHudBackgroundOpacity(double value) {
        hudBackgroundOpacity = snap(value, MIN_HUD_OPACITY, MAX_HUD_OPACITY, HUD_OPACITY_STEP);
        save();
    }

    public double hudBorderOpacity() {
        return hudBorderOpacity;
    }

    public void setHudBorderOpacity(double value) {
        hudBorderOpacity = snap(value, MIN_HUD_OPACITY, MAX_HUD_OPACITY, HUD_OPACITY_STEP);
        save();
    }

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }

        font = GlobalFontOption.CLEAN;
        clickGuiVersion = ClickGuiVersion.V1;
        hudStylePreset = HudStylePreset.REFERENCE;
        hudAccentColor = HudAccentColorOption.SKYBLOCK_GOLD.color();
        customFontEnabled = false;
        hudGradientLine = true;
        hudCompactMode = false;
        textScale = 1.0D;
        hudBackgroundOpacity = 0.70D;
        hudBorderOpacity = 1.0D;
        font = GlobalFontOption.fromId(config.stringValue("font", font.id()));
        clickGuiVersion = ClickGuiVersion.fromId(config.stringValue("click_gui_version", clickGuiVersion.id()));
        hudStylePreset = HudStylePreset.fromId(config.stringValue("hud_style_preset", hudStylePreset.id()));
        hudAccentColor = config.intValue("hud_accent_color", hudAccentColor) | 0xFF000000;
        customFontEnabled = config.contains("custom_font")
                ? config.booleanValue("custom_font", customFontEnabled)
                : migratedCustomFontEnabled();
        hudGradientLine = config.booleanValue("hud_gradient_line", hudGradientLine);
        hudCompactMode = config.booleanValue("hud_compact_mode", hudCompactMode);
        textScale = config.doubleValue("text_scale", textScale);
        textScale = snap(textScale, MIN_TEXT_SCALE, MAX_TEXT_SCALE, TEXT_SCALE_STEP);
        hudBackgroundOpacity = snap(config.doubleValue("hud_background_opacity", hudBackgroundOpacity), MIN_HUD_OPACITY, MAX_HUD_OPACITY, HUD_OPACITY_STEP);
        hudBorderOpacity = snap(config.doubleValue("hud_border_opacity", hudBorderOpacity), MIN_HUD_OPACITY, MAX_HUD_OPACITY, HUD_OPACITY_STEP);
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        config.setString("font", font.id());
        config.setString("click_gui_version", clickGuiVersion.id());
        config.setString("hud_style_preset", hudStylePreset.id());
        config.setInt("hud_accent_color", hudAccentColor);
        config.setBoolean("custom_font", customFontEnabled);
        config.setBoolean("hud_gradient_line", hudGradientLine);
        config.setBoolean("hud_compact_mode", hudCompactMode);
        config.setDouble("text_scale", textScale);
        config.setDouble("hud_background_opacity", hudBackgroundOpacity);
        config.setDouble("hud_border_opacity", hudBorderOpacity);
        return config.save("JAFU global settings");
    }

    private boolean migratedCustomFontEnabled() {
        ConfigFile legacyConfig = ConfigFile.named("jafu-gui.properties");
        if (!legacyConfig.load() || !legacyConfig.contains("custom_font")) {
            return false;
        }
        return legacyConfig.booleanValue("custom_font", false);
    }

    private static double snap(double value, double min, double max, double step) {
        double clamped = MathHelper.clamp(value, min, max);
        return Math.round(clamped / step) * step;
    }
}
