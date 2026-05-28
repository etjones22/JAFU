package dev.jafu.client.hud;

import java.util.HashMap;
import java.util.Map;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;
import dev.jafu.client.gui.util.Rect;
import net.minecraft.util.math.MathHelper;

public final class HudLayoutStore implements JafuConfigurable {
    public static final HudLayoutStore INSTANCE = new HudLayoutStore();

    private final Map<String, Layout> layouts = new HashMap<>();
    private final ConfigFile config = ConfigFile.named("jafu-hud.properties");

    private HudLayoutStore() {
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "hud_layout";
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

    @Override
    public boolean save() {
        config.clear();
        layouts.forEach((id, layout) -> {
            config.setInt(id + ".x", layout.x());
            config.setInt(id + ".y", layout.y());
            if (layout.width() != null) {
                config.setInt(id + ".width", layout.width());
            }
            if (layout.height() != null) {
                config.setInt(id + ".height", layout.height());
            }
        });

        return config.save("JAFU HUD layout");
    }

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }

        layouts.clear();
        for (JafuHudElement element : JafuHudElements.all()) {
            boolean hasLayout = config.contains(element.id() + ".x")
                    || config.contains(element.id() + ".y")
                    || config.contains(element.id() + ".width")
                    || config.contains(element.id() + ".height");
            if (!hasLayout) {
                continue;
            }

            int x = config.intValue(element.id() + ".x", element.defaultX());
            int y = config.intValue(element.id() + ".y", element.defaultY());
            Integer width = config.optionalIntValue(element.id() + ".width");
            Integer height = config.optionalIntValue(element.id() + ".height");
            layouts.put(element.id(), new Layout(x, y, width, height));
        }
        return true;
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
