package dev.jafu.client.feature.qol.tokenguard;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;

public final class TokenGuardSettings implements JafuConfigurable {
    public static final TokenGuardSettings INSTANCE = new TokenGuardSettings();

    private final ConfigFile config = ConfigFile.named("jafu-token-guard.properties");
    private boolean enabled;

    private TokenGuardSettings() {
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "token_guard";
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
        enabled = config.booleanValue("enabled", false);
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        config.setBoolean("enabled", enabled);
        return config.save("JAFU TokenGuard settings");
    }
}
