package dev.jafu.client.feature.general.itemview;

import java.util.EnumMap;
import java.util.Map;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;
import net.minecraft.util.math.MathHelper;

public final class ItemViewSettings implements JafuConfigurable {
    public static final ItemViewSettings INSTANCE = new ItemViewSettings();

    private final Map<ItemViewSetting, Double> values = new EnumMap<>(ItemViewSetting.class);
    private final ConfigFile config = ConfigFile.named("jafu-item-view.properties");

    private ItemViewSettings() {
        for (ItemViewSetting setting : ItemViewSetting.all()) {
            values.put(setting, setting.defaultValue());
        }
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "item_view";
    }

    public double value(ItemViewSetting setting) {
        return values.getOrDefault(setting, setting.defaultValue());
    }

    public void setValue(ItemViewSetting setting, double value) {
        values.put(setting, snap(setting, MathHelper.clamp(value, setting.min(), setting.max())));
        save();
    }

    public int intValue(ItemViewSetting setting) {
        return (int) Math.round(value(setting));
    }

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }

        for (ItemViewSetting setting : ItemViewSetting.all()) {
            double value = config.doubleValue(setting.id(), setting.defaultValue());
            values.put(setting, snap(setting, MathHelper.clamp(value, setting.min(), setting.max())));
        }
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        for (ItemViewSetting setting : ItemViewSetting.all()) {
            config.setDouble(setting.id(), value(setting));
        }
        return config.save("JAFU item view");
    }

    private static double snap(ItemViewSetting setting, double value) {
        double snapped = Math.round(value / setting.step()) * setting.step();
        return Math.round(snapped * 100.0D) / 100.0D;
    }
}
