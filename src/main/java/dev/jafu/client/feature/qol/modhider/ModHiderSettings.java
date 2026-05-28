package dev.jafu.client.feature.qol.modhider;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;

public final class ModHiderSettings implements JafuConfigurable {
    public static final ModHiderSettings INSTANCE = new ModHiderSettings();

    private final ConfigFile config = ConfigFile.named("jafu-mod-hider.properties");
    private boolean enabled;

    private ModHiderSettings() {
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "mod_hider";
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }
        enabled = false;
        enabled = config.booleanValue("enabled", false);
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        config.setBoolean("enabled", enabled);
        return config.save("JAFU Mod Hider settings");
    }
}
