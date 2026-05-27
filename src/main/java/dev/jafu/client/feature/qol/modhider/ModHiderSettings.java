package dev.jafu.client.feature.qol.modhider;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class ModHiderSettings {
    public static final ModHiderSettings INSTANCE = new ModHiderSettings();

    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("jafu-mod-hider.properties");
    private boolean enabled;

    private ModHiderSettings() {
        load();
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

        enabled = Boolean.parseBoolean(properties.getProperty("enabled", "false"));
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty("enabled", Boolean.toString(enabled));

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "JAFU Mod Hider settings");
            }
        } catch (IOException ignored) {
            // Mod hider settings should never crash the client.
        }
    }
}
