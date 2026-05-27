package dev.jafu.client.feature.general.guisettings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class GuiSettings {
    public static final GuiSettings INSTANCE = new GuiSettings();

    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("jafu-gui.properties");
    private boolean customFontEnabled = false;

    private GuiSettings() {
        load();
    }

    public boolean customFontEnabled() {
        return customFontEnabled;
    }

    public void toggleCustomFont() {
        customFontEnabled = !customFontEnabled;
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

        customFontEnabled = Boolean.parseBoolean(properties.getProperty("custom_font", "false"));
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty("custom_font", Boolean.toString(customFontEnabled));

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "JAFU GUI settings");
            }
        } catch (IOException ignored) {
            // GUI settings should never crash the client.
        }
    }
}
