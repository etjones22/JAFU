package dev.jafu.client.feature.qol.cooldown;

import java.util.EnumMap;
import java.util.Map;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;
import net.minecraft.util.math.MathHelper;

public final class CooldownDisplaySettings implements JafuConfigurable {
    public static final CooldownDisplaySettings INSTANCE = new CooldownDisplaySettings();

    public static final double MIN_COOLDOWN_SECONDS = 0.5D;
    public static final double MAX_COOLDOWN_SECONDS = 60.0D;

    private static final String CONFIG_ID = "cooldown_display";
    private static final String ENABLED = "enabled";
    private static final String TEXT_COLOR = "text_color";
    private static final String COOLDOWN_SECONDS = "cooldown_seconds";

    private final ConfigFile file = ConfigFile.named("jafu-cooldown-display.properties");
    private final Map<CooldownDisplayOption, Boolean> options = new EnumMap<>(CooldownDisplayOption.class);
    private boolean enabled = false;
    private int textColor = CooldownDisplayColorOption.GOLD.color();
    private double cooldownSeconds = 5.0D;

    private CooldownDisplaySettings() {
        for (CooldownDisplayOption option : CooldownDisplayOption.all()) {
            options.put(option, option.defaultValue());
        }
        JafuConfigManager.register(this);
        load();
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public boolean isEnabled(CooldownDisplayOption option) {
        return options.getOrDefault(option, option.defaultValue());
    }

    public void toggle(CooldownDisplayOption option) {
        options.put(option, !isEnabled(option));
        save();
    }

    public int textColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
        save();
    }

    public double cooldownSeconds() {
        return cooldownSeconds;
    }

    public void setCooldownSeconds(double cooldownSeconds) {
        this.cooldownSeconds = snapCooldown(cooldownSeconds);
        save();
    }

    public long cooldownMillis() {
        return Math.max(1L, Math.round(cooldownSeconds * 1000.0D));
    }

    @Override
    public String configId() {
        return CONFIG_ID;
    }

    @Override
    public boolean load() {
        if (!file.load()) {
            return false;
        }

        enabled = file.booleanValue(ENABLED, enabled);
        textColor = file.intValue(TEXT_COLOR, textColor);
        cooldownSeconds = snapCooldown(file.doubleValue(COOLDOWN_SECONDS, cooldownSeconds));
        for (CooldownDisplayOption option : CooldownDisplayOption.all()) {
            options.put(option, file.booleanValue(option.id(), option.defaultValue()));
        }
        return true;
    }

    @Override
    public boolean save() {
        file.clear();
        file.setBoolean(ENABLED, enabled);
        file.setInt(TEXT_COLOR, textColor);
        file.setDouble(COOLDOWN_SECONDS, cooldownSeconds);
        for (CooldownDisplayOption option : CooldownDisplayOption.all()) {
            file.setBoolean(option.id(), isEnabled(option));
        }
        return file.save("JAFU cooldown display settings");
    }

    private static double snapCooldown(double value) {
        double clamped = MathHelper.clamp(value, MIN_COOLDOWN_SECONDS, MAX_COOLDOWN_SECONDS);
        return Math.round(clamped * 2.0D) / 2.0D;
    }
}
