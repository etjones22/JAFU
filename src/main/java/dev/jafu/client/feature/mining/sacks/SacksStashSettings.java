package dev.jafu.client.feature.mining.sacks;

import java.util.EnumMap;
import java.util.Map;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;

public final class SacksStashSettings implements JafuConfigurable {
    public static final SacksStashSettings INSTANCE = new SacksStashSettings();

    private final Map<SacksStashOption, Boolean> visibleFields = new EnumMap<>(SacksStashOption.class);
    private final ConfigFile config = ConfigFile.named("jafu-sacks-stash.properties");

    private SacksStashSettings() {
        for (SacksStashOption option : SacksStashOption.all()) {
            visibleFields.put(option, true);
        }
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "sacks_stash";
    }

    public boolean isVisible(SacksStashOption option) {
        return visibleFields.getOrDefault(option, true);
    }

    public void toggle(SacksStashOption option) {
        visibleFields.put(option, !isVisible(option));
        save();
    }

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }

        for (SacksStashOption option : SacksStashOption.all()) {
            visibleFields.put(option, config.booleanValue(option.id(), true));
        }
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        for (SacksStashOption option : SacksStashOption.all()) {
            config.setBoolean(option.id(), isVisible(option));
        }
        return config.save("JAFU sacks and stash tracker");
    }
}
