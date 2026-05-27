package dev.jafu.client.hud;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import dev.jafu.client.gui.util.Rect;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

public final class HudLayoutStore {
    public static final HudLayoutStore INSTANCE = new HudLayoutStore();

    private final Map<String, Position> positions = new HashMap<>();
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("jafu-hud.properties");

    private HudLayoutStore() {
        load();
    }

    public Rect bounds(JafuHudElement element, int width, int height, int screenWidth, int screenHeight) {
        Position position = positions.getOrDefault(element.id(), new Position(element.defaultX(), element.defaultY()));
        int x = clamp(position.x(), width, screenWidth);
        int y = clamp(position.y(), height, screenHeight);
        return new Rect(x, y, width, height);
    }

    public void move(JafuHudElement element, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        positions.put(element.id(), new Position(clamp(x, width, screenWidth), clamp(y, height, screenHeight)));
    }

    public void reset(JafuHudElement element) {
        positions.remove(element.id());
    }

    public void resetAll() {
        positions.clear();
    }

    public void save() {
        Properties properties = new Properties();
        positions.forEach((id, position) -> {
            properties.setProperty(id + ".x", Integer.toString(position.x()));
            properties.setProperty(id + ".y", Integer.toString(position.y()));
        });

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "JAFU HUD layout");
            }
        } catch (IOException ignored) {
            // Layout edits should never crash the client.
        }
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

        for (JafuHudElement element : JafuHudElements.all()) {
            int x = readInt(properties, element.id() + ".x", element.defaultX());
            int y = readInt(properties, element.id() + ".y", element.defaultY());
            positions.put(element.id(), new Position(x, y));
        }
    }

    private static int readInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int size, int screenSize) {
        return MathHelper.clamp(value, 0, Math.max(0, screenSize - size));
    }

    private record Position(int x, int y) {
    }
}
