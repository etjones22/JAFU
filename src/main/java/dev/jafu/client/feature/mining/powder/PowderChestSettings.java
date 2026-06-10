package dev.jafu.client.feature.mining.powder;

import java.util.EnumMap;
import java.util.Map;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;
import net.minecraft.util.math.MathHelper;

public final class PowderChestSettings implements JafuConfigurable {
    public static final PowderChestSettings INSTANCE = new PowderChestSettings();
    public static final double MIN_AUTO_RESET_SECONDS = 10.0D;
    public static final double MAX_AUTO_RESET_SECONDS = 300.0D;
    public static final double DEFAULT_AUTO_RESET_SECONDS = 30.0D;
    public static final double AUTO_RESET_SECONDS_STEP = 5.0D;

    private final Map<PowderChestStatOption, Boolean> visibleStats = new EnumMap<>(PowderChestStatOption.class);
    private final ConfigFile config = ConfigFile.named("jafu-powder-chest.properties");
    private boolean autoResetEnabled = true;
    private boolean onlyShowWithMiningTool = true;
    private double autoResetSeconds = DEFAULT_AUTO_RESET_SECONDS;

    private PowderChestSettings() {
        for (PowderChestStatOption option : PowderChestStatOption.all()) {
            visibleStats.put(option, true);
        }
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "powder_chest";
    }

    public boolean isVisible(PowderChestStatOption option) {
        return visibleStats.getOrDefault(option, true);
    }

    public void toggle(PowderChestStatOption option) {
        visibleStats.put(option, !isVisible(option));
        save();
    }

    public boolean autoResetEnabled() {
        return autoResetEnabled;
    }

    public boolean onlyShowWithMiningTool() {
        return onlyShowWithMiningTool;
    }

    public void toggleOnlyShowWithMiningTool() {
        onlyShowWithMiningTool = !onlyShowWithMiningTool;
        save();
    }

    public void toggleAutoReset() {
        autoResetEnabled = !autoResetEnabled;
        save();
    }

    public double autoResetSeconds() {
        return autoResetSeconds;
    }

    public long autoResetMillis() {
        return Math.round(autoResetSeconds * 1000.0D);
    }

    public void setAutoResetSeconds(double seconds) {
        autoResetSeconds = snapSeconds(seconds);
        save();
    }

    public int visibleStatCount() {
        int count = 0;
        for (PowderChestStatOption option : PowderChestStatOption.all()) {
            if (option.addsHudLine() && isVisible(option)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }

        for (PowderChestStatOption option : PowderChestStatOption.all()) {
            visibleStats.put(option, config.booleanValue(option.id(), true));
        }
        onlyShowWithMiningTool = config.booleanValue("only_show_with_mining_tool", true);
        autoResetEnabled = config.booleanValue("auto_reset", true);
        autoResetSeconds = snapSeconds(config.doubleValue("auto_reset_seconds", DEFAULT_AUTO_RESET_SECONDS));
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        for (PowderChestStatOption option : PowderChestStatOption.all()) {
            config.setBoolean(option.id(), isVisible(option));
        }
        config.setBoolean("only_show_with_mining_tool", onlyShowWithMiningTool);
        config.setBoolean("auto_reset", autoResetEnabled);
        config.setDouble("auto_reset_seconds", autoResetSeconds);
        return config.save("JAFU powder chest tracker");
    }

    private static double snapSeconds(double seconds) {
        double clamped = MathHelper.clamp(seconds, MIN_AUTO_RESET_SECONDS, MAX_AUTO_RESET_SECONDS);
        double snapped = Math.round(clamped / AUTO_RESET_SECONDS_STEP) * AUTO_RESET_SECONDS_STEP;
        return Math.round(snapped * 10.0D) / 10.0D;
    }
}
