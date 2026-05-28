package dev.jafu.client.feature.qol.itemvalue;

import java.util.EnumMap;
import java.util.Map;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;
import net.minecraft.util.math.MathHelper;

public final class ItemValueOverlaySettings implements JafuConfigurable {
    public static final ItemValueOverlaySettings INSTANCE = new ItemValueOverlaySettings();

    private static final String CONFIG_ID = "item_value_overlay";
    private static final String ENABLED = "enabled";

    private final ConfigFile file = ConfigFile.named("jafu-item-value-overlay.properties");
    private final Map<ItemValueOverlayOption, Boolean> visibleOptions = new EnumMap<>(ItemValueOverlayOption.class);
    private final Map<ItemValueNumericSetting, Double> values = new EnumMap<>(ItemValueNumericSetting.class);
    private boolean enabled = false;

    private ItemValueOverlaySettings() {
        for (ItemValueOverlayOption option : ItemValueOverlayOption.all()) {
            visibleOptions.put(option, option.defaultValue());
        }
        for (ItemValueNumericSetting setting : ItemValueNumericSetting.all()) {
            values.put(setting, setting.defaultValue());
        }
        JafuConfigManager.register(this);
        load();
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public boolean isVisible(ItemValueOverlayOption option) {
        return visibleOptions.getOrDefault(option, option.defaultValue());
    }

    public void toggle(ItemValueOverlayOption option) {
        visibleOptions.put(option, !isVisible(option));
        save();
    }

    public double value(ItemValueNumericSetting setting) {
        return values.getOrDefault(setting, setting.defaultValue());
    }

    public int intValue(ItemValueNumericSetting setting) {
        return (int) Math.round(value(setting));
    }

    public void setValue(ItemValueNumericSetting setting, double value) {
        values.put(setting, snap(setting, value));
        save();
        SkyBlockPriceService.INSTANCE.refreshIfNeeded(true);
    }

    @Override
    public String configId() {
        return CONFIG_ID;
    }

    @Override
    public boolean load() {
        if (!file.load()) {
            return false;
        }
        enabled = file.booleanValue(ENABLED, enabled);
        for (ItemValueOverlayOption option : ItemValueOverlayOption.all()) {
            visibleOptions.put(option, file.booleanValue(option.id(), option.defaultValue()));
        }
        for (ItemValueNumericSetting setting : ItemValueNumericSetting.all()) {
            values.put(setting, snap(setting, file.doubleValue(setting.id(), setting.defaultValue())));
        }
        return true;
    }

    @Override
    public boolean save() {
        file.clear();
        file.setBoolean(ENABLED, enabled);
        for (ItemValueOverlayOption option : ItemValueOverlayOption.all()) {
            file.setBoolean(option.id(), isVisible(option));
        }
        for (ItemValueNumericSetting setting : ItemValueNumericSetting.all()) {
            file.setDouble(setting.id(), value(setting));
        }
        return file.save("JAFU item value overlay settings");
    }

    private static double snap(ItemValueNumericSetting setting, double value) {
        double clamped = MathHelper.clamp(value, setting.min(), setting.max());
        return Math.round(clamped / setting.step()) * setting.step();
    }
}
