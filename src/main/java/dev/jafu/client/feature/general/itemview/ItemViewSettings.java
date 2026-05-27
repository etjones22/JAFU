package dev.jafu.client.feature.general.itemview;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

public final class ItemViewSettings {
    public static final ItemViewSettings INSTANCE = new ItemViewSettings();

    private final Map<ItemViewSetting, Double> values = new EnumMap<>(ItemViewSetting.class);
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("jafu-item-view.properties");

    private ItemViewSettings() {
        for (ItemViewSetting setting : ItemViewSetting.all()) {
            values.put(setting, setting.defaultValue());
        }
        load();
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

    private void load() {
        if (!Files.isRegularFile(configPath)) {
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(configPath)) {
            properties.load(reader);
        } catch (IOException ignored) {
            return;
        }

        for (ItemViewSetting setting : ItemViewSetting.all()) {
            try {
                setValue(setting, Double.parseDouble(properties.getProperty(setting.id(), Double.toString(setting.defaultValue()))));
            } catch (NumberFormatException ignored) {
                values.put(setting, setting.defaultValue());
            }
        }
    }

    private void save() {
        Properties properties = new Properties();
        for (ItemViewSetting setting : ItemViewSetting.all()) {
            properties.setProperty(setting.id(), Double.toString(value(setting)));
        }

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "JAFU item view");
            }
        } catch (IOException ignored) {
            // Item view settings should never crash the client.
        }
    }

    private static double snap(ItemViewSetting setting, double value) {
        double snapped = Math.round(value / setting.step()) * setting.step();
        return Math.round(snapped * 100.0D) / 100.0D;
    }
}
