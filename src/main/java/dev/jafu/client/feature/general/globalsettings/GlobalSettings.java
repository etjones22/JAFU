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

    private final ConfigFile config = ConfigFile.named("jafu-global.properties");
    private GlobalFontOption font = GlobalFontOption.CLEAN;
    private ClickGuiVersion clickGuiVersion = ClickGuiVersion.V1;
    private double textScale = 1.0D;

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

    public double textScale() {
        return textScale;
    }

    public void setTextScale(double value) {
        textScale = snap(value, MIN_TEXT_SCALE, MAX_TEXT_SCALE, TEXT_SCALE_STEP);
        save();
    }

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }

        font = GlobalFontOption.CLEAN;
        clickGuiVersion = ClickGuiVersion.V1;
        textScale = 1.0D;
        font = GlobalFontOption.fromId(config.stringValue("font", font.id()));
        clickGuiVersion = ClickGuiVersion.fromId(config.stringValue("click_gui_version", clickGuiVersion.id()));
        textScale = config.doubleValue("text_scale", textScale);
        textScale = snap(textScale, MIN_TEXT_SCALE, MAX_TEXT_SCALE, TEXT_SCALE_STEP);
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        config.setString("font", font.id());
        config.setString("click_gui_version", clickGuiVersion.id());
        config.setDouble("text_scale", textScale);
        return config.save("JAFU global settings");
    }

    private static double snap(double value, double min, double max, double step) {
        double clamped = MathHelper.clamp(value, min, max);
        return Math.round(clamped / step) * step;
    }
}
