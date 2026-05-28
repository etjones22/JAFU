package dev.jafu.client.feature.gui.scrollabletooltips;

import java.util.EnumMap;
import java.util.Map;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;
import net.minecraft.util.math.MathHelper;

public final class ScrollableTooltipsSettings implements JafuConfigurable {
    public static final ScrollableTooltipsSettings INSTANCE = new ScrollableTooltipsSettings();

    private final Map<ScrollableTooltipNumericSetting, Double> values = new EnumMap<>(ScrollableTooltipNumericSetting.class);
    private final ConfigFile config = ConfigFile.named("jafu-scrollable-tooltips.properties");
    private boolean enabled = true;
    private boolean fadeGradients = true;
    private int scrollBarColor = ScrollableTooltipColorOption.SKYBLOCK_GOLD.color();

    private ScrollableTooltipsSettings() {
        for (ScrollableTooltipNumericSetting setting : ScrollableTooltipNumericSetting.all()) {
            values.put(setting, setting.defaultValue());
        }
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "scrollable_tooltips";
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public boolean fadeGradients() {
        return fadeGradients;
    }

    public void toggleFadeGradients() {
        fadeGradients = !fadeGradients;
        save();
    }

    public int scrollBarColor() {
        return scrollBarColor;
    }

    public void setScrollBarColor(int scrollBarColor) {
        this.scrollBarColor = withFullAlpha(scrollBarColor);
        save();
    }

    public double value(ScrollableTooltipNumericSetting setting) {
        return values.getOrDefault(setting, setting.defaultValue());
    }

    public int intValue(ScrollableTooltipNumericSetting setting) {
        return (int) Math.round(value(setting));
    }

    public void setValue(ScrollableTooltipNumericSetting setting, double value) {
        values.put(setting, snap(setting, MathHelper.clamp(value, setting.min(), setting.max())));
        save();
    }

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }

        enabled = config.booleanValue("enabled", true);
        fadeGradients = config.booleanValue("fade_gradients", true);
        scrollBarColor = withFullAlpha(config.intValue("scroll_bar_color", ScrollableTooltipColorOption.SKYBLOCK_GOLD.color()));
        for (ScrollableTooltipNumericSetting setting : ScrollableTooltipNumericSetting.all()) {
            double value = config.doubleValue(setting.id(), setting.defaultValue());
            values.put(setting, snap(setting, MathHelper.clamp(value, setting.min(), setting.max())));
        }
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        config.setBoolean("enabled", enabled);
        config.setBoolean("fade_gradients", fadeGradients);
        config.setInt("scroll_bar_color", scrollBarColor);
        for (ScrollableTooltipNumericSetting setting : ScrollableTooltipNumericSetting.all()) {
            config.setDouble(setting.id(), value(setting));
        }
        return config.save("JAFU scrollable tooltips");
    }

    private static double snap(ScrollableTooltipNumericSetting setting, double value) {
        double snapped = Math.round(value / setting.step()) * setting.step();
        return Math.round(snapped * 100.0D) / 100.0D;
    }

    private static int withFullAlpha(int color) {
        return 0xFF000000 | color & 0x00FFFFFF;
    }
}
