package dev.jafu.client.feature.skyblock.storage;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;

public final class StorageIndexerSettings implements JafuConfigurable {
    public static final StorageIndexerSettings INSTANCE = new StorageIndexerSettings();

    private static final String CONFIG_ID = "storage-indexer";
    private static final String ENABLED = "enabled";

    private final ConfigFile file = ConfigFile.named("jafu-storage-indexer.properties");
    private boolean enabled = true;

    private StorageIndexerSettings() {
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
        return true;
    }

    @Override
    public boolean save() {
        file.clear();
        file.setBoolean(ENABLED, enabled);
        return file.save("JAFU storage indexer settings");
    }
}
