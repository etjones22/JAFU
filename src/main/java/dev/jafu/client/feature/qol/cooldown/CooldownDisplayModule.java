package dev.jafu.client.feature.qol.cooldown;

import dev.jafu.client.gui.JafuTheme;
import dev.jafu.client.module.JafuCategory;
import dev.jafu.client.module.JafuModule;
import dev.jafu.client.module.JafuModules;

public final class CooldownDisplayModule extends JafuModule {
    public CooldownDisplayModule() {
        super(
                JafuModules.COOLDOWN_DISPLAY,
                JafuCategory.QOL,
                "Cooldown display",
                "Shows cooldowns for manually used weapons and items",
                CooldownDisplaySettings.INSTANCE.enabled(),
                JafuTheme.ACCENT
        );
    }

    @Override
    public void toggle() {
        super.toggle();
        CooldownDisplaySettings.INSTANCE.setEnabled(enabled());
    }
}
