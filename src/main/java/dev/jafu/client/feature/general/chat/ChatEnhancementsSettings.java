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

public final class ChatEnhancementsSettings {
    public static final ChatEnhancementsSettings INSTANCE = new ChatEnhancementsSettings();

    private final Map<ChatEnhancementOption, Boolean> enabledOptions = new EnumMap<>(ChatEnhancementOption.class);
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("jafu-chat-enhancements.properties");

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
    }

    private void save() {
        Properties properties = new Properties();
        for (ChatEnhancementOption option : ChatEnhancementOption.all()) {
            properties.setProperty(option.id(), Boolean.toString(isEnabled(option)));
        }

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                properties.store(writer, "JAFU chat enhancements");
            }
        } catch (IOException ignored) {
            // Chat settings should never crash the client.
        }
    }
}
