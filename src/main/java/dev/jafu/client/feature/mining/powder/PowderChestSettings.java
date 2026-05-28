package dev.jafu.client.feature.mining.powder;

import java.util.EnumMap;
import java.util.Map;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;

public final class PowderChestSettings implements JafuConfigurable {
    public static final PowderChestSettings INSTANCE = new PowderChestSettings();

    private final Map<PowderChestStatOption, Boolean> visibleStats = new EnumMap<>(PowderChestStatOption.class);
    private final ConfigFile config = ConfigFile.named("jafu-powder-chest.properties");

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
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        for (PowderChestStatOption option : PowderChestStatOption.all()) {
            config.setBoolean(option.id(), isVisible(option));
        }
        return config.save("JAFU powder chest tracker");
    }
}
