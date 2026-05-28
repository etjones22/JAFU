package dev.jafu.client.feature.general.guisettings;

import dev.jafu.client.config.ConfigFile;
import dev.jafu.client.config.JafuConfigManager;
import dev.jafu.client.config.JafuConfigurable;

public final class GuiSettings implements JafuConfigurable {
    public static final GuiSettings INSTANCE = new GuiSettings();

    private final ConfigFile config = ConfigFile.named("jafu-gui.properties");
    private boolean customFontEnabled = false;

    private GuiSettings() {
        JafuConfigManager.register(this);
        load();
    }

    @Override
    public String configId() {
        return "gui";
    }

    public boolean customFontEnabled() {
        return customFontEnabled;
    }

    public void toggleCustomFont() {
        customFontEnabled = !customFontEnabled;
        save();
    }

    @Override
    public boolean load() {
        if (!config.load()) {
            return false;
        }
        customFontEnabled = false;
        customFontEnabled = config.booleanValue("custom_font", false);
        return true;
    }

    @Override
    public boolean save() {
        config.clear();
        config.setBoolean("custom_font", customFontEnabled);
        return config.save("JAFU GUI settings");
    }
}
