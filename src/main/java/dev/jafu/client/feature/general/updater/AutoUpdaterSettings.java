package dev.jafu.client.feature.general.updater;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class AutoUpdaterSettings {
    public static final AutoUpdaterSettings INSTANCE = new AutoUpdaterSettings();

    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("jafu-updater.properties");
    private UpdateChannel channel = UpdateChannel.STABLE;

    private AutoUpdaterSettings() {
        load();
    }

    public UpdateChannel channel() {
        return channel;
    }

    public void setChannel(UpdateChannel channel) {
        this.channel = channel;
        save();
    }

    public UpdateChannel cycleChannel() {
        setChannel(channel.next());
        return channel;
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

        channel = UpdateChannel.fromId(properties.getProperty("channel", UpdateChannel.STABLE.id()));
    }

    private void save() {
        Properties properties = new Properties();
        properties.setProperty("channel", channel.id());

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "JAFU updater");
            }
        } catch (IOException ignored) {
            // Updater settings should never crash the client.
        }
    }
}
