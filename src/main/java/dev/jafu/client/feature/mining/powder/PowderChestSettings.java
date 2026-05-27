package dev.jafu.client.feature.mining.powder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class PowderChestSettings {
    public static final PowderChestSettings INSTANCE = new PowderChestSettings();

    private final Map<PowderChestStatOption, Boolean> visibleStats = new EnumMap<>(PowderChestStatOption.class);
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("jafu-powder-chest.properties");

    private PowderChestSettings() {
        for (PowderChestStatOption option : PowderChestStatOption.all()) {
            visibleStats.put(option, true);
        }
        load();
    }

    public boolean isVisible(PowderChestStatOption option) {
        return visibleStats.getOrDefault(option, true);
    }

    public void toggle(PowderChestStatOption option) {
        visibleStats.put(option, !isVisible(option));
        save();
    }

    public int visibleStatCount() {
        int count = 0;
        for (PowderChestStatOption option : PowderChestStatOption.all()) {
            if (isVisible(option)) {
                count++;
            }
        }
        return count;
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

        for (PowderChestStatOption option : PowderChestStatOption.all()) {
            visibleStats.put(option, Boolean.parseBoolean(properties.getProperty(option.id(), "true")));
        }
    }

    private void save() {
        Properties properties = new Properties();
        for (PowderChestStatOption option : PowderChestStatOption.all()) {
            properties.setProperty(option.id(), Boolean.toString(isVisible(option)));
        }

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "JAFU powder chest tracker");
            }
        } catch (IOException ignored) {
            // Tracker settings should never crash the client.
        }
    }
}
