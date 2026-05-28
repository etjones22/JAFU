package dev.jafu.client.feature.general.updater;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;

public final class AutoUpdaterSettings implements JafuConfigurable {
    public static final AutoUpdaterSettings INSTANCE = new AutoUpdaterSettings();

    private final ConfigFile config = ConfigFile.named("jafu-updater.properties");
    private UpdateChannel channel = UpdateChannel.STABLE;

    private AutoUpdaterSettings() {
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "auto_updater";
    }

    public UpdateChannel channel() {
        return channel;
    }

    public void setChannel(UpdateChannel channel) {
        this.channel = channel;
        save();
    }

    public UpdateChannel cycleChannel() {
        setChannel(channel.next());
        return channel;
    }

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }
        channel = UpdateChannel.STABLE;
        channel = UpdateChannel.fromId(config.stringValue("channel", UpdateChannel.STABLE.id()));
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        config.setString("channel", channel.id());
        return config.save("JAFU updater");
    }
}
