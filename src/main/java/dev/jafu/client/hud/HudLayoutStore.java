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

    private final Map<String, Layout> layouts = new HashMap<>();
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("jafu-hud.properties");

    private HudLayoutStore() {
        load();
    }

    public Rect bounds(JafuHudElement element, int width, int height, int screenWidth, int screenHeight) {
        Layout layout = layouts.getOrDefault(element.id(), Layout.defaults(element));
        int resolvedWidth = clampSize(layout.widthOr(width), element.minWidth(), screenWidth);
        int resolvedHeight = clampSize(layout.heightOr(height), element.minHeight(), screenHeight);
        int x = clampPosition(layout.x(), resolvedWidth, screenWidth);
        int y = clampPosition(layout.y(), resolvedHeight, screenHeight);
        return new Rect(x, y, resolvedWidth, resolvedHeight);
    }

    public void move(JafuHudElement element, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        Layout current = layouts.getOrDefault(element.id(), Layout.defaults(element));
        int resolvedWidth = clampSize(current.widthOr(width), element.minWidth(), screenWidth);
        int resolvedHeight = clampSize(current.heightOr(height), element.minHeight(), screenHeight);
        layouts.put(element.id(), current.withPosition(
                clampPosition(x, resolvedWidth, screenWidth),
                clampPosition(y, resolvedHeight, screenHeight)
        ));
    }

    public void resize(JafuHudElement element, int x, int y, int width, int height, int screenWidth, int screenHeight) {
        Layout current = layouts.getOrDefault(element.id(), Layout.defaults(element));
        int clampedX = clampPosition(x, element.minWidth(), screenWidth);
        int clampedY = clampPosition(y, element.minHeight(), screenHeight);
        int maxWidth = Math.max(element.minWidth(), screenWidth - clampedX);
        int maxHeight = Math.max(element.minHeight(), screenHeight - clampedY);
        int resolvedWidth = MathHelper.clamp(width, element.minWidth(), maxWidth);
        int resolvedHeight = MathHelper.clamp(height, element.minHeight(), maxHeight);
        layouts.put(element.id(), current.withBounds(clampedX, clampedY, resolvedWidth, resolvedHeight));
    }

    public void reset(JafuHudElement element) {
        layouts.remove(element.id());
    }

    public void resetAll() {
        layouts.clear();
    }

    public void save() {
        Properties properties = new Properties();
        layouts.forEach((id, layout) -> {
            properties.setProperty(id + ".x", Integer.toString(layout.x()));
            properties.setProperty(id + ".y", Integer.toString(layout.y()));
            if (layout.width() != null) {
                properties.setProperty(id + ".width", Integer.toString(layout.width()));
            }
            if (layout.height() != null) {
                properties.setProperty(id + ".height", Integer.toString(layout.height()));
            }
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
            boolean hasLayout = properties.containsKey(element.id() + ".x")
                    || properties.containsKey(element.id() + ".y")
                    || properties.containsKey(element.id() + ".width")
                    || properties.containsKey(element.id() + ".height");
            if (!hasLayout) {
                continue;
            }

            int x = readInt(properties, element.id() + ".x", element.defaultX());
            int y = readInt(properties, element.id() + ".y", element.defaultY());
            Integer width = readOptionalInt(properties, element.id() + ".width");
            Integer height = readOptionalInt(properties, element.id() + ".height");
            layouts.put(element.id(), new Layout(x, y, width, height));
        }
    }

    private static int readInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Integer readOptionalInt(Properties properties, String key) {
        if (!properties.containsKey(key)) {
            return null;
        }

        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int clampPosition(int value, int size, int screenSize) {
        return MathHelper.clamp(value, 0, Math.max(0, screenSize - size));
    }

    private static int clampSize(int value, int minSize, int screenSize) {
        return MathHelper.clamp(value, minSize, Math.max(minSize, screenSize));
    }

    private record Layout(int x, int y, Integer width, Integer height) {
        private static Layout defaults(JafuHudElement element) {
            return new Layout(element.defaultX(), element.defaultY(), null, null);
        }

        private int widthOr(int fallback) {
            return width == null ? fallback : width;
        }

        private int heightOr(int fallback) {
            return height == null ? fallback : height;
        }

        private Layout withPosition(int x, int y) {
            return new Layout(x, y, width, height);
        }

        private Layout withBounds(int x, int y, int width, int height) {
            return new Layout(x, y, width, height);
        }
    }
}
