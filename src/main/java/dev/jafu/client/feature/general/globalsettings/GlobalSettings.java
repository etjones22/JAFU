package dev.jafu.client.feature.general.globalsettings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

public final class GlobalSettings {
    public static final GlobalSettings INSTANCE = new GlobalSettings();
    public static final double MIN_TEXT_SCALE = 0.75D;
    public static final double MAX_TEXT_SCALE = 1.5D;
    public static final double TEXT_SCALE_STEP = 0.05D;

    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("jafu-global.properties");
    private GlobalFontOption font = GlobalFontOption.CLEAN;
    private double textScale = 1.0D;

    private GlobalSettings() {
        load();
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

    public double textScale() {
        return textScale;
    }

    public void setTextScale(double value) {
        textScale = snap(value, MIN_TEXT_SCALE, MAX_TEXT_SCALE, TEXT_SCALE_STEP);
        save();
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

        font = GlobalFontOption.fromId(properties.getProperty("font", font.id()));
        textScale = readDouble(properties, "text_scale", textScale);
        textScale = snap(textScale, MIN_TEXT_SCALE, MAX_TEXT_SCALE, TEXT_SCALE_STEP);
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty("font", font.id());
        properties.setProperty("text_scale", Double.toString(textScale));

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "JAFU global settings");
            }
        } catch (IOException ignored) {
            // Global settings should never crash the client.
        }
    }

    private static double readDouble(Properties properties, String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double snap(double value, double min, double max, double step) {
        double clamped = MathHelper.clamp(value, min, max);
        return Math.round(clamped / step) * step;
    }
}
