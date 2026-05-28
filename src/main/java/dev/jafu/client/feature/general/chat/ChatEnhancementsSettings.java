package dev.jafu.client.feature.general.chat;

import java.util.EnumMap;
import java.util.Map;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;
import dev.jafu.client.module.JafuModules;
import net.minecraft.util.math.MathHelper;

public final class ChatEnhancementsSettings implements JafuConfigurable {
    public static final ChatEnhancementsSettings INSTANCE = new ChatEnhancementsSettings();
    public static final double MIN_CHAT_SCALE = 0.5D;
    public static final double MAX_CHAT_SCALE = 1.75D;
    public static final double CHAT_SCALE_STEP = 0.05D;

    private final Map<ChatEnhancementOption, Boolean> enabledOptions = new EnumMap<>(ChatEnhancementOption.class);
    private final ConfigFile config = ConfigFile.named("jafu-chat-enhancements.properties");
    private double chatScale = 1.0D;

    private ChatEnhancementsSettings() {
        for (ChatEnhancementOption option : ChatEnhancementOption.all()) {
            enabledOptions.put(option, true);
        }
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "chat_enhancements";
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

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }

        chatScale = 1.0D;
        for (ChatEnhancementOption option : ChatEnhancementOption.all()) {
            enabledOptions.put(option, config.booleanValue(option.id(), true));
        }
        chatScale = snap(config.doubleValue("chat_scale", chatScale), MIN_CHAT_SCALE, MAX_CHAT_SCALE, CHAT_SCALE_STEP);
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        for (ChatEnhancementOption option : ChatEnhancementOption.all()) {
            config.setBoolean(option.id(), isEnabled(option));
        }
        config.setDouble("chat_scale", chatScale);
        return config.save("JAFU chat enhancements");
    }

    private static double snap(double value, double min, double max, double step) {
        double clamped = MathHelper.clamp(value, min, max);
        return Math.round(clamped / step) * step;
    }
}
