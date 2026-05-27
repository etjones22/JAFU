package dev.jafu.client.feature.general.chat;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import dev.jafu.client.module.JafuModules;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

public final class ChatEnhancementsSettings {
    public static final ChatEnhancementsSettings INSTANCE = new ChatEnhancementsSettings();
    public static final double MIN_CHAT_SCALE = 0.5D;
    public static final double MAX_CHAT_SCALE = 1.75D;
    public static final double CHAT_SCALE_STEP = 0.05D;

    private final Map<ChatEnhancementOption, Boolean> enabledOptions = new EnumMap<>(ChatEnhancementOption.class);
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("jafu-chat-enhancements.properties");
    private double chatScale = 1.0D;

    private ChatEnhancementsSettings() {
        for (ChatEnhancementOption option : ChatEnhancementOption.all()) {
            enabledOptions.put(option, true);
        }
        load();
    }

    public boolean isEnabled(ChatEnhancementOption option) {
        return enabledOptions.getOrDefault(option, true);
    }

    public boolean smoothChatEnabled() {
        return JafuModules.isEnabled(JafuModules.CHAT_ENHANCEMENTS) && isEnabled(ChatEnhancementOption.SMOOTH_CHAT);
    }

    public boolean cleanFontEnabled() {
        return JafuModules.isEnabled(JafuModules.CHAT_ENHANCEMENTS) && isEnabled(ChatEnhancementOption.CLEAN_FONT);
    }

    public double chatScale() {
        if (!JafuModules.isEnabled(JafuModules.CHAT_ENHANCEMENTS)) {
            return 1.0D;
        }
        return chatScale;
    }

    public double configuredChatScale() {
        return chatScale;
    }

    public void setChatScale(double value) {
        chatScale = snap(value, MIN_CHAT_SCALE, MAX_CHAT_SCALE, CHAT_SCALE_STEP);
        save();
    }

    public void toggle(ChatEnhancementOption option) {
        enabledOptions.put(option, !isEnabled(option));
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

        for (ChatEnhancementOption option : ChatEnhancementOption.all()) {
            enabledOptions.put(option, Boolean.parseBoolean(properties.getProperty(option.id(), "true")));
        }
        chatScale = snap(readDouble(properties, "chat_scale", chatScale), MIN_CHAT_SCALE, MAX_CHAT_SCALE, CHAT_SCALE_STEP);
    }

    private void save() {
        Properties properties = new Properties();
        for (ChatEnhancementOption option : ChatEnhancementOption.all()) {
            properties.setProperty(option.id(), Boolean.toString(isEnabled(option)));
        }
        properties.setProperty("chat_scale", Double.toString(chatScale));

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "JAFU chat enhancements");
            }
        } catch (IOException ignored) {
            // Chat settings should never crash the client.
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
