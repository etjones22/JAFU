package dev.jafu.client.feature.mining.sacks;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class SacksStashSettings {
    public static final SacksStashSettings INSTANCE = new SacksStashSettings();

    private final Map<SacksStashOption, Boolean> visibleFields = new EnumMap<>(SacksStashOption.class);
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("jafu-sacks-stash.properties");

    private SacksStashSettings() {
        for (SacksStashOption option : SacksStashOption.all()) {
            visibleFields.put(option, true);
        }
        load();
    }

    public boolean isVisible(SacksStashOption option) {
        return visibleFields.getOrDefault(option, true);
    }

    public void toggle(SacksStashOption option) {
        visibleFields.put(option, !isVisible(option));
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

        for (SacksStashOption option : SacksStashOption.all()) {
            visibleFields.put(option, Boolean.parseBoolean(properties.getProperty(option.id(), "true")));
        }
    }

    private void save() {
        Properties properties = new Properties();
        for (SacksStashOption option : SacksStashOption.all()) {
            properties.setProperty(option.id(), Boolean.toString(isVisible(option)));
        }

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "JAFU sacks and stash tracker");
            }
        } catch (IOException ignored) {
            // Tracker settings should never crash the client.
        }
    }
}
