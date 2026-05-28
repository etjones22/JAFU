package dev.jafu.client.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class ConfigFile {
    private final Path path;
    private final Properties properties = new Properties();

    private ConfigFile(Path path) {
        this.path = path;
    }

    public static ConfigFile named(String fileName) {
        return new ConfigFile(FabricLoader.getInstance().getConfigDir().resolve(fileName));
    }

    public Path path() {
        return path;
    }

    public boolean load() {
        properties.clear();
        if (!Files.isRegularFile(path)) {
            return true;
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
            return true;
        } catch (IOException ignored) {
            properties.clear();
            return false;
        }
    }

    public boolean save(String comment) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                properties.store(writer, comment);
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public void clear() {
        properties.clear();
    }

    public String stringValue(String key, String fallback) {
        return properties.getProperty(key, fallback);
    }

    public boolean booleanValue(String key, boolean fallback) {
        return Boolean.parseBoolean(properties.getProperty(key, Boolean.toString(fallback)));
    }

    public int intValue(String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public Integer optionalIntValue(String key) {
        if (!properties.containsKey(key)) {
            return null;
        }

        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public double doubleValue(String key, double fallback) {
        try {
            return Double.parseDouble(properties.getProperty(key, Double.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public boolean contains(String key) {
        return properties.containsKey(key);
    }

    public void setString(String key, String value) {
        properties.setProperty(key, value);
    }

    public void setBoolean(String key, boolean value) {
        setString(key, Boolean.toString(value));
    }

    public void setInt(String key, int value) {
        setString(key, Integer.toString(value));
    }

    public void setDouble(String key, double value) {
        setString(key, Double.toString(value));
    }
}
